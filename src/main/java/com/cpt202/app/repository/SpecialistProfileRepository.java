package com.cpt202.app.repository;

import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.model.ExpertiseCategory;
import com.cpt202.app.model.SpecialistStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecialistProfileRepository extends JpaRepository<SpecialistProfile, Long> {
    // 核心功能：大厅里只能展示审核通过 (approved) 的专家！并且支持分页！
    Page<SpecialistProfile> findByStatus(SpecialistStatus status, Pageable pageable);
}