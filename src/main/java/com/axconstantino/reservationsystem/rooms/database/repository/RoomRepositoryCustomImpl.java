package com.axconstantino.reservationsystem.rooms.database.repository;

import com.axconstantino.reservationsystem.booking.database.model.Booking;
import com.axconstantino.reservationsystem.rooms.database.model.Room;
import com.axconstantino.reservationsystem.rooms.database.model.enums.RoomStatus;
import com.axconstantino.reservationsystem.rooms.dto.RoomDTO;
import com.axconstantino.reservationsystem.rooms.dto.RoomFilterRequest;
import com.axconstantino.reservationsystem.rooms.mapper.RoomMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class RoomRepositoryCustomImpl implements RoomRepositoryCustom {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("pricePerNight", "capacity", "type");
    private static final int MAX_PAGE_SIZE = 100;

    private final EntityManager entityManager;
    private final RoomMapper roomMapper;

    @Override
    public Page<RoomDTO> findAvailableRooms(RoomFilterRequest filter) {
        validatePaginationParameters(filter);
        validateSortField(filter.getSortBy());

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Room> cq = cb.createQuery(Room.class);
        Root<Room> room = cq.from(Room.class);

        List<Predicate> predicates = buildPredicates(filter, cb, room);

        cq.where(predicates.toArray(new Predicate[0]))
                .orderBy(createOrder(cb, room, filter));

        TypedQuery<Room> query = entityManager.createQuery(cq)
                .setFirstResult(filter.getPage() * filter.getSize())
                .setMaxResults(filter.getSize())
                .setHint("org.hibernate.fetchSize", Math.min(filter.getSize(), MAX_PAGE_SIZE));

        List<RoomDTO> content = query.getResultStream()
                .map(roomMapper::toDto)
                .collect(Collectors.toList());

        long total = countTotalResults(filter, cb, predicates);

        return new PageImpl<>(content, PageRequest.of(filter.getPage(), filter.getSize()), total);
    }

    private long countTotalResults(RoomFilterRequest filter, CriteriaBuilder cb, List<Predicate> predicates) {
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Room> room = countQuery.from(Room.class);

        countQuery.select(cb.count(room))
                .where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private List<Predicate> buildPredicates(RoomFilterRequest filter, CriteriaBuilder cb, Root<Room> room) {
        List<Predicate> predicates = new ArrayList<>();

        addCapacityPredicate(filter, cb, room, predicates);
        addPricePredicates(filter, cb, room, predicates);
        addTypePredicate(filter, cb, room, predicates);
        addAvailabilityPredicate(filter, cb, room, predicates);

        return predicates;
    }

    private void addAvailabilityPredicate(RoomFilterRequest filter, CriteriaBuilder cb, Root<Room> room, List<Predicate> predicates) {
        predicates.add(cb.equal(room.get("status"), RoomStatus.AVAILABLE));
        if (filter.getStartDate() != null && filter.getEndDate() != null) {
            CriteriaQuery<?> cq = cb.createQuery();
            Subquery<Booking> subquery = cq.subquery(Booking.class);
            Root<Booking> booking = subquery.from(Booking.class);

            Predicate roomMatch = cb.equal(booking.get("room"), room);
            Predicate dateOverlap = cb.and(
                    cb.lessThan(booking.get("startDate"), filter.getEndDate()),
                    cb.greaterThan(booking.get("endDate"), filter.getStartDate())
            );

            subquery.select(booking)
                    .where(cb.and(roomMatch, dateOverlap));

            predicates.add(cb.not(cb.exists(subquery)));
        }
    }

    private Order createOrder(CriteriaBuilder cb, Root<Room> room, RoomFilterRequest filter) {
        Path<Object> sortPath = room.get(filter.getSortBy());
        return "desc".equalsIgnoreCase(filter.getOrder())
                ? cb.desc(sortPath)
                : cb.asc(sortPath);
    }

    private void validateSortField(String sortBy) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sort field: " + sortBy);
        }
    }

    private void validatePaginationParameters(RoomFilterRequest filter) {
        if (filter.getPage() < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }

        if (filter.getSize() <= 0 || filter.getSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private void addCapacityPredicate(RoomFilterRequest filter, CriteriaBuilder cb,
                                      Root<Room> room, List<Predicate> predicates) {
        if (filter.getCapacity() != null) {
            predicates.add(cb.ge(room.get("capacity"), filter.getCapacity()));
        }
    }

    private void addPricePredicates(RoomFilterRequest filter, CriteriaBuilder cb,
                                    Root<Room> room, List<Predicate> predicates) {
        if (filter.getMinPrice() != null) {
            predicates.add(cb.ge(room.get("pricePerNight"), filter.getMinPrice()));
        }
        if (filter.getMaxPrice() != null) {
            predicates.add(cb.le(room.get("pricePerNight"), filter.getMaxPrice()));
        }
    }

    private void addTypePredicate(RoomFilterRequest filter, CriteriaBuilder cb,
                                  Root<Room> room, List<Predicate> predicates) {
        if (filter.getType() != null) {
            predicates.add(cb.equal(room.get("type"), filter.getType()));
        }
    }
}
