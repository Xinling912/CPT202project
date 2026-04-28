package com.cpt202.app.service;

import com.cpt202.app.model.*;
import com.cpt202.app.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional // 保证审批过程中的多步数据库操作要么全成功，要么全回滚
public class AdminService {

    private final SpecialistProfileRepository profileRepository;
    private final SpecialistProfileEditRequestRepository editRequestRepository;
    private final UserRepository userRepository;
    private final ExpertiseCategoryRepository expertiseRepository;

    // 构造器注入
    public AdminService(SpecialistProfileRepository profileRepository,
                        SpecialistProfileEditRequestRepository editRequestRepository,
                        UserRepository userRepository,
                        ExpertiseCategoryRepository expertiseRepository) {
        this.profileRepository = profileRepository;
        this.editRequestRepository = editRequestRepository;
        this.userRepository = userRepository;
        this.expertiseRepository = expertiseRepository;
    }

    // ==========================================
    // 模块一：新专家首次申请审批
    // ==========================================

    public void approveNewSpecialist(Long profileId) {
        SpecialistProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("找不到该申请记录"));

        // 1. 处理自定义专业入库逻辑
        if (profile.getProposedExpertiseName() != null) {
            ExpertiseCategory newCategory = new ExpertiseCategory();
            newCategory.setName(profile.getProposedExpertiseName());
            expertiseRepository.save(newCategory); // 正式存入专业字典表

            profile.setExpertise(newCategory); // 绑定给该专家
            profile.setProposedExpertiseName(null); // 清空临时占位字段
        }

        // 2. 更新专家名片状态为接单中
        profile.setStatus(SpecialistStatus.ACTIVE);

        // 3. 关键：提升用户权限角色
        User user = profile.getUser();
        user.setRole(UserRole.SPECIALIST);

        userRepository.save(user);
        profileRepository.save(profile);
    }

    public void rejectNewSpecialist(Long profileId) {
        SpecialistProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("找不到该申请记录"));

        // 拒绝申请，状态改为 REJECTED，用户角色保持 CUSTOMER 不变
        profile.setStatus(SpecialistStatus.REJECTED);
        profileRepository.save(profile);
    }


    // ==========================================
    // 模块二：老专家修改资料审批
    // ==========================================

    public void approveEditRequest(Long requestId) {
        SpecialistProfileEditRequest editReq = editRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("找不到该修改申请"));

        SpecialistProfile profile = editReq.getSpecialistProfile();

        // 1. 将影子表的新数据覆盖到主表
        if (editReq.getNewRealName() != null && !editReq.getNewRealName().isBlank()) {
            profile.setRealName(editReq.getNewRealName());
        }
        profile.setLevel(editReq.getNewLevel());
        profile.setHourlyFee(editReq.getNewHourlyFee());
        profile.setResume(editReq.getNewResume());

        // 2. 处理修改时的专业变更逻辑
        if (editReq.getNewProposedExpertiseName() != null) {
            // 他填了新的自定义专业
            ExpertiseCategory newCategory = new ExpertiseCategory();
            newCategory.setName(editReq.getNewProposedExpertiseName());
            expertiseRepository.save(newCategory); // 入库

            profile.setExpertise(newCategory);
        } else if (editReq.getNewExpertise() != null) {
            // 他选择了下拉框里的其他官方专业
            profile.setExpertise(editReq.getNewExpertise());
        }

        // 3. 状态流转：影子表改为 APPROVED，主表依然保持 ACTIVE
        editReq.setStatus(SpecialistProfileEditStatus.APPROVED);

        profileRepository.save(profile);
        editRequestRepository.save(editReq);
    }

    public void rejectEditRequest(Long requestId) {
        SpecialistProfileEditRequest editReq = editRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("找不到该修改申请"));

        // 拒绝修改，只改变修改单的状态，主表名片丝毫不受影响
        editReq.setStatus(SpecialistProfileEditStatus.REJECTED);
        editRequestRepository.save(editReq);
    }
}