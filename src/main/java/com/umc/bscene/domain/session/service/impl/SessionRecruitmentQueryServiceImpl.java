package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentDetailResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentListItemResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentListResponse;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SessionRegion;
import com.umc.bscene.domain.session.enums.code.SessionErrorCode;
import com.umc.bscene.domain.session.exception.SessionException;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import com.umc.bscene.domain.session.service.SessionRecruitmentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionRecruitmentQueryServiceImpl implements SessionRecruitmentQueryService {

    private final SessionRecruitmentRepository sessionRecruitmentRepository;

    @Override
    public SessionRecruitmentListResponse getSessionRecruitments(
            Part part,
            SessionGenre genre,
            SessionRegion region,
            String keyword,
            Long cursorId,
            Integer size
    ) {
        int pageSize = size == null ? 10 : size;
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);
        LocalDateTime now = LocalDateTime.now();

        List<SessionRecruitment> recruitments = findRecruitments(
                part,
                genre,
                region,
                keyword,
                cursorId,
                pageRequest,
                now
        );

        boolean hasNext = recruitments.size() > pageSize;

        List<SessionRecruitment> slicedRecruitments = hasNext
                ? recruitments.subList(0, pageSize)
                : recruitments;

        List<SessionRecruitmentListItemResponse> content = slicedRecruitments.stream()
                .map(this::toListItemResponse)
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
    public SessionRecruitmentDetailResponse getSessionRecruitmentDetail(Long recruitmentId) {

        SessionRecruitment recruitment = sessionRecruitmentRepository.findById(recruitmentId)
                .orElseThrow(() -> new SessionException(SessionErrorCode.SESSION_RECRUITMENT_NOT_FOUND));

        return SessionRecruitmentDetailResponse.builder()
                .sessionRecruitmentId(recruitment.getSessionRecruitmentId())
                .recruitmentTitle(recruitment.getRecruitmentTitle())
                .deadlineAt(recruitment.getDeadlineAt())
                .dDay(calculateDDay(recruitment.getDeadlineAt().toLocalDate()))

                // createdAt 기준 3일 이내면 true
                .isNew(recruitment.getCreatedAt() != null
                        && recruitment.getCreatedAt().toLocalDate().isAfter(LocalDate.now().minusDays(3)))

                // 밴드 프로필 정보
                .bandId(recruitment.getBand().getId())
                .bandName(recruitment.getBandProfile().getNickname())
                .bandProfileImageUrl(null)
                .bandGenre(recruitment.getBandProfile().getGenre().getDescription())
                .bandRegion(recruitment.getBandProfile().getRegion().getDescription())


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

    private List<SessionRecruitment> findRecruitments(
            Part part,
            SessionGenre genre,
            SessionRegion region,
            String keyword,
            Long cursorId,
            PageRequest pageRequest,
            LocalDateTime now
    ) {
        boolean hasFilter = part != null && genre != null && region != null;
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasCursor = cursorId != null;

        if (hasFilter) {
            if (hasCursor) {
                return sessionRecruitmentRepository
                        .findByDeletedAtIsNullAndDeadlineAtAfterAndPartAndGenreAndRegionAndSessionRecruitmentIdLessThanOrderBySessionRecruitmentIdDesc(
                                now,
                                part,
                                genre,
                                region,
                                cursorId,
                                pageRequest
                        );
            }

            return sessionRecruitmentRepository
                    .findByDeletedAtIsNullAndDeadlineAtAfterAndPartAndGenreAndRegionOrderBySessionRecruitmentIdDesc(
                            now,
                            part,
                            genre,
                            region,
                            pageRequest
                    );
        }

        if (hasKeyword) {
            if (hasCursor) {
                return sessionRecruitmentRepository
                        .findByDeletedAtIsNullAndDeadlineAtAfterAndRecruitmentTitleContainingAndSessionRecruitmentIdLessThanOrderBySessionRecruitmentIdDesc(
                                now,
                                keyword,
                                cursorId,
                                pageRequest
                        );
            }

            return sessionRecruitmentRepository
                    .findByDeletedAtIsNullAndDeadlineAtAfterAndRecruitmentTitleContainingOrderBySessionRecruitmentIdDesc(
                            now,
                            keyword,
                            pageRequest
                    );
        }

        if (hasCursor) {
            return sessionRecruitmentRepository
                    .findByDeletedAtIsNullAndDeadlineAtAfterAndSessionRecruitmentIdLessThanOrderBySessionRecruitmentIdDesc(
                            now,
                            cursorId,
                            pageRequest
                    );
        }

        return sessionRecruitmentRepository
                .findByDeletedAtIsNullAndDeadlineAtAfterOrderBySessionRecruitmentIdDesc(
                        now,
                        pageRequest
                );
    }

    private SessionRecruitmentListItemResponse toListItemResponse(SessionRecruitment recruitment) {
        return SessionRecruitmentListItemResponse.builder()
                .sessionRecruitmentId(recruitment.getSessionRecruitmentId())
                .bandId(recruitment.getBand().getId())
                .recruitmentTitle(recruitment.getRecruitmentTitle())
                .bandName(recruitment.getBandProfile().getNickname())
                .part(recruitment.getPart())
                .genre(recruitment.getGenre())
                .region(recruitment.getRegion())
                .deadlineAt(recruitment.getDeadlineAt())
                .dDay(calculateDDay(recruitment.getDeadlineAt().toLocalDate()))
                .build();
    }

    private Long calculateDDay(LocalDate deadlineDate) {
        return ChronoUnit.DAYS.between(LocalDate.now(), deadlineDate);
    }
}