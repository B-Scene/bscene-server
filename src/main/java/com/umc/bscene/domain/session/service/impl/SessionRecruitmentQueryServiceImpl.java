package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentListItemResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentListResponse;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SessionRegion;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import com.umc.bscene.domain.session.service.SessionRecruitmentQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

        List<SessionRecruitment> recruitments = findRecruitments(
                part,
                genre,
                region,
                keyword,
                cursorId,
                pageRequest
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

    private List<SessionRecruitment> findRecruitments(
            Part part,
            SessionGenre genre,
            SessionRegion region,
            String keyword,
            Long cursorId,
            PageRequest pageRequest
    ) {
        boolean hasFilter = part != null && genre != null && region != null;
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasCursor = cursorId != null;

        if (hasFilter) {
            if (hasCursor) {
                return sessionRecruitmentRepository
                        .findByDeletedAtIsNullAndPartAndGenreAndRegionAndSessionRecruitmentIdLessThanOrderBySessionRecruitmentIdDesc(
                                part,
                                genre,
                                region,
                                cursorId,
                                pageRequest
                        );
            }

            return sessionRecruitmentRepository
                    .findByDeletedAtIsNullAndPartAndGenreAndRegionOrderBySessionRecruitmentIdDesc(
                            part,
                            genre,
                            region,
                            pageRequest
                    );
        }

        if (hasKeyword) {
            if (hasCursor) {
                return sessionRecruitmentRepository
                        .findByDeletedAtIsNullAndRecruitmentTitleContainingAndSessionRecruitmentIdLessThanOrderBySessionRecruitmentIdDesc(
                                keyword,
                                cursorId,
                                pageRequest
                        );
            }

            return sessionRecruitmentRepository
                    .findByDeletedAtIsNullAndRecruitmentTitleContainingOrderBySessionRecruitmentIdDesc(
                            keyword,
                            pageRequest
                    );
        }

        if (hasCursor) {
            return sessionRecruitmentRepository
                    .findByDeletedAtIsNullAndSessionRecruitmentIdLessThanOrderBySessionRecruitmentIdDesc(
                            cursorId,
                            pageRequest
                    );
        }

        return sessionRecruitmentRepository
                .findByDeletedAtIsNullOrderBySessionRecruitmentIdDesc(pageRequest);
    }

    private SessionRecruitmentListItemResponse toListItemResponse(SessionRecruitment recruitment) {
        return SessionRecruitmentListItemResponse.builder()
                .sessionRecruitmentId(recruitment.getSessionRecruitmentId())
                .bandId(recruitment.getBand().getId())
                .recruitmentTitle(recruitment.getRecruitmentTitle())
                .bandName(recruitment.getBand().getName())
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