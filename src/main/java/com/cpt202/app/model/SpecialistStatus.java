package com.cpt202.app.model;

public enum SpecialistStatus {
    ACTIVE,   // Taking orders: Will be displayed in the search list on the frontend
    INACTIVE,  // Suspended: Not displayed on the frontend, or displayed as "Temporarily not taking orders"
    PENDING,  // Just applied or modified profile, waiting for admin approval
    REJECTED  // Specialist application failed review
}