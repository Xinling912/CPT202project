package com.cpt202.app.repository;

import com.cpt202.app.model.TimeSlot;
import com.cpt202.app.model.SpecialistProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    // 查找某个专家所有未被预约的时间段
    List<TimeSlot> findBySpecialistIdAndIsBookedFalse(Long specialistId);
}