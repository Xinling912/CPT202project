package com.cpt202.app.repository;

import com.cpt202.app.model.Complaint;
import com.cpt202.app.model.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // 检查订单是否已被举报
    boolean existsByBookingId(Long bookingId);

    // 按状态查询举报
    List<Complaint> findByStatus(ComplaintStatus status);

    // 根据举报人 ID 查询该用户所有举报
    List<Complaint> findByReporterId(Long reporterId);
}