package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentInterestResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.InterestedRecruitmentItemResponse;
import com.umc.bscene.domain.session.dto.recruitment.response.InterestedRecruitmentListResponse;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.entity.SessionRecruitmentInterest;
import com.umc.bscene.domain.session.enums.code.SessionErrorCode;
import com.umc.bscene.domain.session.exception.SessionException;
import com.umc.bscene.domain.session.repository.SessionRecruitmentInterestRepository;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import com.umc.bscene.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionRecruitmentInterestService {

    private final SessionRecruitmentInterestRepository interestRepository;
    private final SessionRecruitmentRepository recruitmentRepository;
    private final UserRepository userRepository;

    public InterestedRecruitmentListResponse getMyInterests(
            Long userId,
            Long cursorId,
            Integer size
    ) {
        int pageSize = size == null ? 10 : Math.max(1, Math.min(size, 50));
        List<SessionRecruitmentInterest> interests = interestRepository.findMyInterests(
                userId,
                cursorId,
                PageRequest.of(0, pageSize + 1)
        );
        boolean hasNext = interests.size() > pageSize;
        List<SessionRecruitmentInterest> sliced = hasNext
                ? interests.subList(0, pageSize)
                : interests;
        List<InterestedRecruitmentItemResponse> content = sliced.stream()
                .map(InterestedRecruitmentItemResponse::from)
                .toList();
        Long nextCursor = hasNext && !sliced.isEmpty()
                ? sliced.get(sliced.size() - 1).getSessionRecruitmentInterestId()
                : null;
        return new InterestedRecruitmentListResponse(
                content, pageSize, nextCursor, hasNext
        );
    }

    @Transactional
    public SessionRecruitmentInterestResponse setInterest(Long userId, Long recruitmentId) {
        SessionRecruitment recruitment = getActiveRecruitment(recruitmentId);

        if (interestRepository
                .existsBySessionRecruitment_SessionRecruitmentIdAndUser_Id(recruitmentId, userId)) {
            throw new SessionException(SessionErrorCode.SESSION_RECRUITMENT_INTEREST_ALREADY_EXISTS);
        }

        try {
            interestRepository.save(SessionRecruitmentInterest.builder()
                    .sessionRecruitment(recruitment)
                    .user(userRepository.getReferenceById(userId))
                    .build());
        } catch (DataIntegrityViolationException exception) {
            throw new SessionException(SessionErrorCode.SESSION_RECRUITMENT_INTEREST_ALREADY_EXISTS);
        }

        return SessionRecruitmentInterestResponse.of(recruitmentId, true);
    }

    @Transactional
    public SessionRecruitmentInterestResponse unsetInterest(Long userId, Long recruitmentId) {
        getActiveRecruitment(recruitmentId);
        interestRepository.deleteBySessionRecruitment_SessionRecruitmentIdAndUser_Id(
                recruitmentId,
                userId
        );
        return SessionRecruitmentInterestResponse.of(recruitmentId, false);
    }

    private SessionRecruitment getActiveRecruitment(Long recruitmentId) {
        return recruitmentRepository.findBySessionRecruitmentIdAndDeletedAtIsNull(recruitmentId)
                .orElseThrow(() -> new SessionException(
                        SessionErrorCode.SESSION_RECRUITMENT_NOT_FOUND
                ));
    }
}
