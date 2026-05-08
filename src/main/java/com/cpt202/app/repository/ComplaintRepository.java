package com.cpt202.app.repository;

import com.cpt202.app.model.Complaint;
import com.cpt202.app.model.ComplaintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // Check if the booking has already been reported
    boolean existsByBookingId(Long bookingId);

    // Find complaints by status
    List<Complaint> findByStatus(ComplaintStatus status);

    // Find all complaints by reporter ID
    List<Complaint> findByReporterId(Long reporterId);
}