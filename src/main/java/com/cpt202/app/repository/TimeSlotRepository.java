package com.cpt202.app.repository;

import com.cpt202.app.model.TimeSlot;
import com.cpt202.app.model.SpecialistProfile;
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
    // 查找某个专家所有未被预约的时间段
    List<TimeSlot> findBySpecialistAndIsBookedFalse(SpecialistProfile specialist);
    @Lock(LockModeType.PESSIMISTIC_WRITE) // 🚀 关键：悲观锁，防止两人同时读取到 AVAILABLE
    @Query("SELECT t FROM TimeSlot t WHERE t.id = :id")
    Optional<TimeSlot> findByIdWithLock(@Param("id") Long id);
}