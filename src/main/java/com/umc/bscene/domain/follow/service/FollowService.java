package com.umc.bscene.domain.follow.service;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.follow.dto.response.FollowResponse;
import com.umc.bscene.domain.follow.entity.Follow;
import com.umc.bscene.domain.follow.exception.FollowException;
import com.umc.bscene.domain.follow.repository.FollowRepository;
import com.umc.bscene.domain.follow.response.code.FollowErrorCode;
import com.umc.bscene.global.security.entity.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final BandRepository bandRepository;

    // 사용자가 밴드를 팔로우
    @Transactional
    public FollowResponse follow(AuthMember authMember, Long bandId) {
        Long userId = authMember.getUser().getId();

        // 팔로우 대상 밴드가 존재하는지 확인 (없으면 404 반환)
        Band band = bandRepository.findById(bandId)
                .orElseThrow(() -> new BandException(BandErrorCode.BAND_NOT_FOUND));

        // 이미 팔로우한 경우 중복 저장 방지
        if (followRepository.existsByBand_IdAndUser_Id(bandId, userId)) {
            throw new FollowException(FollowErrorCode.ALREADY_FOLLOWED);
        }

        try {
            followRepository.save(Follow.builder()
                    .band(band)
                    .user(authMember.getUser())
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 위 중복 체크를 둘 다 통과한 경우, 유니크 제약이 최종 방어 → 409로 변환
            throw new FollowException(FollowErrorCode.ALREADY_FOLLOWED);
        }

        return FollowResponse.of(bandId, true);
    }

    // 사용자가 밴드 팔로우를 취소
    @Transactional
    public FollowResponse unfollow(AuthMember authMember, Long bandId) {
        Long userId = authMember.getUser().getId();

        // 팔로우 관계가 있으면 삭제, 없으면 이미 취소 상태이므로 그대로 둠(멱등 처리)
        followRepository.deleteByBand_IdAndUser_Id(bandId, userId);

        return FollowResponse.of(bandId, false);
    }
}
