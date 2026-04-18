package com.cpt202.app.repository;

import com.cpt202.app.model.User;
import com.cpt202.app.model.SpecialistProfile;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.cpt202.app.model.SpecialistStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface SpecialistProfileRepository extends JpaRepository<SpecialistProfile, Long>, JpaSpecificationExecutor<SpecialistProfile> {
    // 查找处于某种专家状态的专家名片
    Page<SpecialistProfile> findByStatus(SpecialistStatus status, Pageable pageable);
    List<SpecialistProfile> findByStatus(SpecialistStatus status);
    // 根据 User 表的userid查对应专家档案的方法
    Optional<SpecialistProfile> findByUserId(Long userId);
     //通过关联的 User 对象查找名片
    Optional<SpecialistProfile> findByUser(User user);
    // 通过Token中获取的username查找对应SpecialistProfile
    Optional<SpecialistProfile> findByUserUsername(String username);
}