package com.cpt202.app.service;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdminService {

    private final SpecialistProfileRepository profileRepository;
    private final SpecialistProfileEditRequestRepository editRequestRepository;
    private final UserRepository userRepository;
    private final ExpertiseCategoryRepository expertiseRepository;

    // Constructor Injection
    public AdminService(SpecialistProfileRepository profileRepository,
                        SpecialistProfileEditRequestRepository editRequestRepository,
                        UserRepository userRepository,
                        ExpertiseCategoryRepository expertiseRepository) {
        this.profileRepository = profileRepository;
        this.editRequestRepository = editRequestRepository;
        this.userRepository = userRepository;
        this.expertiseRepository = expertiseRepository;
    }


    //Approval of First-time Specialist Applications

    public void approveNewSpecialist(Long profileId) {
        SpecialistProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Application record not found"));

        // 1. Handle custom expertise storage logic (Fixed version: prevents duplicate name errors)
        // Modified original code to prevent "Duplicate entry" errors
        if (profile.getProposedExpertiseName() != null) {
            String expName = profile.getProposedExpertiseName();

            // Check the database to see if this expertise already exists
            ExpertiseCategory existingCategory = expertiseRepository.findAll().stream()
                    .filter(e -> e.getName().equalsIgnoreCase(expName))
                    .findFirst()
                    .orElse(null);

            if (existingCategory != null) {
                // If it exists, bind directly to the existing one; do not insert again
                profile.setExpertise(existingCategory);
            } else {
                // If it truly does not exist, create and save it
                ExpertiseCategory newCategory = new ExpertiseCategory();
                newCategory.setName(expName);
                newCategory.setDescription("User-defined new expertise, pending administrator confirmation");
                expertiseRepository.save(newCategory);
                profile.setExpertise(newCategory);
            }

            profile.setProposedExpertiseName(null); // Clear the temporary placeholder field
        }

        // 2. Update specialist profile status to ACTIVE
        profile.setStatus(SpecialistStatus.ACTIVE);

        // 3. Crucial: Elevate user permission role
        User user = profile.getUser();
        user.setRole(UserRole.SPECIALIST);

        userRepository.save(user);
        profileRepository.save(profile);
    }

    public void rejectNewSpecialist(Long profileId) {
        SpecialistProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Application record not found"));

        // Reject application, status changed to REJECTED, user role remains CUSTOMER
        profile.setStatus(SpecialistStatus.REJECTED);
        profileRepository.save(profile);
    }



    // Approval of Existing Specialist Profile Edits

    public void approveEditRequest(Long requestId) {
        SpecialistProfileEditRequest editReq = editRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Edit request not found"));

        SpecialistProfile profile = editReq.getSpecialistProfile();

        // 1. Overwrite the main table with new data from the shadow table
        if (editReq.getNewRealName() != null && !editReq.getNewRealName().isBlank()) {
            profile.setRealName(editReq.getNewRealName());
        }
        profile.setLevel(editReq.getNewLevel());
        profile.setHourlyFee(editReq.getNewHourlyFee());
        profile.setResume(editReq.getNewResume());

        // 2. Handle expertise change logic during editing (prevent Duplicate entry error)
        if (editReq.getNewProposedExpertiseName() != null) {
            String expName = editReq.getNewProposedExpertiseName();

            // Safety check: prevent duplicate insertion
            ExpertiseCategory existingCategory = expertiseRepository.findAll().stream()
                    .filter(e -> e.getName().equalsIgnoreCase(expName))
                    .findFirst()
                    .orElse(null);

            if (existingCategory != null) {
                profile.setExpertise(existingCategory); // Exists, bind directly
            } else {
                ExpertiseCategory newCategory = new ExpertiseCategory();
                newCategory.setName(expName);
                expertiseRepository.save(newCategory); // Truly does not exist, then save
                profile.setExpertise(newCategory);
            }
        } else if (editReq.getNewExpertise() != null) {
            // User selected another official expertise from the dropdown
            profile.setExpertise(editReq.getNewExpertise());
        }

        // 3. Status transition: Shadow table becomes APPROVED, main table remains ACTIVE
        editReq.setStatus(SpecialistProfileEditStatus.APPROVED);

        profileRepository.save(profile);
        editRequestRepository.save(editReq);
    }

    public void rejectEditRequest(Long requestId) {
        SpecialistProfileEditRequest editReq = editRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Edit request not found"));

        // Reject edit, only change the status of the request; main profile remains unaffected
        editReq.setStatus(SpecialistProfileEditStatus.REJECTED);
        editRequestRepository.save(editReq);
    }
}