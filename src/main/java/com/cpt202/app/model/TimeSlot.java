package com.cpt202.app.model;

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
    @ManyToOne
    @JoinColumn(name = "specialist_id", referencedColumnName = "id", nullable = false)
    private SpecialistProfile specialist;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate; // 日期：如 2026-03-30

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime; // 开始时间：如 14:00

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime; // 结束时间：如 15:00

    //把timeslotstatus 从boolean改成enum
    @Column(name = "status", nullable = false, length = 20)
    private String status = "AVAILABLE";



    // TODO: 请生成 Getter 和 Setter
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



    //  这里的命名必须是 getStatus，BookingService
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}