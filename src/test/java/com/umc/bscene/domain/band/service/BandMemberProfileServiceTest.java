package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.band.dto.request.BandMemberProfileCreateRequest;
import com.umc.bscene.domain.band.dto.request.BandMemberProfileUpdateRequest;
import com.umc.bscene.domain.band.dto.response.BandMemberProfileResponse;
import com.umc.bscene.domain.band.entity.BandMemberProfile;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.repository.BandMemberProfileRepository;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BandMemberProfileServiceTest {

    @Mock
    private BandMemberProfileRepository bandMemberProfileRepository;
    @Mock
    private BandMemberRepository bandMemberRepository;
    @Mock
    private UserRepository userRepository;

    private BandMemberProfileService service;

    private static final Long USER_ID = 1L;
    private static final Long PROFILE_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new BandMemberProfileService(bandMemberProfileRepository, bandMemberRepository, userRepository);
    }

    private BandMemberProfile profile(Long id, boolean active) {
        return BandMemberProfile.builder()
                .id(id)
                .user(User.builder().id(USER_ID).build())
                .nickname("닉네임")
                .part(Part.GUITAR)
                .active(active)
                .build();
    }

    @Test
    void createProfile_첫프로필이면_자동으로_활성화된다() {
        when(bandMemberProfileRepository.existsByUser_IdAndActiveTrue(USER_ID)).thenReturn(false);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
        when(bandMemberProfileRepository.save(any(BandMemberProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        BandMemberProfileCreateRequest request = new BandMemberProfileCreateRequest("닉네임", Part.GUITAR);

        BandMemberProfileResponse response = service.createProfile(USER_ID, request);

        assertTrue(response.active());
    }

    @Test
    void createProfile_기존_활성_프로필이_있으면_비활성으로_생성된다() {
        when(bandMemberProfileRepository.existsByUser_IdAndActiveTrue(USER_ID)).thenReturn(true);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
        when(bandMemberProfileRepository.save(any(BandMemberProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        BandMemberProfileCreateRequest request = new BandMemberProfileCreateRequest("닉네임", Part.DRUM);

        BandMemberProfileResponse response = service.createProfile(USER_ID, request);

        assertFalse(response.active());
    }

    @Test
    void getProfiles_전체_프로필_목록을_반환한다() {
        when(bandMemberProfileRepository.findAllByUser_Id(USER_ID)).thenReturn(List.of(profile(PROFILE_ID, true)));

        List<BandMemberProfileResponse> result = service.getProfiles(USER_ID);

        assertEquals(1, result.size());
    }

    @Test
    void getProfile_본인_소유가_아니면_예외() {
        when(bandMemberProfileRepository.findByIdAndUser_Id(PROFILE_ID, USER_ID)).thenReturn(Optional.empty());

        BandException exception = assertThrows(BandException.class, () -> service.getProfile(USER_ID, PROFILE_ID));

        assertEquals(BandErrorCode.BAND_MEMBER_PROFILE_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void getActiveProfile_활성_프로필이_없으면_예외() {
        when(bandMemberProfileRepository.findByUser_IdAndActiveTrue(USER_ID)).thenReturn(Optional.empty());

        BandException exception = assertThrows(BandException.class, () -> service.getActiveProfile(USER_ID));

        assertEquals(BandErrorCode.BAND_MEMBER_PROFILE_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void getActiveProfile_존재하면_반환한다() {
        when(bandMemberProfileRepository.findByUser_IdAndActiveTrue(USER_ID)).thenReturn(Optional.of(profile(PROFILE_ID, true)));

        BandMemberProfileResponse response = service.getActiveProfile(USER_ID);

        assertEquals(PROFILE_ID, response.id());
    }

    @Test
    void updateProfile_닉네임과_파트를_수정한다() {
        BandMemberProfile profile = profile(PROFILE_ID, true);
        when(bandMemberProfileRepository.findByIdAndUser_Id(PROFILE_ID, USER_ID)).thenReturn(Optional.of(profile));
        BandMemberProfileUpdateRequest request = new BandMemberProfileUpdateRequest("새닉네임", Part.BASS);

        BandMemberProfileResponse response = service.updateProfile(USER_ID, PROFILE_ID, request);

        assertEquals("새닉네임", response.nickname());
        assertEquals(Part.BASS, response.part());
    }

    @Test
    void deleteProfile_밴드에서_사용중이면_예외() {
        BandMemberProfile profile = profile(PROFILE_ID, true);
        when(bandMemberProfileRepository.findByIdAndUser_Id(PROFILE_ID, USER_ID)).thenReturn(Optional.of(profile));
        when(bandMemberRepository.existsByBandMemberProfile_Id(PROFILE_ID)).thenReturn(true);

        BandException exception = assertThrows(BandException.class, () -> service.deleteProfile(USER_ID, PROFILE_ID));

        assertEquals(BandErrorCode.BAND_MEMBER_PROFILE_IN_USE, exception.getBaseResponseCode());
        verify(bandMemberProfileRepository, never()).delete(any());
    }

    @Test
    void deleteProfile_사용중이_아니면_삭제된다() {
        BandMemberProfile profile = profile(PROFILE_ID, true);
        when(bandMemberProfileRepository.findByIdAndUser_Id(PROFILE_ID, USER_ID)).thenReturn(Optional.of(profile));
        when(bandMemberRepository.existsByBandMemberProfile_Id(PROFILE_ID)).thenReturn(false);

        service.deleteProfile(USER_ID, PROFILE_ID);

        verify(bandMemberProfileRepository).delete(profile);
    }

    @Test
    void activateProfile_기존_활성_프로필을_비활성화하고_대상을_활성화한다() {
        BandMemberProfile current = profile(200L, true);
        BandMemberProfile target = profile(PROFILE_ID, false);
        when(bandMemberProfileRepository.findByIdAndUser_Id(PROFILE_ID, USER_ID)).thenReturn(Optional.of(target));
        when(bandMemberProfileRepository.findByUser_IdAndActiveTrue(USER_ID)).thenReturn(Optional.of(current));

        BandMemberProfileResponse response = service.activateProfile(USER_ID, PROFILE_ID);

        assertTrue(response.active());
        assertFalse(current.getActive());
    }

    @Test
    void activateProfile_대상이_이미_활성프로필이면_그대로_유지한다() {
        BandMemberProfile target = profile(PROFILE_ID, true);
        when(bandMemberProfileRepository.findByIdAndUser_Id(PROFILE_ID, USER_ID)).thenReturn(Optional.of(target));
        when(bandMemberProfileRepository.findByUser_IdAndActiveTrue(USER_ID)).thenReturn(Optional.of(target));

        BandMemberProfileResponse response = service.activateProfile(USER_ID, PROFILE_ID);

        assertTrue(response.active());
    }
}
