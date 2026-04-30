package com.cpt202.app.service;

import com.cpt202.app.model.BookingStatus;
import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.model.TimeSlot;
import com.cpt202.app.model.TimeSlotStatus;
import com.cpt202.app.repository.SpecialistProfileRepository;
import com.cpt202.app.repository.TimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 新增事务注解

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonFormat;

@Service
public class TimeSlotService {

    public record TimeSlotWithBookingDTO(
            Long id,
            @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8") LocalDate slotDate,
            @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8") LocalTime startTime,
            @JsonFormat(pattern = "HH:mm:ss", timezone = "GMT+8") LocalTime endTime,
            TimeSlotStatus timeSlotStatus,
            BookingStatus bookingStatus,
            String customerUsername,
            String customerNotes,
            Long bookingId
    ) {}

    public record TimeSlotBatchRequest(List<DailySlot> slots) {}

    public record DailySlot(LocalDate date, LocalTime startTime, LocalTime endTime) {}

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private SpecialistProfileRepository specialistRepository;

    /**
     * 获取未来两周内专家的可用日期
     */
    public List<LocalDate> getAvailableDatesForSpecialist(Long specialistId) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusWeeks(2);
        
        List<TimeSlot> allSlots = timeSlotRepository.findBySpecialistId(specialistId);
        
        return allSlots.stream()
                .filter(slot -> slot.getStatus() == TimeSlotStatus.AVAILABLE)
                .filter(slot -> !slot.getSlotDate().isBefore(startDate) && !slot.getSlotDate().isAfter(endDate))
                .map(TimeSlot::getSlotDate)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**获取特定日期的专家可用时段
     * Get available time slots for a specialist on a specific date
     */
    public List<TimeSlot> getAvailableTimeSlotsForDate(Long specialistId, LocalDate date) {
        List<TimeSlot> allSlots = timeSlotRepository.findBySpecialistId(specialistId);
        
        return allSlots.stream()
                .filter(slot -> slot.getStatus() == TimeSlotStatus.AVAILABLE)
                .filter(slot -> slot.getSlotDate().equals(date))
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .collect(Collectors.toList());
    }

    /**获取本周和下周专家的所有时间段
     * Get all time slots for a specialist within current week and next week
     */
    public List<TimeSlot> getAllTimeSlotsForSpecialistSchedule(Long specialistId) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusWeeks(2);
        
        List<TimeSlot> allSlots = timeSlotRepository.findBySpecialistId(specialistId);
        
        return allSlots.stream()
                .filter(slot -> !slot.getSlotDate().isBefore(startDate) && !slot.getSlotDate().isAfter(endDate))
                .sorted((a, b) -> {
                    int dateCompare = a.getSlotDate().compareTo(b.getSlotDate());
                    if (dateCompare != 0) return dateCompare;
                    return a.getStartTime().compareTo(b.getStartTime());
                })
                .collect(Collectors.toList());
    }

    /**获取本周或目标周内专家的所有日期和时间段
     * Get all dates with time slots for a specialist within current week or target week
     */
    public List<TimeSlot> getSpecialistScheduleByDateRange(Long specialistId, LocalDate startDate, LocalDate endDate) {
        // 调用底层 Repository 去查这 7 天内的数据，并按日期和时间排序
        return timeSlotRepository.findBySpecialistIdAndSlotDateBetweenOrderBySlotDateAscStartTimeAsc(specialistId, startDate, endDate);
    }



    /**
     * 批量发布排班（前端传来一周的勾选数据）
     */
    public void batchCreateSlots(String username, TimeSlotBatchRequest request) {
        SpecialistProfile profile = specialistRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("专家档案不存在"));

        Long specId = profile.getId();
        List<TimeSlot> slotsToSave = new ArrayList<>();

        for (DailySlot dailySlot : request.slots()) {
            if (dailySlot.date().isBefore(LocalDate.now())) {
                throw new RuntimeException("不能发布过去的排班：" + dailySlot.date());
            }

            // 防重叠校验（需在 Repository 中添加 existOverlappingSlot 方法）
            boolean isOverlapping = timeSlotRepository.existsOverlappingSlot(
                    specId, dailySlot.date(), dailySlot.startTime(), dailySlot.endTime());

            if (isOverlapping) {
                throw new RuntimeException("时间段发生重叠，请检查：" + dailySlot.date() + " " + dailySlot.startTime());
            }

            TimeSlot timeSlot = new TimeSlot();
            timeSlot.setSpecialist(profile);
            timeSlot.setSlotDate(dailySlot.date());
            timeSlot.setStartTime(dailySlot.startTime());
            timeSlot.setEndTime(dailySlot.endTime());
            timeSlot.setStatus(TimeSlotStatus.AVAILABLE);

            slotsToSave.add(timeSlot);
        }

        timeSlotRepository.saveAll(slotsToSave);
    }

    /**
     * 专家管理：删除某个未被预约的排班
     */
    public void deleteTimeSlot(String username, Long slotId) {
        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("排班不存在"));

        // 安全校验：确认是本人的排班
        if (!slot.getSpecialist().getUser().getUsername().equals(username)) {
            throw new RuntimeException("无权操作他人的排班");
        }

        // 业务规则：只能删除 AVAILABLE（未被预约）的排班
        if (slot.getStatus() != TimeSlotStatus.AVAILABLE) {
            throw new RuntimeException("该时间段已被预约或锁定，无法删除！请先联系客户取消订单。");
        }

        timeSlotRepository.delete(slot);
    }
}
