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

    /**
     * 获取所有专业列表（所有人可访问）
     */
    @GetMapping("/list")
    public ResponseEntity<?> listAllExpertise() {
        List<ExpertiseCategory> categories = expertiseService.getAllExpertise();
        return ResponseEntity.ok(Map.of("data", categories));
    }

    /**
     * 添加专业（仅限管理员）
     */
    @PostMapping("/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addExpertise(@RequestBody ExpertiseCategory category) {
        try {
            ExpertiseCategory saved = expertiseService.addExpertise(category);
            return ResponseEntity.ok(Map.of(
                    "message", "专业【" + saved.getName() + "】添加成功",
                    "data", saved
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 删除专业（仅限管理员）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteExpertise(@PathVariable Long id) {
        try {
            expertiseService.deleteExpertise(id);
            return ResponseEntity.ok(Map.of("message", "专业已删除"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 修改专业（仅限管理员）
     * 对应前端的 PUT http://localhost:8080/api/expertise/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateExpertise(@PathVariable Long id, @RequestBody ExpertiseCategory category) {
        try {
            ExpertiseCategory updated = expertiseService.updateExpertise(id, category);
            return ResponseEntity.ok(Map.of(
                    "message", "专业信息修改成功",
                    "data", updated
            ));
        } catch (Exception e) {
            // 这里的报错会被我们前端的 showToast 完美捕捉！
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}