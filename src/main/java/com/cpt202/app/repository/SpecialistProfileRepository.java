package com.cpt202.app.repository;

import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.model.ExpertiseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecialistProfileRepository extends JpaRepository<SpecialistProfile, Long> {
    // 核心功能：按专业分类筛选专家
    List<SpecialistProfile> findByExpertise(ExpertiseCategory expertise);
}