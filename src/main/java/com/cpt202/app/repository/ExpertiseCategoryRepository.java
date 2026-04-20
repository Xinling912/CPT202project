package com.cpt202.app.repository;

import com.cpt202.app.model.ExpertiseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExpertiseCategoryRepository extends JpaRepository<ExpertiseCategory, Long> {

    // 根据专业名称查找，且忽略大小写
    Optional<ExpertiseCategory> findByNameIgnoreCase(String name);

}