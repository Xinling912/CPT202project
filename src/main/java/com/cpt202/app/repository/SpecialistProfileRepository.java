package com.cpt202.app.repository;

import com.cpt202.app.model.SpecialistProfile;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import com.cpt202.app.model.SpecialistStatus;
import com.cpt202.app.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialistProfileRepository extends JpaRepository<SpecialistProfile, Long> {
    //// 1.通过 User 实体找到对应的专家名片
    Optional<SpecialistProfile> findByUser(User user);

}