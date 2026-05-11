package com.cpt202.app.model;

public enum ApplicationStatus {
    NONE,                // Never applied
    APPLY_PENDING,       // Specialist application pending review
    APPLY_REJECTED,      // Specialist application rejected
    IS_ACTIVE_SPECIALIST,// Already an active specialist
    EDIT_PENDING,        // Profile edit pending review
    EDIT_APPROVED,       // Edit approved
    EDIT_REJECTED        // Edit rejected
}