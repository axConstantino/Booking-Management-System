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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of custom repository operations for {@code Room} entities.
 * <p>
 *     This class provides a method to search for available rooms in the database
 *     based on various filter criteria such as capacity, prince range, room type,
 *     and availability dates. It constructs a dynamic JPA Criteria API query,
 *     applies pagination and sorting, and returns the results as DTOs.
 * </p>
 */
@RequiredArgsConstructor
@Repository
@Slf4j
public class RoomRepositoryCustomImpl implements RoomRepositoryCustom {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("pricePerNight", "capacity", "type");
    private static final int MAX_PAGE_SIZE = 100;

    private final EntityManager entityManager;
    private final RoomMapper roomMapper;

    /**
     * Searches for rooms that are currently available and meet the specified criteria.
     * <p>
     * The method validates pagination parameters and the requested sort field,
     * constructs a CriteriaQuery to filter by capacity, price, type, and date overlap,
     * applies sorting and pagination hints, executes the query, maps entities to DTOs,
     * and returns a {@link Page} of {@link RoomDTO}.
     * </p>
     *
     * @param filter a {@link RoomFilterRequest} containing pagination, sorting, and filter parameters
     * @return a {@link Page} of {@link RoomDTO} matching the filter criteria
     * @throws IllegalArgumentException if pagination parameters or sort field are invalid
     */
    @Override
    public Page<RoomDTO> findAvailableRooms(RoomFilterRequest filter) {
        log.info("Executing findAvailableRooms with filter: {}", filter);
        validatePaginationParameters(filter);
        validateSortField(filter.getSortBy());

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Room> cq = cb.createQuery(Room.class);
        Root<Room> room = cq.from(Room.class);

        // Join explícito con Booking (requiere relación en la entidad Room)
        Join<Room, Booking> bookingJoin = room.join("bookings", JoinType.LEFT);

        List<Predicate> predicates = buildPredicates(filter, cb, room);

        cq.select(room)
                .where(predicates.toArray(new Predicate[0]))
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


    // --- Private Helper Methods ---

    /**
     * Counts total rooms matching the given filter criteria.
     *
     * @param filter the filter request containing search criteria
     * @param cb CriteriaBuilder for constructing queries
     * @param predicates list of predicates to apply
     * @return the total count of matching rooms
     */
    private long countTotalResults(RoomFilterRequest filter, CriteriaBuilder cb, List<Predicate> predicates) {
        log.debug("Entering countTotalResults with filter={} and {} predicates", filter, predicates.size());

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Room> room = countQuery.from(Room.class);

        // 1. Mismo join que en la consulta principal
        Join<Room, Booking> bookingJoin = room.join("bookings", JoinType.LEFT);

        // 2. Reconstruir predicates usando el join
        List<Predicate> countPredicates = rebuildPredicatesForCount(filter, cb, room);

        countQuery.select(cb.countDistinct(room)) // Usar distinct para evitar duplicados
                .where(countPredicates.toArray(new Predicate[0]));

        long result = entityManager.createQuery(countQuery).getSingleResult();
        log.debug("Count query result: {}", result);
        return result;
    }

    /**
     * Builds a list of JPA predicates based on the filter request.
     *
     * @param filter the filter request containing search criteria
     * @param cb CriteriaBuilder for constructing predicates
     * @param room the root entity for predicates
     * @return list of predicates to apply to the query
     */
    private List<Predicate> buildPredicates(RoomFilterRequest filter, CriteriaBuilder cb, Root<Room> room) {
        log.debug("Building predicates for filter={}", filter);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(room.get("status"), RoomStatus.AVAILABLE));
        addCapacityPredicate(filter, cb, room, predicates);
        addPricePredicates(filter, cb, room, predicates);
        addTypePredicate(filter, cb, room, predicates);
        addAvailabilityPredicate(filter, cb, room, predicates);

        return predicates;
    }

    private List<Predicate> rebuildPredicatesForCount(RoomFilterRequest filter, CriteriaBuilder cb, Root<Room> room) {
        List<Predicate> predicates = new ArrayList<>();

        // Status siempre aplica
        predicates.add(cb.equal(room.get("status"), RoomStatus.AVAILABLE));

        // Fechas
        if (filter.getStartDate() != null && filter.getEndDate() != null) {
            Subquery<Booking> subquery = cb.createQuery().subquery(Booking.class);
            Root<Booking> booking = subquery.from(Booking.class);

            Predicate roomMatch = cb.equal(booking.get("room"), room);
            Predicate dateOverlap = cb.and(
                    cb.lessThan(booking.get("startDate"), filter.getEndDate()),
                    cb.greaterThan(booking.get("endDate"), filter.getStartDate())
            );

            subquery.select(booking).where(cb.and(roomMatch, dateOverlap));
            predicates.add(cb.not(cb.exists(subquery)));
        }

        // Otros filtros (capacidad, precio, tipo)
        if (filter.getCapacity() != null) {
            predicates.add(cb.ge(room.get("capacity"), filter.getCapacity()));
        }
        if (filter.getMinPrice() != null) {
            predicates.add(cb.ge(room.get("pricePerNight"), filter.getMinPrice()));
        }
        if (filter.getMaxPrice() != null) {
            predicates.add(cb.le(room.get("pricePerNight"), filter.getMaxPrice()));
        }
        if (filter.getType() != null) {
            predicates.add(cb.equal(room.get("type"), filter.getType()));
        }

        return predicates;
    }

    /**
     * Adds a predicate ensuring the room is available and not booked in the requested date range.
     *
     * @param filter the filter request containing date range
     * @param cb CriteriaBuilder for constructing queries
     * @param room the root Room entity
     * @param predicates list to which new predicates will be added
     */
    private void addAvailabilityPredicate(RoomFilterRequest filter, CriteriaBuilder cb, Root<Room> room, List<Predicate> predicates) {
        if (filter.getStartDate() != null && filter.getEndDate() != null) {
            Subquery<Booking> subquery = cb.createQuery().subquery(Booking.class);
            Root<Booking> booking = subquery.from(Booking.class);

            Predicate roomMatch = cb.equal(booking.get("room"), room);
            Predicate dateOverlap = cb.and(
                    cb.lessThan(booking.get("startDate"), filter.getEndDate()),
                    cb.greaterThan(booking.get("endDate"), filter.getStartDate())
            );

            subquery.select(booking).where(cb.and(roomMatch, dateOverlap));
            predicates.add(cb.not(cb.exists(subquery)));
        }
    }

    /**
     * Creates an Order clause based on the sort parameters in the filter.
     *
     * @param cb CriteriaBuilder for constructing order
     * @param room the root Room entity
     * @param filter the filter request containing sortBy and order
     * @return Order clause for the query
     */
    private Order createOrder(CriteriaBuilder cb, Root<Room> room, RoomFilterRequest filter) {
        log.debug("Creating order clause: {} {}", filter.getSortBy(), filter.getOrder());
        Path<Object> sortPath = room.get(filter.getSortBy());
        Order orderClause = "desc".equalsIgnoreCase(filter.getOrder())
                ? cb.desc(sortPath)
                : cb.asc(sortPath);
        log.debug("Order clause created: {}", orderClause);
        return orderClause;
    }

    /**
     * Validates that the provided sort field is allowed.
     *
     * @param sortBy the field to sort by
     * @throws IllegalArgumentException if sortBy is not in ALLOWED_SORT_FIELDS
     */
    private void validateSortField(String sortBy) {
        log.debug("Validating sort field: {}", sortBy);
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            log.error("Invalid sort field: {}", sortBy);
            throw new IllegalArgumentException("Invalid sort field: " + sortBy);
        }
    }

    /**
     * Validates pagination parameters (page index and size).
     *
     * @param filter the filter request containing page and size
     * @throws IllegalArgumentException if parameters are out of bounds
     */
    private void validatePaginationParameters(RoomFilterRequest filter) {
        log.debug("Validating pagination parameters: page={}, size={}",
                filter.getPage(), filter.getSize());

        if (filter.getPage() < 0) {
            log.error("Negative page number: {}", filter.getPage());
            throw new IllegalArgumentException("Page number cannot be negative");
        }

        if (filter.getSize() <= 0 || filter.getSize() > MAX_PAGE_SIZE) {
            log.error("Invalid page size: {} (max={})", filter.getSize(), MAX_PAGE_SIZE);
            throw new IllegalArgumentException(
                    "Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    /**
     * Adds a capacity predicate if the filter specifies a minimum capacity.
     *
     * @param filter the filter request
     * @param cb CriteriaBuilder for constructing predicates
     * @param room the root Room entity
     * @param predicates list to which new predicates will be added
     */
    private void addCapacityPredicate(RoomFilterRequest filter,
                                      CriteriaBuilder cb,
                                      Root<Room> room,
                                      List<Predicate> predicates) {
        if (filter.getCapacity() != null) {
            log.debug("Adding capacity predicate: >= {}", filter.getCapacity());
            predicates.add(cb.ge(room.get("capacity"), filter.getCapacity()));
        }
    }

    /**
     * Adds price predicates if the filter specifies min and/or max price.
     *
     * @param filter the filter request
     * @param cb CriteriaBuilder for constructing predicates
     * @param room the root Room entity
     * @param predicates list to which new predicates will be added
     */
    private void addPricePredicates(RoomFilterRequest filter,
                                    CriteriaBuilder cb,
                                    Root<Room> room,
                                    List<Predicate> predicates) {
        if (filter.getMinPrice() != null) {
            log.debug("Adding min price predicate: >= {}", filter.getMinPrice());
            predicates.add(cb.ge(room.get("pricePerNight"), filter.getMinPrice()));
        }
        if (filter.getMaxPrice() != null) {
            log.debug("Adding max price predicate: <= {}", filter.getMaxPrice());
            predicates.add(cb.le(room.get("pricePerNight"), filter.getMaxPrice()));
        }
    }

    /**
     * Adds a type predicate if the filter specifies a room type.
     *
     * @param filter the filter request
     * @param cb CriteriaBuilder for constructing predicates
     * @param room the root Room entity
     * @param predicates list to which new predicates will be added
     */
    private void addTypePredicate(RoomFilterRequest filter,
                                  CriteriaBuilder cb,
                                  Root<Room> room,
                                  List<Predicate> predicates) {
        if (filter.getType() != null) {
            log.debug("Adding type predicate: = {}", filter.getType());
            predicates.add(cb.equal(room.get("type"), filter.getType()));
        }
    }
}



