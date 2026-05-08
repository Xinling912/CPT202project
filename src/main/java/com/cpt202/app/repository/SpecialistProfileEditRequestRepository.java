package com.cpt202.app.repository;

import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.model.SpecialistProfileEditStatus;
import com.cpt202.app.model.SpecialistProfileEditRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface SpecialistProfileEditRequestRepository extends JpaRepository<SpecialistProfileEditRequest, Long> {
    // Find modification requests by a specific edit status (e.g., PENDING)
    List<SpecialistProfileEditRequest> findByStatus(SpecialistProfileEditStatus status);
    // Check if a specialist already has a request with PENDING status
    boolean existsBySpecialistProfileAndStatus(SpecialistProfile specialistProfile, SpecialistProfileEditStatus status);
    // Find the latest edit request for a specialist
    Optional<SpecialistProfileEditRequest> findTopBySpecialistProfileOrderBySubmitTimeDesc(SpecialistProfile profile);
}