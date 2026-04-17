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

    // 谁买的？关联顾客 (User)
    // 优化：在订单里，我们只需要顾客的 id 和 username，忽略掉他的注册时间、角色等不重要的信息
    @JsonIgnoreProperties({"createdAt", "role", "password", "email"})
    @ManyToOne
    @JoinColumn(name = "customer_id", referencedColumnName = "id", nullable = false)
    private User customer;

    // 买了哪个专家？关联专家名片
    @JsonIgnoreProperties({"expertise", "hourlyFee"})
    @ManyToOne
    @JoinColumn(name = "specialist_id", referencedColumnName = "id", nullable = false)
    private SpecialistProfile specialist;

    // 买了哪个时间段？
    // 核心防并发设计：用 @OneToOne 加上 unique = true
    // 数据库在物理层面保证：同一个 TimeSlot 绝对不可能出现在两个订单里！
    @OneToOne
    @JoinColumn(name = "time_slot_id", referencedColumnName = "id", nullable = false, unique = true)
    private TimeSlot timeSlot;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private BookingStatus status; // PENDING, CONFIRMED, COMPLETED, CANCELLED

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(columnDefinition = "TEXT")
    private String notes; // 顾客的留言/诉求

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 下单时间

    // TODO: 请生成 Getter 和 Setter
    public Long getId() {
        return id;
    }
    //id set仅用于测试
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
    //正常情况不会允许手动改动或传入下单时间
    public void setTimeSlot(TimeSlot timeSlot) {
        this.timeSlot = timeSlot;
    }
    //PrePersist作用是监听这个对象的一生。
    //当 Hibernate 准备把这个对象存入数据库时，会自动触发这个被 @PrePersist 标记的方法。
    //不需要（也不应该）手动去写 booking.setCreatedAt(LocalDateTime.now())
    //当调用 bookingRepository.save(booking)时，框架在生成 SQL 的前一毫秒，自动帮你执行 onCreate() 方法
    // 把当前的精准时间“悄悄”塞进 createdAt 属性里
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