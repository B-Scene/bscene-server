package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.session.entity.SessionApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionApplicationRepository extends JpaRepository<SessionApplication, Long> {
}