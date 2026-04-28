package com.cpt202.app.repository;

import com.cpt202.app.model.SpecialistProfile;
import com.cpt202.app.model.SpecialistProfileEditStatus;
import com.cpt202.app.model.SpecialistProfileEditRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface SpecialistProfileEditRequestRepository extends JpaRepository<SpecialistProfileEditRequest, Long> {
    // 查找某种特定专家文档修改状态（如 PENDING）的修改申请
    List<SpecialistProfileEditRequest> findByStatus(SpecialistProfileEditStatus status);
    // 检查某个专家是否已有 PENDING 状态的申请
    boolean existsBySpecialistProfileAndStatus(SpecialistProfile specialistProfile, SpecialistProfileEditStatus status);
    // 按专家查询查最新的一条修改申请
    Optional<SpecialistProfileEditRequest> findTopBySpecialistProfileOrderBySubmitTimeDesc(SpecialistProfile profile);
}
