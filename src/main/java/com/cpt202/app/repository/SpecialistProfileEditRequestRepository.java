package com.cpt202.app.repository;

import com.cpt202.app.model.SpecialistProfileEditStatus;
import com.cpt202.app.model.SpecialistProfileEditRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface SpecialistProfileEditRequestRepository extends JpaRepository<SpecialistProfileEditRequest, Long> {
    // 查找某种特定专家文档修改状态（如 PENDING）的修改申请
    List<SpecialistProfileEditRequest> findByStatus(SpecialistProfileEditStatus status);
}
