package com.axconstantino.reservationsystem.rooms.database.repository;

import com.axconstantino.reservationsystem.booking.database.model.Booking;
import com.axconstantino.reservationsystem.rooms.database.model.Room;
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
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
public class RoomRepositoryCustomImpl implements RoomRepositoryCustom {
    private final EntityManager entityManager;
    private final RoomMapper roomMapper;

    @Override
    public Page<RoomDTO> findAvailableRooms(RoomFilterRequest filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Room> cq = cb.createQuery(Room.class);
        Root<Room> room = cq.from(Room.class);

        List<Predicate> predicates = new ArrayList<>();

        if (filter.getCapacity() != null)
            predicates.add(cb.ge(room.get("capacity"), filter.getCapacity()));

        if (filter.getMinPrice() != null)
            predicates.add(cb.ge(room.get("price"), filter.getMinPrice()));

        if (filter.getMaxPrice() != null)
            predicates.add(cb.le(room.get("price"), filter.getMaxPrice()));

        if (filter.getType() != null)
            predicates.add(cb.equal(room.get("type"), filter.getType()));

        if (filter.getStartDate() != null && filter.getEndDate() != null) {
            Subquery<Booking> sub = cq.subquery(Booking.class);
            Root<Booking> booking = sub.from(Booking.class);
            sub.select(booking)
                    .where(
                            cb.equal(booking.get("room"), room),
                            cb.equal(booking.get("startDate"), filter.getEndDate()),
                            cb.equal(booking.get("endDate"), filter.getStartDate())
                    );
            predicates.add(cb.not(cb.exists(sub)));
        }

        cq.where(predicates.toArray(new Predicate[0]));

        Path<Object> sortBy = room.get(filter.getSortBy());
        if ("desc".equalsIgnoreCase(filter.getOrder()))
            cq.orderBy(cb.desc(sortBy));
        else
            cq.orderBy(cb.asc(sortBy));

        TypedQuery<Room> query = entityManager.createQuery(cq);

        int page = filter.getPage();
        int size = filter.getSize();

        query.setFirstResult(page * size);
        query.setMaxResults(size);

        List<RoomDTO> result = query.getResultList()
                .stream()
                .map(roomMapper::toDto)
                .collect(Collectors.toList());

        long total = countTotalResults(filter);

        return new PageImpl<>(result, PageRequest.of(page, size), total);
    }


    private long countTotalResults(RoomFilterRequest filter) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Room> room = cq.from(Room.class);

        List<Predicate> predicates = new ArrayList<>();

        if (filter.getCapacity() != null)
            predicates.add(cb.ge(room.get("capacity"), filter.getCapacity()));

        if (filter.getMinPrice() != null)
            predicates.add(cb.ge(room.get("price"), filter.getMinPrice()));

        if (filter.getMaxPrice() != null)
            predicates.add(cb.le(room.get("price"), filter.getMaxPrice()));

        if (filter.getType() != null)
            predicates.add(cb.equal(room.get("type"), filter.getType()));

        if (filter.getStartDate() != null && filter.getEndDate() != null) {
            Subquery<Booking> sub = cq.subquery(Booking.class);
            Root<Booking> booking = sub.from(Booking.class);
            sub.select(booking)
                    .where(
                            cb.equal(booking.get("room"), room),
                            cb.equal(booking.get("startDate"), filter.getEndDate()),
                            cb.equal(booking.get("endDate"), filter.getStartDate())
                    );
            predicates.add(cb.not(cb.exists(sub)));
        }

        cq.where(predicates.toArray(new Predicate[0]));

        return entityManager.createQuery(cq).getSingleResult();
    }
}
