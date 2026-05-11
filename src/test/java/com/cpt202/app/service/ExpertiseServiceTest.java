package com.cpt202.app.service;

import com.cpt202.app.model.ExpertiseCategory;
import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.repository.ExpertiseCategoryRepository;
import com.cpt202.app.repository.SpecialistProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpertiseServiceTest {

    @Mock
    private ExpertiseCategoryRepository expertiseRepository;

    @Mock
    private SpecialistProfileRepository profileRepository;

    @InjectMocks
    private ExpertiseService expertiseService;

    private ExpertiseCategory psychologyCategory;

    @BeforeEach
    void setUp() {
        psychologyCategory = new ExpertiseCategory();
        psychologyCategory.setId(1L);
        psychologyCategory.setName("Psychology");
        psychologyCategory.setDescription("Study of mind and behavior");
    }

    // ==========================================
    // 1. Get All Expertise Tests
    // ==========================================
    @Test
    void getAllExpertise_ShouldReturnList() {
        // Arrange
        when(expertiseRepository.findAll()).thenReturn(List.of(psychologyCategory));

        // Act
        List<ExpertiseCategory> result = expertiseService.getAllExpertise();

        // Assert
        assertEquals(1, result.size());
        assertEquals("Psychology", result.get(0).getName());
        verify(expertiseRepository, times(1)).findAll();
    }

    // ==========================================
    // 2. Add Expertise Tests
    // ==========================================
    @Test
    void addExpertise_HappyPath_ShouldSaveAndReturn() {
        // Arrange
        ExpertiseCategory newCategory = new ExpertiseCategory();
        newCategory.setName("  Career Planning  "); // Test trim functionality
        newCategory.setDescription("Help with jobs");

        when(expertiseRepository.existsByNameIgnoreCase("Career Planning")).thenReturn(false);
        when(expertiseRepository.save(any(ExpertiseCategory.class))).thenAnswer(invocation -> {
            ExpertiseCategory saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        // Act
        ExpertiseCategory result = expertiseService.addExpertise(newCategory);

        // Assert
        assertEquals(2L, result.getId());
        assertEquals("Career Planning", result.getName()); // Verify it was trimmed
        verify(expertiseRepository, times(1)).save(any(ExpertiseCategory.class));
    }

    @Test
    void addExpertise_BlankName_ShouldThrowException() {
        // Arrange
        ExpertiseCategory invalidCategory = new ExpertiseCategory();
        invalidCategory.setName("   "); // Blank name

        // Act & Assert
        Exception e = assertThrows(RuntimeException.class, () -> expertiseService.addExpertise(invalidCategory));
        assertEquals("Expertise name cannot be empty", e.getMessage());
        verify(expertiseRepository, never()).save(any());
    }

    @Test
    void addExpertise_DuplicateName_ShouldThrowException() {
        // Arrange
        ExpertiseCategory duplicateCategory = new ExpertiseCategory();
        duplicateCategory.setName("Psychology");

        when(expertiseRepository.existsByNameIgnoreCase("Psychology")).thenReturn(true);

        // Act & Assert
        Exception e = assertThrows(RuntimeException.class, () -> expertiseService.addExpertise(duplicateCategory));
        assertTrue(e.getMessage().contains("already exists"));
        verify(expertiseRepository, never()).save(any());
    }

    // ==========================================
    // 3. Delete Expertise Tests
    // ==========================================
    @Test
    void deleteExpertise_HappyPath_ShouldDelete() {
        // Arrange
        when(expertiseRepository.findById(1L)).thenReturn(Optional.of(psychologyCategory));
        // Mock that NO profiles are currently using this expertise
        when(profileRepository.findAll()).thenReturn(List.of());

        // Act
        expertiseService.deleteExpertise(1L);

        // Assert
        verify(expertiseRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteExpertise_InUseBySpecialist_ShouldThrowException() {
        // Arrange
        when(expertiseRepository.findById(1L)).thenReturn(Optional.of(psychologyCategory));

        // Create a mock specialist profile that uses "Psychology" (ID = 1L)
        SpecialistProfile mockProfile = new SpecialistProfile();
        mockProfile.setId(100L);
        mockProfile.setExpertise(psychologyCategory);

        // Mock that the repository returns this profile
        when(profileRepository.findAll()).thenReturn(List.of(mockProfile));

        // Act & Assert
        Exception e = assertThrows(RuntimeException.class, () -> expertiseService.deleteExpertise(1L));
        assertTrue(e.getMessage().contains("is currently being used by specialists and cannot be deleted"));
        verify(expertiseRepository, never()).deleteById(anyLong());
    }

    // ==========================================
    // 4. Update Expertise Tests
    // ==========================================
    @Test
    void updateExpertise_HappyPath_ShouldUpdateNameAndDescription() {
        // Arrange
        ExpertiseCategory updateData = new ExpertiseCategory();
        updateData.setName("Clinical Psychology");
        updateData.setDescription("Updated description");

        when(expertiseRepository.findById(1L)).thenReturn(Optional.of(psychologyCategory));
        when(expertiseRepository.existsByNameIgnoreCase("Clinical Psychology")).thenReturn(false);
        when(expertiseRepository.save(any(ExpertiseCategory.class))).thenReturn(psychologyCategory);

        // Act
        ExpertiseCategory result = expertiseService.updateExpertise(1L, updateData);

        // Assert
        assertEquals("Clinical Psychology", result.getName());
        assertEquals("Updated description", result.getDescription());
        verify(expertiseRepository, times(1)).save(psychologyCategory);
    }

    @Test
    void updateExpertise_DuplicateNewName_ShouldThrowException() {
        // Arrange
        ExpertiseCategory updateData = new ExpertiseCategory();
        updateData.setName("Career Planning"); // Trying to rename "Psychology" to "Career Planning"

        when(expertiseRepository.findById(1L)).thenReturn(Optional.of(psychologyCategory));
        // Simulate that "Career Planning" already exists in the database
        when(expertiseRepository.existsByNameIgnoreCase("Career Planning")).thenReturn(true);

        // Act & Assert
        Exception e = assertThrows(RuntimeException.class, () -> expertiseService.updateExpertise(1L, updateData));
        assertTrue(e.getMessage().contains("already exists"));
        verify(expertiseRepository, never()).save(any());
    }
}