package com.cpt202.app.repository;

import com.cpt202.app.model.TimeSlot;
import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.model.TimeSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    // 查找某个专家所有可用时间段
    List<TimeSlot> findBySpecialistIdAndStatus(Long specialistId, TimeSlotStatus status);
    // 查找某个专家所有的排班（不论是否被预约）
    List<TimeSlot> findBySpecialistId(Long specialistId);
}