package com.cpt202.app.service;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.BookingRepository;
import com.cpt202.app.repository.ComplaintRepository;
import com.cpt202.app.repository.SpecialistProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final BookingRepository bookingRepository;
    private final SpecialistProfileRepository specialistProfileRepository;

    public ComplaintService(ComplaintRepository complaintRepository,
                            BookingRepository bookingRepository,
                            SpecialistProfileRepository specialistProfileRepository) {
        this.complaintRepository = complaintRepository;
        this.bookingRepository = bookingRepository;
        this.specialistProfileRepository = specialistProfileRepository;
    }

    /**
     * 用户举报订单
     */
    public Complaint reportBooking(Long bookingId, Long reporterId, String reason) {
        // 1. 检查订单是否存在
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 2. 检查订单状态是否为 COMPLETED
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new RuntimeException("只有已完成的服务才能进行举报");
        }

        // 3. 检查是否已经举报过
        if (complaintRepository.existsByBookingId(bookingId)) {
            throw new RuntimeException("该订单已被举报过，请等待管理员处理");
        }

        // 4. 获取举报人
        User reporter = booking.getCustomer();
        if (!reporter.getId().equals(reporterId)) {
            throw new RuntimeException("只能举报自己的订单");
        }

        // 5. 创建举报记录
        Complaint complaint = new Complaint(booking, reporter, reason);
        return complaintRepository.save(complaint);
    }

    /**
     * 获取某个用户的所有举报记录(user)
     */
    public List<Complaint> getComplaintsByReporterId(Long reporterId) {
        return complaintRepository.findByReporterId(reporterId);
    }


    /**
     * 获取所有待处理的举报（admin）
     */
    public List<Complaint> getPendingComplaints() {
        return complaintRepository.findByStatus(ComplaintStatus.PENDING);
    }

    /**
     * 获取所有举报（管理员用）
     */
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    /**
     * 处理举报：驳回（不封禁）
     */
    public void dismissComplaint(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("举报记录不存在"));
        complaint.setStatus(ComplaintStatus.DISMISSED);
        complaintRepository.save(complaint);
    }

    /**
     * 处理举报：封禁专家
     */
    public void banSpecialistByComplaint(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("举报记录不存在"));

        // 获取被举报的专家
        SpecialistProfile specialist = complaint.getBooking().getSpecialist();
        specialist.setStatus(SpecialistStatus.INACTIVE);
        specialistProfileRepository.save(specialist);

        // 更新举报状态
        complaint.setStatus(ComplaintStatus.BANNED);
        complaintRepository.save(complaint);
    }
}