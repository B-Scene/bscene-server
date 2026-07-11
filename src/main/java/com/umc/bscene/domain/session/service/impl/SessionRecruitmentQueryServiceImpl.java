package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentDetailResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentListItemResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentListResponse;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SessionRegion;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.domain.session.enums.code.SessionErrorCode;
import com.umc.bscene.domain.session.exception.SessionException;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import com.umc.bscene.domain.session.repository.SessionRecruitmentInterestRepository;
import com.umc.bscene.domain.session.service.SessionRecruitmentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionRecruitmentQueryServiceImpl implements SessionRecruitmentQueryService {

    private final SessionRecruitmentRepository sessionRecruitmentRepository;
    private final SessionRecruitmentInterestRepository interestRepository;

    @Override
    public SessionRecruitmentListResponse getSessionRecruitments(
            Long userId,
            Part part,
            SkillLevel skillLevel,
            SessionGenre genre,
            SessionRegion region,
            String keyword,
            Long cursorId,
            Integer size
    ) {
        int pageSize = size == null ? 10 : size;
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);
        LocalDateTime now = LocalDateTime.now();

        String normalizedKeyword = keyword == null || keyword.isBlank()
                ? null
                : keyword.trim();

        List<SessionRecruitment> recruitments = sessionRecruitmentRepository.findRecruitments(
                now,
                part,
                skillLevel,
                genre,
                region,
                normalizedKeyword,
                cursorId,
                pageRequest
        );

        boolean hasNext = recruitments.size() > pageSize;

        List<SessionRecruitment> slicedRecruitments = hasNext
                ? recruitments.subList(0, pageSize)
                : recruitments;

        List<Long> recruitmentIds = slicedRecruitments.stream()
                .map(SessionRecruitment::getSessionRecruitmentId)
                .toList();
        Set<Long> interestedIds = recruitmentIds.isEmpty()
                ? Set.of()
                : interestRepository.findInterestedRecruitmentIds(userId, recruitmentIds);

        List<SessionRecruitmentListItemResponse> content = slicedRecruitments.stream()
                .map(recruitment -> toListItemResponse(recruitment, now, interestedIds))
                .toList();

        Long nextCursor = hasNext && !slicedRecruitments.isEmpty()
                ? slicedRecruitments.get(slicedRecruitments.size() - 1).getSessionRecruitmentId()
                : null;

        return SessionRecruitmentListResponse.builder()
                .content(content)
                .size(pageSize)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    // 세션 모집 공고 상세 조회
    @Override
    public SessionRecruitmentDetailResponse getSessionRecruitmentDetail(
            Long userId,
            Long recruitmentId
    ) {

        SessionRecruitment recruitment = sessionRecruitmentRepository
                .findBySessionRecruitmentIdAndDeletedAtIsNull(recruitmentId)
                .orElseThrow(() -> new SessionException(SessionErrorCode.SESSION_RECRUITMENT_NOT_FOUND));

        return SessionRecruitmentDetailResponse.builder()
                .sessionRecruitmentId(recruitment.getSessionRecruitmentId())
                .recruitmentTitle(recruitment.getRecruitmentTitle())
                .deadlineAt(recruitment.getDeadlineAt())
                .dDay(calculateDDay(recruitment.getDeadlineAt().toLocalDate()))

                // createdAt 기준 3일 이내면 true
                .isNew(isNewRecruitment(recruitment.getCreatedAt(), LocalDateTime.now()))
                .isInterested(interestRepository
                        .existsBySessionRecruitment_SessionRecruitmentIdAndUser_Id(
                                recruitmentId,
                                userId
                        ))

                // 밴드 프로필 정보
                .bandId(recruitment.getBand().getId())
                .bandName(recruitment.getBand().getName())
                .bandProfileImageUrl(recruitment.getBand().getProfileImageUrl())
                .bandGenre(recruitment.getBand().getGenre().getName())
                .bandRegion(recruitment.getBand().getRegion().getName())


                // 모집 상세 정보
                .content(recruitment.getContent())
                .part(recruitment.getPart().getDescription())
                .genre(recruitment.getGenre().getDescription())
                .region(recruitment.getRegion().getDescription())
                .practiceSchedule(recruitment.getPracticeSchedule())
                .practicePlace(recruitment.getPracticePlace())
                .qualification(recruitment.getQualification())
                .build();
    }

    private SessionRecruitmentListItemResponse toListItemResponse(
            SessionRecruitment recruitment,
            LocalDateTime now,
            Set<Long> interestedIds
    ) {
        return SessionRecruitmentListItemResponse.builder()
                .sessionRecruitmentId(recruitment.getSessionRecruitmentId())
                .bandId(recruitment.getBand().getId())
                .recruitmentTitle(recruitment.getRecruitmentTitle())
                .bandName(recruitment.getBand().getName())
                .bandGenre(recruitment.getBand().getGenre().getName())
                .bandRegion(recruitment.getBand().getRegion().getName())
                .content(recruitment.getContent())
                .part(recruitment.getPart())
                .skillLevel(recruitment.getSkillLevel())
                .practiceSchedule(recruitment.getPracticeSchedule())
                .deadlineAt(recruitment.getDeadlineAt())
                .dDay(calculateDDay(recruitment.getDeadlineAt().toLocalDate()))
                .isNew(isNewRecruitment(recruitment.getCreatedAt(), now))
                .isInterested(interestedIds.contains(recruitment.getSessionRecruitmentId()))
                .build();
    }

    static boolean isNewRecruitment(LocalDateTime createdAt, LocalDateTime now) {
        return createdAt != null && now.isBefore(createdAt.plusDays(3));
    }

    private Long calculateDDay(LocalDate deadlineDate) {
        return ChronoUnit.DAYS.between(LocalDate.now(), deadlineDate);
    }
}
