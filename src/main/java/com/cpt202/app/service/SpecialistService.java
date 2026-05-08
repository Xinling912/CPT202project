package com.cpt202.app.service;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@Transactional
public class SpecialistService {

    @Autowired
    private SpecialistProfileRepository profileRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ExpertiseCategoryRepository expertiseRepository;
    @Autowired
    private SpecialistProfileEditRequestRepository editRequestRepository;
    @Autowired
    private SpecialistProfileRepository specialistProfileRepository;
    @Autowired
    private BookingRepository bookingRepository;

    public record SpecialistApplyRequest(
            String realName, // Real Name
            SpecialistLevel level,
            BigDecimal hourlyFee,
            String resume,
            Long expertiseId,       // Pass ID if an existing expertise is selected
            String newExpertiseName // Pass custom name if "Other" is selected
    ) {
    }

    public void submitProfileApplication(String username, SpecialistApplyRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User does not exist"));

        Optional<SpecialistProfile> existingProfile = profileRepository.findByUser(user);

        // Logic branching:
        if (existingProfile.isEmpty() || existingProfile.get().getStatus() != SpecialistStatus.ACTIVE) {
            // [Scenario A] New specialist application (or re-submission after failed review): Direct operation on the main table SpecialistProfile

            // If a profile already exists, check if re-application is allowed
            if (existingProfile.isPresent()) {
                SpecialistProfile profile = existingProfile.get();
                SpecialistStatus currentStatus = profile.getStatus();

                // Existing application pending review
                if (currentStatus == SpecialistStatus.PENDING) {
                    throw new RuntimeException("You already have a specialist application pending review, please wait for admin processing");
                }

                // Account is banned (cannot re-apply)
                if (currentStatus == SpecialistStatus.INACTIVE) {
                    throw new RuntimeException("Your account has been banned and you cannot re-apply to become a specialist");
                }

                // Rejected (REJECTED) -> Allow re-application, update existing record (do not create new)
                if (currentStatus == SpecialistStatus.REJECTED) {
                    fillProfileData(profile, user, request);
                    profile.setStatus(SpecialistStatus.PENDING);
                    profileRepository.save(profile);
                    return;
                }
            }

            // Case where creation is allowed (no profile exists)
            SpecialistProfile profile = existingProfile.orElse(new SpecialistProfile());
            fillProfileData(profile, user, request);
            profile.setStatus(SpecialistStatus.PENDING); // Set to pending review
            profileRepository.save(profile);

        } else {
            // [Scenario B] Existing specialist modifying profile: Operation on the shadow table EditRequest

            // Check if there is already a pending modification request
            SpecialistProfile activeProfile = existingProfile.get();
            boolean hasPendingRequest = editRequestRepository.existsBySpecialistProfileAndStatus(
                    activeProfile,
                    SpecialistProfileEditStatus.PENDING
            );

            if (hasPendingRequest) {
                throw new RuntimeException("You already have a modification request pending review, please wait for admin processing before submitting a new one");
            }
            // Create modification request
            SpecialistProfileEditRequest editReq = new SpecialistProfileEditRequest();
            editReq.setSpecialistProfile(existingProfile.get());
            editReq.setNewHourlyFee(request.hourlyFee());
            editReq.setNewResume(request.resume());
            editReq.setNewRealName(request.realName());
            editReq.setNewLevel(request.level());

            // Handle specialist expertise logic
            if (request.expertiseId() != null) {
                editReq.setNewExpertise(expertiseRepository.findById(request.expertiseId()).orElse(null));
                editReq.setNewProposedExpertiseName(null);
            } else {
                editReq.setNewProposedExpertiseName(request.newExpertiseName());
            }

            editReq.setStatus(SpecialistProfileEditStatus.PENDING); // Status of modification order set to pending
            editRequestRepository.save(editReq);
        }
    }

    private void fillProfileData(SpecialistProfile profile, User user, SpecialistApplyRequest request) {
        profile.setUser(user);
        profile.setRealName(request.realName);
        profile.setLevel(request.level());
        profile.setHourlyFee(request.hourlyFee());
        profile.setResume(request.resume());

        // Expertise logic for new applicants
        if (request.expertiseId() != null) {
            // User selected an existing expertise
            ExpertiseCategory category = expertiseRepository.findById(request.expertiseId())
                    .orElseThrow(() -> new RuntimeException("The selected expertise does not exist"));
            profile.setExpertise(category);
            profile.setProposedExpertiseName(null); // Clear temporary field
        } else if (request.newExpertiseName() != null && !request.newExpertiseName().trim().isEmpty()) {
            // User selected "Other" and entered a custom expertise
            String newName = request.newExpertiseName().trim();

            // Backend safety check: check if this name already exists (ignore case)
            Optional<ExpertiseCategory> existingCat = expertiseRepository.findByNameIgnoreCase(newName);
            if (existingCat.isPresent()) {
                profile.setExpertise(existingCat.get());
                profile.setProposedExpertiseName(null);
            } else {
                profile.setExpertise(null); // Official expertise left blank temporarily
                // Store in the main table's temporary field, to be processed when the admin approves the new application
                profile.setProposedExpertiseName(newName);
            }
        } else {
            throw new RuntimeException("You must select an expertise or fill in a custom expertise");


        }
    }
    // ========== Get Specialist Cumulative Total Earnings ==========
    /**
     * Get specialist cumulative total earnings (sum of all completed booking amounts)
     * @param username The username of the specialist account
     * @return Cumulative total earnings, rounded to two decimal places
     */
    public BigDecimal getTotalEarnings(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User does not exist"));

        // Recommendation: add role validation
        if (user.getRole() != UserRole.SPECIALIST) {
            throw new RuntimeException("Only specialists can view earnings");
        }

        SpecialistProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Specialist profile does not exist"));

        BigDecimal earnings = bookingRepository.sumTotalAmountBySpecialistIdAndStatus(
                profile.getId(),
                BookingStatus.COMPLETED
        );

        return earnings.setScale(2, RoundingMode.HALF_UP);
    }

    public ApplicationStatus getCurrentApplicationStatus(User user) {
        Optional<SpecialistProfile> profileOpt = specialistProfileRepository.findByUser(user);

        // ========== 1. General User (or application stage) ==========
        if (profileOpt.isPresent()) {
            SpecialistProfile profile = profileOpt.get();
            SpecialistStatus status = profile.getStatus();

            if (status == SpecialistStatus.PENDING) {
                return ApplicationStatus.APPLY_PENDING;
            }
            if (status == SpecialistStatus.REJECTED) {
                return ApplicationStatus.APPLY_REJECTED;
            }
            if (status == SpecialistStatus.ACTIVE) {
                // Already a formal specialist, continue checking for modification requests
                return getActiveSpecialistEditStatus(profile);
            }
        }

        // ========== 2. Never applied ==========
        return ApplicationStatus.NONE;
    }


    private ApplicationStatus getActiveSpecialistEditStatus(SpecialistProfile profile) {
        // Only query the latest modification request
        Optional<SpecialistProfileEditRequest> latestOpt =
                editRequestRepository.findTopBySpecialistProfileOrderBySubmitTimeDesc(profile);

        if (latestOpt.isEmpty()) {
            return ApplicationStatus.IS_ACTIVE_SPECIALIST;
        }

        SpecialistProfileEditRequest latest = latestOpt.get();
        SpecialistProfileEditStatus editStatus = latest.getStatus();

        switch (editStatus) {
            case PENDING:
                return ApplicationStatus.EDIT_PENDING;
            case APPROVED:
                return ApplicationStatus.EDIT_APPROVED;
            case REJECTED:
                return ApplicationStatus.EDIT_REJECTED;
            default:
                return ApplicationStatus.IS_ACTIVE_SPECIALIST;
        }
    }
}