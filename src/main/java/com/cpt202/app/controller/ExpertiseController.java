package com.cpt202.app.controller;

import com.cpt202.app.model.ExpertiseCategory;
import com.cpt202.app.service.ExpertiseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expertise")
@CrossOrigin
public class ExpertiseController {

    private final ExpertiseService expertiseService;

    public ExpertiseController(ExpertiseService expertiseService) {
        this.expertiseService = expertiseService;
    }

    /**Get all expertise list (accessible to everyone)*/
    @GetMapping("/list")
    public ResponseEntity<?> listAllExpertise() {
        List<ExpertiseCategory> categories = expertiseService.getAllExpertise();
        return ResponseEntity.ok(Map.of("data", categories));
    }

    /**Add expertise (Admin only)*/
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addExpertise(@RequestBody ExpertiseCategory category) {
        try {
            ExpertiseCategory saved = expertiseService.addExpertise(category);
            return ResponseEntity.ok(Map.of(
                    "message", "Expertise [" + saved.getName() + "] added successfully",
                    "data", saved
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**Delete expertise (Admin only)*/
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteExpertise(@PathVariable Long id) {
        try {
            expertiseService.deleteExpertise(id);
            return ResponseEntity.ok(Map.of("message", "Expertise deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**Update expertise (Admin only)*/
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateExpertise(@PathVariable Long id, @RequestBody ExpertiseCategory category) {
        try {
            ExpertiseCategory updated = expertiseService.updateExpertise(id, category);
            return ResponseEntity.ok(Map.of(
                    "message", "Expertise information updated successfully",
                    "data", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}