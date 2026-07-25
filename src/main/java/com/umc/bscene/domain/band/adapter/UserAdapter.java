package com.umc.bscene.domain.band.adapter;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.entity.BandMemberProfile;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandMemberType;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.port.FollowPort;
import com.umc.bscene.domain.band.port.PerformancePort;
import com.umc.bscene.domain.band.port.SessionPort;
import com.umc.bscene.domain.band.repository.BandMemberProfileRepository;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.user.dto.response.BandMemberResponse;
import com.umc.bscene.domain.user.dto.response.MyBandProfile;
import com.umc.bscene.domain.user.dto.response.MyProfileResponse;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.port.BandPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class UserAdapter implements BandPort {

    private final BandMemberProfileRepository bandMemberProfileRepository;
    private final BandMemberRepository bandMemberRepository;
    private final BandRepository bandRepository;
    private final FollowPort followPort;
    private final SessionPort sessionPort;
    private final PerformancePort performancePort;

    @Override
    public void validateActiveBandMember(Long userId, Long bandId) {

        // 밴드 소속(ACCEPTED)이 아니면 접근 불가
        BandMember bandMember = bandMemberRepository
                .findByBand_IdAndUser_IdAndStatus(bandId, userId, BandMemberStatus.ACCEPTED)
                .orElseThrow(() -> new BandException(BandErrorCode.BAND_PERMISSION_DENIED));

        // 세션 멤버(memberType=SESSION)는 밴드 기능 사용 불가
        if (!bandMember.isMember()) {
            throw new BandException(BandErrorCode.BAND_PERMISSION_DENIED);
        }

        // 멤버는 맞지만 이 밴드의 프로필로 전환된 상태가 아님
        // FE가 모드 전환 후 재시도하는 복구 플로우를 탈 수 있도록 위와 다른 코드로 구분
        BandMemberProfile profile = bandMember.getBandMemberProfile();
        if (profile == null || !Boolean.TRUE.equals(profile.getActive())) {
            throw new BandException(BandErrorCode.BAND_MODE_REQUIRED);
        }
    }

    @Override
    @Transactional
    public void registerSessionMember(Long bandId, User applicant, String nickname, Part part) {

        // (bandId, userId) 유니크 제약 - 이미 소속(정회원/세션)이면 중복 등록하지 않음
        if (bandMemberRepository.existsByBand_IdAndUser_Id(bandId, applicant.getId())) {
            return;
        }

        Band band = bandRepository.findById(bandId)
                .orElseThrow(() -> new BandException(BandErrorCode.BAND_NOT_FOUND));

        // 지원 시점 지원서의 닉네임·파트로 pre-fill한 멤버 프로필 생성 (active는 기본값 false)
        BandMemberProfile profile = bandMemberProfileRepository.save(BandMemberProfile.builder()
                .nickname(nickname)
                .part(part)
                .user(applicant)
                .build());

        bandMemberRepository.save(BandMember.builder()
                .band(band)
                .user(applicant)
                .bandMemberProfile(profile)
                .status(BandMemberStatus.ACCEPTED)
                .memberType(BandMemberType.SESSION)
                .build());
    }

    @Override
    public BandMemberResponse getActiveBandMemberProfile(Long userId) {
        // 밴드 모드 사용자는 반드시 활성 멤버 프로필을 가짐 (없으면 데이터 이상)
        BandMemberProfile profile = bandMemberProfileRepository.findByUser_IdAndActiveTrue(userId)
                .orElseThrow(() -> new BandException(BandErrorCode.BAND_MEMBER_PROFILE_NOT_FOUND));

        // 활성 프로필로 활동 중인 소속 밴드 조회 - 소속 밴드가 없으면 밴드 정보는 null로 응답
        BandMember bandMember = bandMemberRepository
                .findWithBandByUser_IdInAndStatus(Set.of(userId), BandMemberStatus.ACCEPTED)
                .stream()
                .findFirst()
                .orElse(null);

        assert bandMember != null;
        Band band = bandMember.getBand();

        return buildBandMemberResponse(band, profile, bandMember.isMember());
    }

    @Override
    public Long getActiveBandMemberProfile_BandIdIdByUserId(Long userId) {

        // BandRepository를 사용, join을 통해 BandMember, BandMemberProfile을 타고 가서 활성화 필터링
        BandMember bandMember = bandMemberRepository.findWithBandByUser_IdAndActiveProfile(userId, true, BandMemberStatus.ACCEPTED)
                .orElseThrow(() -> new BandException(BandErrorCode.BAND_MEMBER_NOT_FOUND));

        if (!bandMember.isMember()) {
            throw new BandException(BandErrorCode.BAND_PERMISSION_DENIED);
        }

        return bandMember.getBand().getId();
    }

    @Override
    public List<MyBandProfile> getAssociatedBandProfiles(Long userId) {
        return bandMemberRepository.getMyBandProfiles(userId, BandMemberStatus.ACCEPTED);
    }

    @Override
    @Transactional
    public void changeProfileByProfileId(Long userId, Long profileId) {

        BandMemberProfile profile = bandMemberProfileRepository.findByIdAndUser_Id(profileId, userId)
                .orElseThrow(() -> new BandException(BandErrorCode.PROFILE_ACTIVATION_FORBIDDEN));

        bandMemberProfileRepository.findByUser_IdAndActiveTrue(userId)
                .ifPresent(BandMemberProfile::deactivate);

        profile.activate();
    }

    @Override
    @Transactional
    public void deactivateCurrentActiveProfile(Long userId) {
        bandMemberProfileRepository.findByUser_IdAndActiveTrue(userId)
                .ifPresent(BandMemberProfile::deactivate);
    }

    private BandMemberResponse buildBandMemberResponse(Band band, BandMemberProfile profile, boolean isMember) {
        if (band != null) {
            return new BandMemberResponse(
                    profile.getId(),
                    band.getProfileImageUrl(),
                    profile.getNickname(),
                    band.getName(),
                    List.of(profile.getPart().getDescription()),
                    Math.toIntExact(followPort.countFollowersByBandId(band.getId())),
                    Math.toIntExact(sessionPort.getActiveSessionApplicantCount(band.getId())),
                    Math.toIntExact(performancePort.countPerformancesByBandIdAndStatus(band.getId())),
                    isMember
            );
        } else {
            return new BandMemberResponse(
                    profile.getId(),
                    null,
                    profile.getNickname(),
                    null,
                    List.of(profile.getPart().getDescription()),
                    null,
                    null,
                    null,
                    isMember
            );
        }
    }
}
