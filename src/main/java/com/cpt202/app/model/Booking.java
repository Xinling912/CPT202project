package com.cpt202.app.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who bought it? Associated customer (User)
    // Optimization: In the booking, we only need the customer's id and username, ignoring unimportant information like registration time, role, etc.
    @JsonIgnoreProperties({"createdAt", "role", "password", "email"})
    @ManyToOne
    @JoinColumn(name = "customer_id", referencedColumnName = "id", nullable = false)
    private User customer;

    // Which specialist was booked? Associated specialist profile
    @JsonIgnoreProperties({"expertise", "hourlyFee"})
    @ManyToOne
    @JoinColumn(name = "specialist_id", referencedColumnName = "id", nullable = false)
    private SpecialistProfile specialist;

    // Which time slot was booked?
    // Core concurrency prevention design: Use @OneToOne with unique = true
    // The database guarantees at the physical level: The same TimeSlot can absolutely not appear in two bookings!
    @OneToOne
    @JoinColumn(name = "time_slot_id", referencedColumnName = "id", nullable = false, unique = true)
    private TimeSlot timeSlot;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private BookingStatus status; // PENDING, CONFIRMED, COMPLETED, CANCELLED

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(columnDefinition = "TEXT")
    private String notes; // Customer's notes/requests

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // Booking creation time

    // TODO: Please generate Getters and Setters
    public Long getId() {
        return id;
    }
    // id setter is only used for testing
    public void setId(Long id) {
        this.id = id;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public SpecialistProfile getSpecialist() {
        return specialist;
    }

    public void setSpecialist(SpecialistProfile specialist) {
        this.specialist = specialist;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }
    // Normally, manual modification or passing in the booking time is not allowed
    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }
    // The role of PrePersist is to listen to the lifecycle of this object.
    // When Hibernate prepares to save this object to the database, it will automatically trigger this method marked with @PrePersist.
    // There is no need (and you shouldn't) to manually write booking.setCreatedAt(LocalDateTime.now())
    // When calling bookingRepository.save(booking), the framework will automatically execute the onCreate() method for you a millisecond before generating the SQL
    // And "quietly" inject the current precise time into the createdAt property
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}