package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.session.entity.SessionProfileLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionProfileLinkRepository extends JpaRepository<SessionProfileLink, Long> {

    List<SessionProfileLink> findAllBySessionProfileSessionProfileIdAndDeletedAtIsNull(
            Long sessionProfileId
    );
}