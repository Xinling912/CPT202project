package com.cpt202.app.repository;

import com.cpt202.app.model.TimeSlot;
import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.model.TimeSlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    // 查找某个专家所有可用时间段，【给客户】查看专家有哪些“空位”可约

    List<TimeSlot> findBySpecialistIdAndStatus(Long specialistId, TimeSlotStatus status);

    // 【给专家】查看自己所有的排班计划（含已订、未订、锁定等）

    List<TimeSlot> findBySpecialistId(Long specialistId);

    // 2. 抢单专用：PBI 4.2 核心，防止两人同时读取到 AVAILABLE
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TimeSlot t WHERE t.id = :id")
    Optional<TimeSlot> findByIdWithLock(@Param("id") Long id);
}