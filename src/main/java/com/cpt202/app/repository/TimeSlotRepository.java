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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    // 查找某个专家所有可用时间段
    List<TimeSlot> findBySpecialistIdAndStatus(Long specialistId, TimeSlotStatus status);
    // 查找某个专家所有的排班（不论是否被预约）
    List<TimeSlot> findBySpecialistId(Long specialistId);
    // 查找某个专家在指定日期范围内的所有排班，并按时间先后排序
    List<TimeSlot> findBySpecialistIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(
            Long specialistId, LocalDate startDate, LocalDate endDate);
    // 检查是否存在重叠的时间段
    //重叠条件： 已存在记录的开始时间 < 新申请的结束时间  AND  已存在记录的结束时间 > 新申请的开始时间
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END " +
            "FROM TimeSlot t WHERE t.specialist.id = :specialistId " +
            "AND t.slotDate = :date " +
            "AND t.startTime < :endTime " +
            "AND t.endTime > :startTime")
    boolean existsOverlappingSlot(@Param("specialistId") Long specialistId,
                                  @Param("date") LocalDate date,
                                  @Param("startTime") LocalTime startTime,
                                  @Param("endTime") LocalTime endTime);

    // 抢单专用：PBI 4.2 核心，防止两人同时读取到 AVAILABLE
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TimeSlot t WHERE t.id = :id")
    Optional<TimeSlot> findByIdWithLock(@Param("id") Long id);
}