package com.cpt202.app.repository;

import com.cpt202.app.model.ExpertiseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExpertiseCategoryRepository extends JpaRepository<ExpertiseCategory, Long> {

    // 根据专业名称查找，且忽略大小写
    Optional<ExpertiseCategory> findByNameIgnoreCase(String name);

    // 检查专业名称是否已存在（忽略大小写）
    boolean existsByNameIgnoreCase(String name);

    // 根据名称删除专业
    void deleteByName(String name);
}