package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.band.dto.request.BandMemberProfileCreateRequest;
import com.umc.bscene.domain.band.dto.request.BandMemberProfileUpdateRequest;
import com.umc.bscene.domain.band.dto.response.BandMemberProfileResponse;
import com.umc.bscene.domain.band.entity.BandMemberProfile;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.repository.BandMemberProfileRepository;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BandMemberProfileService {

    private final BandMemberProfileRepository bandMemberProfileRepository;
    private final BandMemberRepository bandMemberRepository;
    private final UserRepository userRepository;

    // 멤버 프로필 생성 (첫 프로필이면 자동으로 활성화)
    @Transactional
    public BandMemberProfileResponse createProfile(Long userId, BandMemberProfileCreateRequest request) {
        boolean isFirstProfile = !bandMemberProfileRepository.existsByUser_IdAndActiveTrue(userId);

        BandMemberProfile profile = BandMemberProfile.builder()
                .user(userRepository.getReferenceById(userId))
                .nickname(request.nickname())
                .part(request.part())
                .active(isFirstProfile)
                .build();

        return BandMemberProfileResponse.from(bandMemberProfileRepository.save(profile));
    }

    // 내 멤버 프로필 목록 조회
    public List<BandMemberProfileResponse> getProfiles(Long userId) {
        return bandMemberProfileRepository.findAllByUser_Id(userId).stream()
                .map(BandMemberProfileResponse::from)
                .toList();
    }

    // 내 멤버 프로필 단건 조회
    public BandMemberProfileResponse getProfile(Long userId, Long profileId) {
        return BandMemberProfileResponse.from(getOwnProfile(userId, profileId));
    }

    // 내 활성 프로필 조회
    public BandMemberProfileResponse getActiveProfile(Long userId) {
        BandMemberProfile profile = bandMemberProfileRepository.findByUser_IdAndActiveTrue(userId)
                .orElseThrow(() -> new BandException(BandErrorCode.BAND_MEMBER_PROFILE_NOT_FOUND));

        return BandMemberProfileResponse.from(profile);
    }

    // 멤버 프로필 수정
    @Transactional
    public BandMemberProfileResponse updateProfile(Long userId, Long profileId, BandMemberProfileUpdateRequest request) {
        BandMemberProfile profile = getOwnProfile(userId, profileId);

        profile.update(request.nickname(), request.part());

        return BandMemberProfileResponse.from(profile);
    }

    // 멤버 프로필 삭제 (밴드에서 사용 중이면 삭제 불가)
    @Transactional
    public void deleteProfile(Long userId, Long profileId) {
        BandMemberProfile profile = getOwnProfile(userId, profileId);

        if (bandMemberRepository.existsByBandMemberProfile_Id(profileId)) {
            throw new BandException(BandErrorCode.BAND_MEMBER_PROFILE_IN_USE);
        }

        bandMemberProfileRepository.delete(profile);
    }

    // 활성 프로필 전환
    @Transactional
    public BandMemberProfileResponse activateProfile(Long userId, Long profileId) {
        BandMemberProfile target = getOwnProfile(userId, profileId);

        bandMemberProfileRepository.findByUser_IdAndActiveTrue(userId)
                .filter(current -> !current.getId().equals(target.getId()))
                .ifPresent(BandMemberProfile::deactivate);

        target.activate();

        return BandMemberProfileResponse.from(target);
    }

    private BandMemberProfile getOwnProfile(Long userId, Long profileId) {
        return bandMemberProfileRepository.findByIdAndUser_Id(profileId, userId)
                .orElseThrow(() -> new BandException(BandErrorCode.BAND_MEMBER_PROFILE_NOT_FOUND));
    }
}
