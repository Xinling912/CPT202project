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

    // 与专业分类的多对一关系：多个专家可以属于同一个专业
    @ManyToOne
    @JoinColumn(name = "expertise_id", referencedColumnName = "id", nullable = false)
    private ExpertiseCategory expertise;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private SpecialistLevel level; // JUNIOR, SENIOR, EXPERT

    // 注：在 Java 里算钱，绝对不能用 Double，必须用 BigDecimal 防止精度丢失！
    @Column(name = "hourly_fee", precision = 10, scale = 2, nullable = false)
    private BigDecimal hourlyFee;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SpecialistStatus status; // ACTIVE (接单中), INACTIVE (休息中)

    // TODO: 请使用 IDE 生成 Getter 和 Setter (或添加 Lombok 的 @Data 注解)
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
}