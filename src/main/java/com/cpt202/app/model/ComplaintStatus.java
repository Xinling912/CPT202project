package com.cpt202.app.model;

public enum ComplaintStatus {
    PENDING("Pending"),
    DISMISSED("Dismissed"),
    BANNED("Banned");

    private final String description;

    ComplaintStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}