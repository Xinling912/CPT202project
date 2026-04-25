package com.cpt202.app.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "specialist_profile")
public class SpecialistProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    @ManyToOne
    @JoinColumn(name = "expertise_id", referencedColumnName = "id", nullable = true)
    private ExpertiseCategory expertise;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private SpecialistLevel level;

    @Column(name = "hourly_fee", precision = 10, scale = 2, nullable = false)
    private BigDecimal hourlyFee;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SpecialistStatus status;


    @Column(columnDefinition = "TEXT") // 指定为 TEXT 类型，匹配数据库
    private String resume;


    @Column(name = "real_name", length = 100)
    private String realName;


    @Column(name = "proposed_expertise_name", length = 100)
    private String proposedExpertiseName;



    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getProposedExpertiseName() {
        return proposedExpertiseName;
    }

    public void setProposedExpertiseName(String proposedExpertiseName) {
        this.proposedExpertiseName = proposedExpertiseName;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ExpertiseCategory getExpertise() {
        return expertise;
    }

    public void setExpertise(ExpertiseCategory expertise) {
        this.expertise = expertise;
    }

    public SpecialistLevel getLevel() {
        return level;
    }

    public void setLevel(SpecialistLevel level) {
        this.level = level;
    }

    public BigDecimal getHourlyFee() {
        return hourlyFee;
    }

    public void setHourlyFee(BigDecimal hourlyFee) {
        this.hourlyFee = hourlyFee;
    }

    public SpecialistStatus getStatus() {
        return status;
    }

    public void setStatus(SpecialistStatus status) {
        this.status = status;
    }

    public String getResume() {
        return resume;
    }

    public void setResume(String resume) {
        this.resume = resume;
    }
}