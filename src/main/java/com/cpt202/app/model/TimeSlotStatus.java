package com.cpt202.app.model;

public enum TimeSlotStatus {
    AVAILABLE, // Available: Displayed in the booking list on the frontend
    BOOKED,    // Booked: Not displayed on the frontend, and associated with the Booking table
    DISABLED // Disabled by specialist: Manually closed by the specialist due to temporary unavailability, neither selectable nor belonging to any booking
}