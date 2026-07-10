package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.session.entity.SessionApplicationLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionApplicationLinkRepository extends JpaRepository<SessionApplicationLink, Long> {

    List<SessionApplicationLink> findAllBySessionApplicationSessionApplicationIdAndDeletedAtIsNull(
            Long sessionApplicationId
    );
}
