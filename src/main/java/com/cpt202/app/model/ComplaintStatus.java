package com.cpt202.app.model;

public enum ComplaintStatus {
    PENDING("待处理"),
    DISMISSED("已驳回"),
    BANNED("已封禁");

    private final String description;

    ComplaintStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}