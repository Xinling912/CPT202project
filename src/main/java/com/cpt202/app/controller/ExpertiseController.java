package com.cpt202.app.controller;

import com.cpt202.app.model.ExpertiseCategory;
import com.cpt202.app.repository.ExpertiseCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/expertises")
public class ExpertiseController {

    @Autowired
    private ExpertiseCategoryRepository expertiseRepository;

    // 获取所有可用专业
    @GetMapping
    public List<ExpertiseCategory> getAllCategories() {
        return expertiseRepository.findAll();
    }
}