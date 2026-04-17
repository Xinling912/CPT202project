package com.cpt202.app.controller;

import com.cpt202.app.model.Booking;
import com.cpt202.app.model.BookingStatus;
import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.model.TimeSlot;
import com.cpt202.app.model.TimeSlotStatus;
import com.cpt202.app.model.User;
import com.cpt202.app.model.UserRole;
import com.cpt202.app.repository.BookingRepository;
import com.cpt202.app.repository.SpecialistProfileRepository;
import com.cpt202.app.service.TimeSlotService;
import com.cpt202.app.service.TimeSlotService.TimeSlotWithBookingDTO;
import com.cpt202.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/timeslots")
@CrossOrigin
public class TimeSlotController {

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SpecialistProfileRepository specialistProfileRepository;

    @Autowired
    private UserService userService;

    /**
     * 【需求一：用户视角】
     * 1. 获取专家未来两周内有空闲时间段的日期列表 (用于点亮日历)
     */
    @GetMapping("/specialist/{specialistId}/available-dates")
    public Map<String, Object> getAvailableDates(@PathVariable Long specialistId) {
        Map<String, Object> response = new HashMap<>();
        List<LocalDate> availableDates = timeSlotService.getAvailableDatesForSpecialist(specialistId);
        response.put("availableDates", availableDates);
        return response;
    }

    /**
     * 【需求一：用户视角】
     * 2. 当用户点击某一天时，获取该专家这一天的所有可用时间段 (用于展示具体可选时间)
     */
    @GetMapping("/specialist/{specialistId}/available-times")
    public Map<String, Object> getAvailableTimeSlots(
            @PathVariable Long specialistId,
            @RequestParam String date) {
        Map<String, Object> response = new HashMap<>();
        LocalDate targetDate = LocalDate.parse(date);
        List<TimeSlot> timeSlots = timeSlotService.getAvailableTimeSlotsForDate(specialistId, targetDate);
        response.put("timeSlots", timeSlots);
        return response;
    }

    /**
     * 【需求二：专家视角】
     * 3. 专家查看专家查看指定周（或默认本周）的排班表（包含空闲时间段和已有订单的详细信息）
     */
    @GetMapping("/my-schedule")
    public Map<String, Object> getMySchedule(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // 0. 从 JWT 获取当前用户，并连表查出专家的真实 Profile ID
        Long actualSpecialistId = getAuthenticatedSpecialistId(authentication);

        Map<String, Object> response = new HashMap<>();

        // 1. 智能计算“本周”范围：如果前端没传日期，后端自动计算当前的周一到周日
        if (startDate == null || endDate == null) {
            LocalDate today = LocalDate.now();
            startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            endDate = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        }

        // 2. 获取这 7 天内的所有排班格子 (TimeSlot)
        List<TimeSlot> timeSlots = timeSlotService.getSpecialistScheduleByDateRange(actualSpecialistId, startDate, endDate);

        // 3. 获取这 7 天内相关的有效订单 (Booking)
        // 直接用 booking.specialist_id 查
        List<Booking> bookings = bookingRepository.findBySpecialistIdAndStatusIn(
                actualSpecialistId,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.COMPLETED)
        );

        // 4. 将订单转为字典(Map)进行缝合
        Map<Long, Booking> bookingMap = bookings.stream()
                .collect(Collectors.toMap(b -> b.getTimeSlot().getId(), b -> b));

        // 5. 组装成包含完整状态的 DTO 列表 (使用 Record)
        List<TimeSlotWithBookingDTO> scheduleDTOs = timeSlots.stream()
                .map(timeSlot -> {
                    Booking booking = bookingMap.get(timeSlot.getId());
                    return new TimeSlotWithBookingDTO(
                            timeSlot.getId(),
                            timeSlot.getSlotDate(),
                            timeSlot.getStartTime(),
                            timeSlot.getEndTime(),
                            timeSlot.getStatus(),
                            booking != null ? booking.getStatus() : null,
                            booking != null ? booking.getCustomer().getUsername() : null,
                            booking != null ? booking.getNotes() : null,
                            booking != null ? booking.getId() : null
                    );
                })
                .collect(Collectors.toList());

        // 6. 按日期分组 (备选数据结构)
        Map<LocalDate, List<TimeSlotWithBookingDTO>> scheduleByDate = scheduleDTOs.stream()
                .collect(Collectors.groupingBy(TimeSlotWithBookingDTO::slotDate));

        // 7. 返回日历组件最爱的扁平数组，同时告知当前渲染的周范围
        response.put("flatSchedule", scheduleDTOs);
        response.put("scheduleGrouped", scheduleByDate);
        response.put("currentWeekStart", startDate);
        response.put("currentWeekEnd", endDate);

        return response;
    }

    /**
     * 【专家面板】
     * 4. 获取专家排班和预约的全局数据统计 (可用于前端仪表盘展示)
     */
    @GetMapping("/my-schedule-summary")
    public Map<String, Object> getMyScheduleSummary(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Long actualSpecialistId = getAuthenticatedSpecialistId(authentication);
        Map<String, Object> response = new HashMap<>();

        // 统计面板的日期计算逻辑同步
        if (startDate == null || endDate == null) {
            LocalDate today = LocalDate.now();
            startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            endDate = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        }

        List<TimeSlot> timeSlots = timeSlotService.getSpecialistScheduleByDateRange(actualSpecialistId, startDate, endDate);
        List<Booking> bookings = bookingRepository.findBySpecialistIdAndStatusIn(
                actualSpecialistId,
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.COMPLETED)
        );

        long totalSlots = timeSlots.size();
        long availableSlots = timeSlots.stream().filter(ts -> ts.getStatus() == TimeSlotStatus.AVAILABLE).count();
        long bookedSlots = timeSlots.stream().filter(ts -> ts.getStatus() == TimeSlotStatus.BOOKED).count();
        long disabledSlots = timeSlots.stream().filter(ts -> ts.getStatus() == TimeSlotStatus.DISABLED).count();

        long pendingBookings = bookings.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();
        long confirmedBookings = bookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long completedBookings = bookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();

        response.put("totalSlots", totalSlots);
        response.put("availableSlots", availableSlots);
        response.put("bookedSlots", bookedSlots);
        response.put("disabledSlots", disabledSlots);
        response.put("pendingBookings", pendingBookings);
        response.put("confirmedBookings", confirmedBookings);
        response.put("completedBookings", completedBookings);
        response.put("summaryRange", startDate + " to " + endDate);

        return response;
    }

    /**
     * 内部核心安全方法：从 JWT 中提取身份，校验专家角色，并查出真实的 Profile ID
     */
    private Long getAuthenticatedSpecialistId(Authentication authentication) {
        String currentUsername = authentication.getName();
        User currentUser = userService.getByUsername(currentUsername);

        if (currentUser.getRole() != UserRole.SPECIALIST) {
            throw new AccessDeniedException("访问被拒绝：当前用户不是专家身份");
        }

        SpecialistProfile profile = specialistProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("数据异常：未找到该账号关联的专家档案"));

        return profile.getId();
    }
}