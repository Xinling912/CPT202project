package com.cpt202.app.repository;

import com.cpt202.app.model.TimeSlot;
import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.model.TimeSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    // 查找某个专家所有可用时间段
    List<TimeSlot> findBySpecialistIdAndStatus(Long specialistId, TimeSlotStatus status);
    // 查找某个专家所有的排班（不论是否被预约）
    List<TimeSlot> findBySpecialistId(Long specialistId);
    // 查找某个专家在指定日期范围内的所有排班，并按时间先后排序
    List<TimeSlot> findBySpecialistIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(
            Long specialistId, LocalDate startDate, LocalDate endDate);
}