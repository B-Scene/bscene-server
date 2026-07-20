package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.session.entity.SessionRecruitmentSearchKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionRecruitmentSearchKeywordRepository
        extends JpaRepository<SessionRecruitmentSearchKeyword, Long> {

    List<SessionRecruitmentSearchKeyword> findAllByUserIdOrderBySearchedAtDesc(Long userId);

    Optional<SessionRecruitmentSearchKeyword> findByUserIdAndKeyword(Long userId, String keyword);

    void deleteBySessionRecruitmentSearchKeywordIdAndUserId(Long keywordId, Long userId);
}
