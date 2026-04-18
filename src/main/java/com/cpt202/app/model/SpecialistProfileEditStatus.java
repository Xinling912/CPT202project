package com.cpt202.app.model;

public enum SpecialistProfileEditStatus {
    PENDING,   // 专家刚提交修改，等待管理员审核
    APPROVED,  // 管理员已批准，数据已同步到主表
    REJECTED   // 管理员已驳回，原主表不受影响
}
