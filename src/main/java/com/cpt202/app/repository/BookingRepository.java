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
//JpaRepository<Booking, Long>：第一个参数实体类名，第二个参数主键的数据类型
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // 查询该顾客是否在这个时间段有过特定状态的订单（用于查 CANCELLED 拦截刷单）
    boolean existsByCustomerIdAndTimeSlotIdAndStatus(Long customerId, Long timeSlotId, BookingStatus status);
    List<Booking> findByStatus(BookingStatus status);

    // 按照用户ID查询
    List<Booking> findByCustomerId(Long customerId);

    // 按照专家ID查询
    List<Booking> findBySpecialistId(Long specialistId);

    // 检查用户在特定时间段是否已有有效预约
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

    // 根据专家的 ID 和 订单状态 查询所有有效订单
    List<Booking> findBySpecialistIdAndStatusIn(Long specialistId, List<BookingStatus> statuses);

    // 累加专家已完成订单的总金额
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Booking b " +
            "WHERE b.specialist.id = :specialistId AND b.status = :status")
    BigDecimal sumTotalAmountBySpecialistIdAndStatus(
            @Param("specialistId") Long specialistId,
            @Param("status") BookingStatus status
    );

}