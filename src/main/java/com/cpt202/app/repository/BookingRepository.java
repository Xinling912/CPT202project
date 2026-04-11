package com.cpt202.app.repository;

import com.cpt202.app.model.Booking;
import com.cpt202.app.model.BookingStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;



@Repository
//JpaRepository<Booking, Long>：第一个参数实体类名，第二个参数主键的数据类型
public interface BookingRepository extends JpaRepository<Booking, Long> {
    // 【PBI 4.2 核心】防冲突：检查该时段是否已有订单
    boolean existsByTimeSlotId(Long timeSlotId);

    // Spring Data JPA 会根据方法名自动生成 SQL，不需要手写
    long countByCustomerIdAndStatusAndCreatedAtAfter(Long customerId, BookingStatus status, LocalDateTime date);




}

