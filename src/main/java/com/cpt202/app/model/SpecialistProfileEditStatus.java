package com.cpt202.app.model;

public enum SpecialistProfileEditStatus {
    PENDING,   // Specialist just submitted the edit, waiting for admin approval
    APPROVED,  // Admin approved, data has been synchronized to the main table
    REJECTED   // Admin rejected, the original main table is unaffected
}