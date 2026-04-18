package com.cpt202.app.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "time_slot")
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 谁的排班？关联到专家名片
    // Ignore：JPA 查数据库时照常连表，但转成 JSON 时忽略这个字段！
    // 防止specialistProfile字段反复出现
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "specialist_id", referencedColumnName = "id", nullable = false)
    private SpecialistProfile specialist;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate; // 日期：如 2026-03-30

    @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8")
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime; // 开始时间：如 14:00

    @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8")
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime; // 结束时间：如 15:00


    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TimeSlotStatus status = TimeSlotStatus.AVAILABLE;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SpecialistProfile getSpecialist() {
        return specialist;
    }

    public void setSpecialist(SpecialistProfile specialist) {
        this.specialist = specialist;
    }

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public void setSlotDate(LocalDate slotDate) {
        this.slotDate = slotDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public TimeSlotStatus getStatus() {
        return status;
    }

    public void setStatus(TimeSlotStatus status) {
        this.status = status;
    }
}