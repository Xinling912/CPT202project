package com.cpt202.app.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "specialist_profile")
public class SpecialistProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 与 User 的一对一关系：保证一个账号只能有一张名片
    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    // 专家的真实姓名
    @Column(nullable = false, length = 50)
    private String realName;

    // 与专业分类的多对一关系：多个专家可以属于同一个专业
    @ManyToOne
    @JoinColumn(name = "expertise_id", referencedColumnName = "id", nullable = false)
    private ExpertiseCategory expertise;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private SpecialistLevel level;

    @Column(name = "hourly_fee", precision = 10, scale = 2, nullable = false)
    private BigDecimal hourlyFee;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SpecialistStatus status = SpecialistStatus.PENDING; // 默认是待审核;
    // ACTIVE (接单中), INACTIVE (禁止接单中), PENDING (待审批)

    // 个人简历介绍
    @Column(columnDefinition = "TEXT")
    private String resume;

    // 存放用户填写的“其他”专业（审批通过前临时存放）
    @Column(name = "proposed_expertise_name")
    private String proposedExpertiseName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRealName() {return realName;}

    public void setRealName(String realName) {this.realName = realName;}

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

    public String getResume() {return resume;}

    public void setResume(String resume) {this.resume = resume;}

    public String getProposedExpertiseName() {return proposedExpertiseName;}

    public void setProposedExpertiseName(String proposedExpertiseName) {this.proposedExpertiseName = proposedExpertiseName;}
}