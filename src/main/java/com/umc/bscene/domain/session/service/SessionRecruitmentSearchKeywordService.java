package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.session.dto.recruitment.response.RecruitmentSearchKeywordResponse;
import com.umc.bscene.domain.session.entity.SessionRecruitmentSearchKeyword;
import com.umc.bscene.domain.session.repository.SessionRecruitmentSearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionRecruitmentSearchKeywordService {

    private static final int MAX_KEYWORD_LENGTH = 100;

    private final SessionRecruitmentSearchKeywordRepository repository;

    @Transactional
    public void record(Long userId, String keyword) {
        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword == null) return;

        repository.findByUserIdAndKeyword(userId, normalizedKeyword)
                .ifPresentOrElse(
                        SessionRecruitmentSearchKeyword::refresh,
                        () -> repository.save(SessionRecruitmentSearchKeyword.builder()
                                .userId(userId)
                                .keyword(normalizedKeyword)
                                .searchedAt(LocalDateTime.now())
                                .build())
                );
    }

    @Transactional(readOnly = true)
    public List<RecruitmentSearchKeywordResponse> getAll(Long userId) {
        return repository.findAllByUserIdOrderBySearchedAtDesc(userId).stream()
                .map(RecruitmentSearchKeywordResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long userId, Long keywordId) {
        repository.deleteBySessionRecruitmentSearchKeywordIdAndUserId(keywordId, userId);
    }

    private String normalize(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        String normalized = keyword.trim();
        return normalized.length() <= MAX_KEYWORD_LENGTH
                ? normalized
                : normalized.substring(0, MAX_KEYWORD_LENGTH);
    }
}
