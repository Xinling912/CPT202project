package com.cpt202.app.repository;

import com.cpt202.app.model.Booking;
import com.cpt202.app.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;


import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Query whether the customer has had a booking with a specific status in this time slot (used to check for CANCELLED to intercept spamming)
    boolean existsByCustomerIdAndTimeSlotIdAndStatus(Long customerId, Long timeSlotId, BookingStatus status);
    List<Booking> findByStatus(BookingStatus status);

    // Query by customer ID
    List<Booking> findByCustomerId(Long customerId);

    // Query by specialist ID
    List<Booking> findBySpecialistId(Long specialistId);

    // Check if the user already has a valid booking in a specific time slot
    boolean existsByCustomerIdAndTimeSlotIdAndStatusIn(
            Long customerId,
            Long timeSlotId,
            List<BookingStatus> activeStatuses
    );

    //
    long countByCustomerIdAndStatusAndCreatedAtAfter(
            Long customerId,
            BookingStatus status,
            LocalDateTime date);

    // Query all valid bookings based on the specialist's ID and booking status
    List<Booking> findBySpecialistIdAndStatusIn(Long specialistId, List<BookingStatus> statuses);

    // Accumulate the total amount of completed bookings for the specialist
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b " +
            "WHERE b.specialist.id = :specialistId AND b.status = :status")
    BigDecimal sumTotalAmountBySpecialistIdAndStatus(
            @Param("specialistId") Long specialistId,
            @Param("status") BookingStatus status
    );

}