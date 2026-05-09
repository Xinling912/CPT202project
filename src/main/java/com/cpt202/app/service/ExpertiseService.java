package com.cpt202.app.service;

import com.cpt202.app.model.ExpertiseCategory;
import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.repository.ExpertiseCategoryRepository;
import com.cpt202.app.repository.SpecialistProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ExpertiseService {

    private final ExpertiseCategoryRepository expertiseRepository;
    private final SpecialistProfileRepository profileRepository;

    public ExpertiseService(ExpertiseCategoryRepository expertiseRepository,
                            SpecialistProfileRepository profileRepository) {
        this.expertiseRepository = expertiseRepository;
        this.profileRepository = profileRepository;
    }

    /**Get all expertise categories*/
    public List<ExpertiseCategory> getAllExpertise() {
        return expertiseRepository.findAll();
    }

    /**Add a new expertise (duplicates not allowed) */
    public ExpertiseCategory addExpertise(ExpertiseCategory category) {
        if (category == null || category.getName() == null || category.getName().isBlank()) {
            throw new RuntimeException("Expertise name cannot be empty");
        }

        String name = category.getName().trim();
        if (expertiseRepository.existsByNameIgnoreCase(name)) {
            throw new RuntimeException("Expertise [" + name + "] already exists");
        }

        ExpertiseCategory newCategory = new ExpertiseCategory();
        newCategory.setName(name);
        newCategory.setDescription(category.getDescription());

        return expertiseRepository.save(newCategory);
    }

    /**Delete expertise (only when not in use by any specialist)*/
    public void deleteExpertise(Long id) {
        ExpertiseCategory category = expertiseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expertise does not exist"));

        // Check if any specialist is using this expertise
        boolean inUse = profileRepository.findAll().stream()
                .anyMatch(profile -> profile.getExpertise() != null
                        && profile.getExpertise().getId().equals(id));

        if (inUse) {
            throw new RuntimeException("Expertise [" + category.getName() + "] is currently being used by specialists and cannot be deleted");
        }

        expertiseRepository.deleteById(id);
    }

    public ExpertiseCategory updateExpertise(Long id, ExpertiseCategory updateData) {
        // 1. Check if it exists
        ExpertiseCategory category = expertiseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expertise category does not exist"));

        // 2. Validate name (if renaming, cannot change to a name already existing in the database)
        if (updateData.getName() != null && !updateData.getName().isBlank()) {
            String newName = updateData.getName().trim();
            // If the new name is different from the old one and the database already contains this new name, throw exception
            if (!category.getName().equalsIgnoreCase(newName) &&
                    expertiseRepository.existsByNameIgnoreCase(newName)) {
                throw new RuntimeException("Update failed: Expertise [" + newName + "] already exists");
            }
            category.setName(newName);
        }

        // 3. Update description
        if (updateData.getDescription() != null) {
            category.setDescription(updateData.getDescription());
        }

        return expertiseRepository.save(category);
    }
}