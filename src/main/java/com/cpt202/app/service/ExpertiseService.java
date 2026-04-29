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

    /**
     * 获取所有专业列表
     */
    public List<ExpertiseCategory> getAllExpertise() {
        return expertiseRepository.findAll();
    }

    /**
     * 添加新专业（不允许重复）
     */
    public ExpertiseCategory addExpertise(ExpertiseCategory category) {
        if (category == null || category.getName() == null || category.getName().isBlank()) {
            throw new RuntimeException("专业名称不能为空");
        }

        String name = category.getName().trim();
        if (expertiseRepository.existsByNameIgnoreCase(name)) {
            throw new RuntimeException("专业【" + name + "】已存在");
        }

        ExpertiseCategory newCategory = new ExpertiseCategory();
        newCategory.setName(name);
        newCategory.setDescription(category.getDescription());

        return expertiseRepository.save(newCategory);
    }

    /**
     * 删除专业（仅当不被任何专家使用时）
     */
    public void deleteExpertise(Long id) {
        ExpertiseCategory category = expertiseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("专业不存在"));

        // 检查是否有专家在使用这个专业
        boolean inUse = profileRepository.findAll().stream()
                .anyMatch(profile -> profile.getExpertise() != null
                        && profile.getExpertise().getId().equals(id));

        if (inUse) {
            throw new RuntimeException("专业【" + category.getName() + "】正在被某些专家使用，无法删除");
        }

        expertiseRepository.deleteById(id);
    }

    public ExpertiseCategory updateExpertise(Long id, ExpertiseCategory updateData) {
        // 1. 查找是否存在
        ExpertiseCategory category = expertiseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("专业分类不存在"));

        // 2. 校验名称（如果要改名，不能改成数据库里已有的其他名字）
        if (updateData.getName() != null && !updateData.getName().isBlank()) {
            String newName = updateData.getName().trim();
            // 如果新名字跟旧名字不一样，且数据库里已经有这个新名字了，就报错
            if (!category.getName().equalsIgnoreCase(newName) &&
                    expertiseRepository.existsByNameIgnoreCase(newName)) {
                throw new RuntimeException("修改失败：专业【" + newName + "】已存在");
            }
            category.setName(newName);
        }

        // 3. 更新描述
        if (updateData.getDescription() != null) {
            category.setDescription(updateData.getDescription());
        }

        return expertiseRepository.save(category);
    }
}