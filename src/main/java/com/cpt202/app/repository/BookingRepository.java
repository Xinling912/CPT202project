package com.cpt202.app.repository;

import com.cpt202.app.model.Booking;
import com.cpt202.app.model.BookingStatus;
import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
//JpaRepository<Booking, Long>：第一个参数实体类名，第二个参数主键的数据类型
public interface BookingRepository extends JpaRepository<Booking, Long> {
    // 查找某个顾客的所有订单
    List<Booking> findByCustomer(User customer);

    // 查找某个专家的所有订单
    List<Booking> findBySpecialist(SpecialistProfile Specialist);

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

    // Spring Data JPA 会根据方法名自动生成 SQL，不需要手写
    long countByCustomerIdAndStatusAndCreatedAtAfter(
            Long customerId,
            BookingStatus status,
            LocalDateTime date);

    // 进阶魔法：查专家ID，并且订单状态必须在我们给定的集合(In)里面
    List<Booking> findByTimeSlot_Specialist_IdAndStatusIn(Long specialistId, List<BookingStatus> statuses);
    // 根据专家的 ID 和 订单状态 查询所有有效订单
    List<Booking> findBySpecialistIdAndStatusIn(Long specialistId, List<BookingStatus> statuses);

}