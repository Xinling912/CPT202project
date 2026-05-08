package com.cpt202.app.repository;

import com.cpt202.app.model.ExpertiseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExpertiseCategoryRepository extends JpaRepository<ExpertiseCategory, Long> {

    // Find by name, ignoring case
    Optional<ExpertiseCategory> findByNameIgnoreCase(String name);

    // Check if name already exists, ignoring case
    boolean existsByNameIgnoreCase(String name);

    // Delete expertise by name
    void deleteByName(String name);
}