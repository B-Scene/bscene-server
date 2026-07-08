package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.session.dto.recruitment.request.SessionRecruitmentCreateRequest;
import com.umc.bscene.domain.session.dto.recruitment.request.SessionRecruitmentUpdateRequest;
import com.umc.bscene.domain.session.dto.recruitment.response.SessionRecruitmentCreateResponse;
import com.umc.bscene.domain.band.entity.BandProfile;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.enums.code.SessionErrorCode;
import com.umc.bscene.domain.session.exception.BandProfileException;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import com.umc.bscene.domain.session.service.SessionRecruitmentCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionRecruitmentCommandServiceImpl implements SessionRecruitmentCommandService {

    private final SessionRecruitmentRepository sessionRecruitmentRepository;
    private final BandMemberRepository bandMemberRepository;

    @Override
    public SessionRecruitmentCreateResponse createSessionRecruitment(
            Long userId,
            SessionRecruitmentCreateRequest request
    ) {
        BandMember bandMember = bandMemberRepository
                .findByIdAndUser_IdAndStatus(
                        request.getBandMemberId(),
                        userId,
                        BandMemberStatus.ACCEPTED
                )
                .orElseThrow(() -> new BandException(BandErrorCode.BAND_MEMBER_NOT_FOUND));

        Band band = bandMember.getBand();

        // 추가: 세션 모집 공고는 초기 밴드 생성자(대표자)만 등록 가능
        validateBandOwner(band, userId);

        BandProfile bandProfile = bandMember.getBandProfile();

        if (bandProfile == null) {
            throw new BandProfileException(SessionErrorCode.BAND_PROFILE_NOT_FOUND);
        }

        validateDeadlineAt(request.getDeadlineAt());

        SessionRecruitment recruitment = SessionRecruitment.builder()
                .band(band)
                .bandProfile(bandProfile)
                .recruitmentTitle(request.getRecruitmentTitle())
                .content(request.getContent())
                .part(request.getPart())
                .skillLevel(request.getSkillLevel())
                .genre(request.getGenre())
                .region(request.getRegion())
                .practiceSchedule(request.getPracticeSchedule())
                .practicePlace(request.getPracticePlace())
                .deadlineAt(request.getDeadlineAt())
                .qualification(request.getQualification())
                .build();

        SessionRecruitment savedRecruitment = sessionRecruitmentRepository.save(recruitment);

        return SessionRecruitmentCreateResponse.from(savedRecruitment);
    }

    @Override
    public SessionRecruitmentCreateResponse updateSessionRecruitment(
            Long userId,
            Long sessionRecruitmentId,
            SessionRecruitmentUpdateRequest request
    ) {
        SessionRecruitment recruitment = sessionRecruitmentRepository.findById(sessionRecruitmentId)
                .orElseThrow(() -> new BandProfileException(SessionErrorCode.SESSION_RECRUITMENT_NOT_FOUND));

        Band band = recruitment.getBand();

        // 변경: 수정도 대표자만 가능
        validateBandOwner(band, userId);

        validateDeadlineAt(request.getDeadlineAt());

        recruitment.update(
                request.getRecruitmentTitle(),
                request.getContent(),
                request.getPart(),
                request.getSkillLevel(),
                request.getGenre(),
                request.getRegion(),
                request.getPracticeSchedule(),
                request.getPracticePlace(),
                request.getDeadlineAt(),
                request.getQualification()
        );

        return SessionRecruitmentCreateResponse.from(recruitment);
    }

    @Override
    public void deleteSessionRecruitment(
            Long userId,
            Long sessionRecruitmentId
    ) {
        SessionRecruitment recruitment = sessionRecruitmentRepository.findById(sessionRecruitmentId)
                .orElseThrow(() -> new BandProfileException(SessionErrorCode.SESSION_RECRUITMENT_NOT_FOUND));

        Band band = recruitment.getBand();

        // 변경: 삭제도 대표자만 가능
        validateBandOwner(band, userId);

        sessionRecruitmentRepository.delete(recruitment);
    }

    private void validateBandOwner(Band band, Long userId) {
        if (!band.getOwner().getId().equals(userId)) {
            throw new BandProfileException(SessionErrorCode.BAND_PERMISSION_DENIED);
        }
    }

    private void validateDeadlineAt(LocalDateTime deadlineAt) {
        if (!deadlineAt.isAfter(LocalDateTime.now())) {
            throw new BandProfileException(SessionErrorCode.INVALID_SESSION_RECRUITMENT_DEADLINE);
        }
    }
}