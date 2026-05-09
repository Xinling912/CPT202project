package com.cpt202.app.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class SpecialistProfileEditRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String newRealName;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private SpecialistLevel newLevel;

    @ManyToOne
    @JoinColumn(name = "profile_id", nullable = false)
    private SpecialistProfile specialistProfile;

    private BigDecimal newHourlyFee;
    private String newResume;

    @ManyToOne
    @JoinColumn(name = "new_expertise_id")
    private ExpertiseCategory newExpertise;

    private String newProposedExpertiseName;

    @Enumerated(EnumType.STRING)
    private SpecialistProfileEditStatus status = SpecialistProfileEditStatus.PENDING; // Approval status

    private LocalDateTime submitTime = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SpecialistProfile getSpecialistProfile() {
        return specialistProfile;
    }

    public void setSpecialistProfile(SpecialistProfile specialistProfile) {
        this.specialistProfile = specialistProfile;
    }

    public BigDecimal getNewHourlyFee() {
        return newHourlyFee;
    }

    public void setNewHourlyFee(BigDecimal newHourlyFee) {
        this.newHourlyFee = newHourlyFee;
    }

    public String getNewResume() {
        return newResume;
    }

    public void setNewResume(String newResume) {
        this.newResume = newResume;
    }

    public ExpertiseCategory getNewExpertise() {
        return newExpertise;
    }

    public void setNewExpertise(ExpertiseCategory newExpertise) {
        this.newExpertise = newExpertise;
    }

    public String getNewProposedExpertiseName() {
        return newProposedExpertiseName;
    }

    public void setNewProposedExpertiseName(String newProposedExpertiseName) {
        this.newProposedExpertiseName = newProposedExpertiseName;
    }

    public SpecialistProfileEditStatus getStatus() {
        return status;
    }

    public void setStatus(SpecialistProfileEditStatus status) {
        this.status = status;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public String getNewRealName() {
        return newRealName;
    }

    public void setNewRealName(String newRealName) {
        this.newRealName = newRealName;
    }

    public SpecialistLevel getNewLevel() {
        return newLevel;
    }

    public void setNewLevel(SpecialistLevel newLevel) {
        this.newLevel = newLevel;
    }
}