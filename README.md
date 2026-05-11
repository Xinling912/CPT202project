# SAS - Specialist Appointment System

> A web-based booking platform for consultancy services.

## Group Information

- **Group Number**: Group 13
- **Module**: CPT202 Software Engineering

---

## Project Overview

This system allows customers to browse specialists, check available time slots, and create bookings. Specialists can manage their schedules and confirm or complete appointments. Administrators can approve specialist applications, manage expertise categories, and handle customer complaints.

---

## Key Features

- User registration and login (email verification, JWT authentication)
- Role‑based access control (Customer / Specialist / Administrator)
- Specialist hall: browse, search, filter by expertise, level, and availability
- Specialist profile management and onboarding approval
- Schedule management (publish, view, delete time slots)
- Booking creation, confirmation, completion, and cancellation
- Complaint submission and admin handling
- Expertise category management (admin only)

---

## Getting Started

### Prerequisites

- JDK 17 or 21
- MySQL 8.0
- Maven (or use the included Maven Wrapper)

### Run Locally

1. Clone the repository
2. Configure database connection in `src/main/resources/application.properties`
3. Create the database:
   ```sql
   CREATE DATABASE booking_system;
Build and run:

bash
mvn clean spring-boot:run
Access the application at http://localhost:8080

Deployment
The first release is deployed on Alibaba Cloud ECS:

URL: http://121.41.102.221:8080/landingpage.html

OS: Ubuntu 22.04

Java: 21

Database: MySQL 8.0 (local instance)