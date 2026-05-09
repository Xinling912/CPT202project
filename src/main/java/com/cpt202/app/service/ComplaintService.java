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
     * User reports a booking
     */
    public Complaint reportBooking(Long bookingId, Long reporterId, String reason) {
        // 1. Check if the booking exists
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking does not exist"));

        // 2. Check if the booking status is COMPLETED
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new RuntimeException("Only completed services can be reported");
        }

        // 3. Check if it has already been reported
        if (complaintRepository.existsByBookingId(bookingId)) {
            throw new RuntimeException("This booking has already been reported, please wait for admin processing");
        }

        // 4. Get the reporter
        User reporter = booking.getCustomer();
        if (!reporter.getId().equals(reporterId)) {
            throw new RuntimeException("You can only report your own bookings");
        }

        // 5. Create the complaint record
        Complaint complaint = new Complaint(booking, reporter, reason);
        return complaintRepository.save(complaint);
    }

    /**Get all complaint records for a specific user (customer)*/
    public List<Complaint> getComplaintsByReporterId(Long reporterId) {
        return complaintRepository.findByReporterId(reporterId);
    }


    /**Get all pending complaints (admin)*/
    public List<Complaint> getPendingComplaints() {
        return complaintRepository.findByStatus(ComplaintStatus.PENDING);
    }

    /**Get all complaints (for admin use)*/
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    /**Handle complaint: Dismiss*/
    public void dismissComplaint(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint record does not exist"));
        complaint.setStatus(ComplaintStatus.DISMISSED);
        complaintRepository.save(complaint);
    }

    /**Handle complaint: Ban the specialist*/
    public void banSpecialistByComplaint(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint record does not exist"));

        // Get the reported specialist
        SpecialistProfile specialist = complaint.getBooking().getSpecialist();
        specialist.setStatus(SpecialistStatus.INACTIVE);
        specialistProfileRepository.save(specialist);

        // Update complaint status
        complaint.setStatus(ComplaintStatus.BANNED);
        complaintRepository.save(complaint);
    }
}