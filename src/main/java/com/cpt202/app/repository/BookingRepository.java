package com.cpt202.app.repository;

import com.cpt202.app.model.Booking;
import com.cpt202.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
//JpaRepository<Booking, Long>：第一个参数实体类名，第二个参数主键的数据类型
public interface BookingRepository extends JpaRepository<Booking, Long> {
    // 查找某个顾客的所有订单
    List<Booking> findByCustomer(User customer);
}