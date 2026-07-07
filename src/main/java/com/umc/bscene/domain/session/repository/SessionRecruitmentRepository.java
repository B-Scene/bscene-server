package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.session.entity.SessionRecruitment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionRecruitmentRepository extends JpaRepository<SessionRecruitment, Long> {

    Optional<SessionRecruitment> findBySessionRecruitmentIdAndDeletedAtIsNull(Long sessionRecruitmentId);

}