package com.cpt202.app.repository;

import com.cpt202.app.model.User;
import com.cpt202.app.model.SpecialistProfile;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.cpt202.app.model.SpecialistStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface SpecialistProfileRepository extends JpaRepository<SpecialistProfile, Long>, JpaSpecificationExecutor<SpecialistProfile> {
    // Find specialist profiles by a specific specialist status
    Page<SpecialistProfile> findByStatus(SpecialistStatus status, Pageable pageable);
    List<SpecialistProfile> findByStatus(SpecialistStatus status);
    // Find the corresponding specialist profile by userId from the User table
    Optional<SpecialistProfile> findByUserId(Long userId);
    // Find profile by associated User object
    Optional<SpecialistProfile> findByUser(User user);
    // Find corresponding SpecialistProfile by username obtained from Token
    Optional<SpecialistProfile> findByUserUsername(String username);
}