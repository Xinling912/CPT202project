package com.cpt202.app.model;

public enum TimeSlotStatus {
    AVAILABLE, // 可选：前端展示在预约列表中
    BOOKED,    // 已预约：前端不展示，且与 Booking 表关联
    DISABLED,  // 专家禁用：专家临时有事手动关闭，既不可选也不属于任何订单
}