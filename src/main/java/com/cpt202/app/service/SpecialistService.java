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
    private BookingRepository bookingRepository;

    public record SpecialistApplyRequest(
            String realName, // 真实姓名
            SpecialistLevel level,
            BigDecimal hourlyFee,
            String resume,
            Long expertiseId,       // 选了已有专业则传 ID
            String newExpertiseName // 选了“其他”则传自定义名称
    ) {
    }

    public void submitProfileApplication(String username, SpecialistApplyRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Optional<SpecialistProfile> existingProfile = profileRepository.findByUser(user);

        // 逻辑分流：
        if (existingProfile.isEmpty() || existingProfile.get().getStatus() != SpecialistStatus.ACTIVE) {
            // 【场景 A】新专家申请（或未过审）：直接操作主表 SpecialistProfile
            SpecialistProfile profile = existingProfile.orElse(new SpecialistProfile());
            fillProfileData(profile, user, request);
            profile.setStatus(SpecialistStatus.PENDING); // 设为待审核
            profileRepository.save(profile);
        } else {
            // 【场景 B】已入驻专家修改资料：操作影子表 EditRequest
            SpecialistProfileEditRequest editReq = new SpecialistProfileEditRequest();
            editReq.setSpecialistProfile(existingProfile.get());
            editReq.setNewHourlyFee(request.hourlyFee());
            editReq.setNewResume(request.resume());
            editReq.setNewRealName(request.realName());
            editReq.setNewLevel(request.level());

            // 处理专家专业逻辑
            if (request.expertiseId() != null) {
                editReq.setNewExpertise(expertiseRepository.findById(request.expertiseId()).orElse(null));
                editReq.setNewProposedExpertiseName(null);
            } else {
                editReq.setNewProposedExpertiseName(request.newExpertiseName());
            }

            editReq.setStatus(SpecialistProfileEditStatus.PENDING); // 修改单状态为待审
            editRequestRepository.save(editReq);
        }
    }

    private void fillProfileData(SpecialistProfile profile, User user, SpecialistApplyRequest request) {
        profile.setUser(user);
        profile.setRealName(request.realName);
        profile.setLevel(request.level());
        profile.setHourlyFee(request.hourlyFee());
        profile.setResume(request.resume());

        // 新申请者的专业逻辑
        if (request.expertiseId() != null) {
            // 用户选择了现有专业
            ExpertiseCategory category = expertiseRepository.findById(request.expertiseId())
                    .orElseThrow(() -> new RuntimeException("所选专业不存在"));
            profile.setExpertise(category);
            profile.setProposedExpertiseName(null); // 清空临时字段
        } else if (request.newExpertiseName() != null && !request.newExpertiseName().trim().isEmpty()) {
            // 用户选了“其他”并输入了自定义专业
            String newName = request.newExpertiseName().trim();

            // 后端防呆：检查这个名字是不是早就存在了（忽略大小写）
            Optional<ExpertiseCategory> existingCat = expertiseRepository.findByNameIgnoreCase(newName);
            if (existingCat.isPresent()) {
                profile.setExpertise(existingCat.get());
                profile.setProposedExpertiseName(null);
            } else {
                profile.setExpertise(null); // 官方专业暂空
                // 存入主表的临时字段，等待管理员审批新申请时一并处理
                profile.setProposedExpertiseName(newName);
            }
        } else {
            throw new RuntimeException("必须选择一个专业或填写自定义专业");


        }
    }
    // ========== 新增：获取专家累计总收入 ==========
    /**
     * 获取专家累计总收入（所有已完成订单的金额总和）
     * @param username 专家账号的用户名
     * @return 累计总收入，保留两位小数
     */
    public BigDecimal getTotalEarnings(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 建议添加：角色校验
        if (user.getRole() != UserRole.SPECIALIST) {
            throw new RuntimeException("只有专家可以查看收入");
        }

        SpecialistProfile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("专家档案不存在"));

        BigDecimal earnings = bookingRepository.sumTotalAmountBySpecialistIdAndStatus(
                profile.getId(),
                BookingStatus.COMPLETED
        );

        return earnings.setScale(2, RoundingMode.HALF_UP);
    }
}
