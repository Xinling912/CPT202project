package com.cpt202.app.model;

public enum ApplicationStatus {
    NONE,                // 从未申请过
    APPLY_PENDING,       // 专家申请审核中
    APPLY_REJECTED,      // 专家申请被驳回
    IS_ACTIVE_SPECIALIST,// 已是正式专家
    EDIT_PENDING,        // 修改资料审核中
    EDIT_APPROVED,       // 修改已通过
    EDIT_REJECTED        // 修改被驳回
}
