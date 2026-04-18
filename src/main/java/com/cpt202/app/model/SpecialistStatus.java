package com.cpt202.app.model;

public enum SpecialistStatus {
    ACTIVE,   // 接单中：前端会展示在搜索列表中
    INACTIVE,  // 封禁中：前端不展示，或者显示“暂不接单”
    PENDING  // 刚申请或修改资料后，等待管理员审批
}