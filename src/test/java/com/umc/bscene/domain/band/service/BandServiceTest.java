package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.dto.request.BandCreateRequest;
import com.umc.bscene.domain.band.dto.request.BandMemberInviteRequest;
import com.umc.bscene.domain.band.dto.request.BandMemberProfileCreateRequest;
import com.umc.bscene.domain.band.dto.request.BandOwnerTransferRequest;
import com.umc.bscene.domain.band.dto.request.BandUpdateRequest;
import com.umc.bscene.domain.band.dto.request.MusicLinkSaveRequest;
import com.umc.bscene.domain.band.dto.response.BandMemberAcceptResponse;
import com.umc.bscene.domain.band.dto.response.BandMemberProfileResponse;
import com.umc.bscene.domain.band.dto.response.BandMemberResponse;
import com.umc.bscene.domain.band.dto.response.BandMemberSearchItem;
import com.umc.bscene.domain.band.dto.response.BandNameCheckResponse;
import com.umc.bscene.domain.band.dto.response.BandProfileResponse;
import com.umc.bscene.domain.band.dto.response.BandResponse;
import com.umc.bscene.domain.band.dto.response.MusicLinkResponse;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.entity.BandMemberProfile;
import com.umc.bscene.domain.band.entity.MusicLink;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandMemberType;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.port.FollowPort;
import com.umc.bscene.domain.band.port.NotifyPort;
import com.umc.bscene.domain.band.port.PerformancePort;
import com.umc.bscene.domain.band.port.StreamPort;
import com.umc.bscene.domain.band.repository.BandMemberProfileRepository;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.repository.MusicLinkRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BandServiceTest {

    @Mock
    private BandRepository bandRepository;
    @Mock
    private BandMemberRepository bandMemberRepository;
    @Mock
    private BandMemberProfileRepository bandMemberProfileRepository;
    @Mock
    private MusicLinkRepository musicLinkRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FollowPort followPort;
    @Mock
    private PerformancePort performancePort;
    @Mock
    private StreamPort streamPort;
    @Mock
    private NotifyPort notifyPort;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private BandMemberProfileService bandMemberProfileService;

    private BandService service;

    private static final Long OWNER_ID = 1L;
    private static final Long BAND_ID = 10L;

    @BeforeEach
    void setUp() {
        service = new BandService(
                bandRepository, bandMemberRepository, bandMemberProfileRepository, musicLinkRepository,
                userRepository, followPort, performancePort, streamPort, notifyPort, eventPublisher,
                bandMemberProfileService
        );
        // 알림 발송이 TransactionSynchronizationManager.registerSynchronization 을 거치므로,
        // 트랜잭션 밖에서 호출되는 단위테스트에서도 예외 없이 등록만 되도록 동기화를 열어둔다.
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private User user(Long id) {
        return User.builder().id(id).build();
    }

    private Band band(Long id, Long ownerId) {
        return Band.builder()
                .id(id)
                .owner(user(ownerId))
                .name("밴드" + id)
                .genre(Genre.HARD_ROCK)
                .region(Region.SEOUL)
                .build();
    }

    private BandMemberProfile profile(Long id, Long userId) {
        return BandMemberProfile.builder().id(id).user(user(userId)).nickname("닉네임").part(Part.GUITAR).build();
    }

    // ---------- createBand ----------

    @Test
    void createBand_밴드명이_중복이면_예외() {
        BandCreateRequest request = new BandCreateRequest("밴드명", Genre.HARD_ROCK, Region.SEOUL, 100L, null, null);
        when(bandRepository.existsByName("밴드명")).thenReturn(true);

        BandException exception = assertThrows(BandException.class, () -> service.createBand(OWNER_ID, request));

        assertEquals(BandErrorCode.DUPLICATE_BAND_NAME, exception.getBaseResponseCode());
        verify(bandRepository, never()).save(any());
    }

    @Test
    void createBand_본인_소유가_아닌_프로필이면_예외() {
        BandCreateRequest request = new BandCreateRequest("밴드명", Genre.HARD_ROCK, Region.SEOUL, 100L, null, null);
        when(bandRepository.existsByName("밴드명")).thenReturn(false);
        when(bandMemberProfileRepository.findById(100L)).thenReturn(Optional.of(profile(100L, 999L)));

        BandException exception = assertThrows(BandException.class, () -> service.createBand(OWNER_ID, request));

        assertEquals(BandErrorCode.NOT_OWN_BAND_MEMBER_PROFILE, exception.getBaseResponseCode());
    }

    @Test
    void createBand_프로필_자체가_존재하지_않으면_예외() {
        BandCreateRequest request = new BandCreateRequest("밴드명", Genre.HARD_ROCK, Region.SEOUL, 100L, null, null);
        when(bandRepository.existsByName("밴드명")).thenReturn(false);
        when(bandMemberProfileRepository.findById(100L)).thenReturn(Optional.empty());

        BandException exception = assertThrows(BandException.class, () -> service.createBand(OWNER_ID, request));

        assertEquals(BandErrorCode.BAND_MEMBER_PROFILE_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void createBand_성공시_오너로_밴드멤버가_ACCEPTED_상태로_생성된다() {
        BandCreateRequest request = new BandCreateRequest("밴드명", Genre.HARD_ROCK, Region.SEOUL, 100L, null, null);
        when(bandRepository.existsByName("밴드명")).thenReturn(false);
        when(bandMemberProfileRepository.findById(100L)).thenReturn(Optional.of(profile(100L, OWNER_ID)));
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(user(OWNER_ID));
        when(bandRepository.save(any(Band.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BandResponse response = service.createBand(OWNER_ID, request);

        assertEquals("밴드명", response.name());
        verify(bandMemberRepository).save(argThat(member ->
                member.getStatus() == BandMemberStatus.ACCEPTED && member.getUser().getId().equals(OWNER_ID)
        ));
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    // ---------- checkBandName ----------

    @Test
    void checkBandName_존재하지_않으면_사용가능() {
        when(bandRepository.existsByName("새이름")).thenReturn(false);

        BandNameCheckResponse response = service.checkBandName("새이름");

        assertTrue(response.available());
    }

    // ---------- getBandProfile / getBandDetail ----------

    @Test
    void getBandProfile_존재하지_않으면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.empty());

        BandException exception = assertThrows(BandException.class, () -> service.getBandProfile(BAND_ID));

        assertEquals(BandErrorCode.BAND_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void getBandProfile_팔로워수_멤버수_공연수를_포함해_반환한다() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));
        when(followPort.countFollowersByBandId(BAND_ID)).thenReturn(5L);
        when(bandMemberRepository.countByBand_IdAndStatus(BAND_ID, BandMemberStatus.ACCEPTED)).thenReturn(3L);
        when(performancePort.countPerformancesByBandId(BAND_ID)).thenReturn(2L);

        BandProfileResponse response = service.getBandProfile(BAND_ID);

        assertEquals(5L, response.followerCount());
        assertEquals(3L, response.memberCount());
        assertEquals(2L, response.performanceCount());
    }

    @Test
    void getBandDetail_라이브중이면_liveId를_포함한다() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));
        when(followPort.countFollowersByBandId(BAND_ID)).thenReturn(0L);
        when(followPort.isFollowing(OWNER_ID, BAND_ID)).thenReturn(true);
        when(streamPort.findOpenLiveId(BAND_ID)).thenReturn(Optional.of(999L));

        var response = service.getBandDetail(OWNER_ID, BAND_ID);

        assertTrue(response.isFollowing());
        assertTrue(response.isLive());
        assertEquals(999L, response.liveId());
    }

    // ---------- updateBandProfile ----------

    @Test
    void updateBandProfile_오너가_아니면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));
        BandUpdateRequest request = new BandUpdateRequest("새이름", null, null, null, null);

        BandException exception = assertThrows(BandException.class,
                () -> service.updateBandProfile(999L, BAND_ID, request));

        assertEquals(BandErrorCode.NOT_BAND_OWNER, exception.getBaseResponseCode());
    }

    @Test
    void updateBandProfile_변경할_이름이_이미_존재하면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));
        when(bandRepository.existsByName("중복이름")).thenReturn(true);
        BandUpdateRequest request = new BandUpdateRequest("중복이름", null, null, null, null);

        BandException exception = assertThrows(BandException.class,
                () -> service.updateBandProfile(OWNER_ID, BAND_ID, request));

        assertEquals(BandErrorCode.DUPLICATE_BAND_NAME, exception.getBaseResponseCode());
    }

    @Test
    void updateBandProfile_이름이_기존과_같으면_중복체크를_하지_않는다() {
        Band band = band(BAND_ID, OWNER_ID);
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band));
        when(followPort.countFollowersByBandId(BAND_ID)).thenReturn(0L);
        when(bandMemberRepository.countByBand_IdAndStatus(BAND_ID, BandMemberStatus.ACCEPTED)).thenReturn(0L);
        when(performancePort.countPerformancesByBandId(BAND_ID)).thenReturn(0L);
        BandUpdateRequest request = new BandUpdateRequest(band.getName(), null, null, null, null);

        service.updateBandProfile(OWNER_ID, BAND_ID, request);

        verify(bandRepository, never()).existsByName(any());
    }

    // ---------- inviteMember ----------

    @Test
    void inviteMember_오너가_아니면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));
        BandMemberInviteRequest request = new BandMemberInviteRequest(2L, BandMemberType.MEMBER);

        BandException exception = assertThrows(BandException.class,
                () -> service.inviteMember(999L, BAND_ID, request));

        assertEquals(BandErrorCode.BAND_MEMBER_INVITE_FORBIDDEN, exception.getBaseResponseCode());
    }

    @Test
    void inviteMember_초대대상_유저가_없으면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        BandMemberInviteRequest request = new BandMemberInviteRequest(2L, BandMemberType.MEMBER);

        BandException exception = assertThrows(BandException.class,
                () -> service.inviteMember(OWNER_ID, BAND_ID, request));

        assertEquals(BandErrorCode.INVITEE_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void inviteMember_이미_밴드멤버면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(bandMemberRepository.existsByBand_IdAndUser_Id(BAND_ID, 2L)).thenReturn(true);
        BandMemberInviteRequest request = new BandMemberInviteRequest(2L, BandMemberType.MEMBER);

        BandException exception = assertThrows(BandException.class,
                () -> service.inviteMember(OWNER_ID, BAND_ID, request));

        assertEquals(BandErrorCode.ALREADY_BAND_MEMBER, exception.getBaseResponseCode());
    }

    @Test
    void inviteMember_성공하면_초대상태의_멤버가_저장된다() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L)));
        when(bandMemberRepository.existsByBand_IdAndUser_Id(BAND_ID, 2L)).thenReturn(false);
        when(bandMemberRepository.save(any(BandMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BandMemberInviteRequest request = new BandMemberInviteRequest(2L, BandMemberType.MEMBER);

        BandMemberResponse response = service.inviteMember(OWNER_ID, BAND_ID, request);

        assertEquals(2L, response.userId());
        verify(bandMemberRepository).save(any(BandMember.class));
    }

    // ---------- acceptInvite ----------

    @Test
    void acceptInvite_초대받은_적이_없으면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));
        when(bandMemberRepository.findByBand_IdAndUser_Id(BAND_ID, 2L)).thenReturn(Optional.empty());
        BandMemberProfileCreateRequest request = new BandMemberProfileCreateRequest("닉네임", Part.GUITAR);

        BandException exception = assertThrows(BandException.class,
                () -> service.acceptInvite(2L, BAND_ID, request));

        assertEquals(BandErrorCode.NOT_INVITED_MEMBER, exception.getBaseResponseCode());
    }

    @Test
    void acceptInvite_초대받은_상태가_아니면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));
        BandMember accepted = BandMember.builder().id(1L).band(band(BAND_ID, OWNER_ID)).user(user(2L))
                .status(BandMemberStatus.ACCEPTED).build();
        when(bandMemberRepository.findByBand_IdAndUser_Id(BAND_ID, 2L)).thenReturn(Optional.of(accepted));
        BandMemberProfileCreateRequest request = new BandMemberProfileCreateRequest("닉네임", Part.GUITAR);

        BandException exception = assertThrows(BandException.class,
                () -> service.acceptInvite(2L, BAND_ID, request));

        assertEquals(BandErrorCode.INVITE_ALREADY_PROCESSED, exception.getBaseResponseCode());
    }

    @Test
    void acceptInvite_성공시_생성한_프로필로_수락처리된다() {
        Band band = band(BAND_ID, OWNER_ID);
        BandMember invited = BandMember.builder().id(1L).band(band).user(user(2L))
                .status(BandMemberStatus.INVITED).build();
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band));
        when(bandMemberRepository.findByBand_IdAndUser_Id(BAND_ID, 2L)).thenReturn(Optional.of(invited));
        BandMemberProfileCreateRequest request = new BandMemberProfileCreateRequest("닉네임", Part.GUITAR);
        when(bandMemberProfileService.createProfile(2L, request))
                .thenReturn(new BandMemberProfileResponse(300L, "닉네임", Part.GUITAR, true, null));
        when(bandMemberProfileRepository.findById(300L)).thenReturn(Optional.of(profile(300L, 2L)));

        BandMemberAcceptResponse response = service.acceptInvite(2L, BAND_ID, request);

        assertEquals(BandMemberStatus.ACCEPTED, response.status());
        assertEquals(300L, response.bandMemberProfileId());
    }

    // ---------- rejectInvite ----------

    @Test
    void rejectInvite_이미_수락된_멤버는_거절할수_없다() {
        BandMember accepted = BandMember.builder().id(1L).band(band(BAND_ID, OWNER_ID)).user(user(2L))
                .status(BandMemberStatus.ACCEPTED).build();
        when(bandMemberRepository.findByBand_IdAndUser_Id(BAND_ID, 2L)).thenReturn(Optional.of(accepted));

        BandException exception = assertThrows(BandException.class, () -> service.rejectInvite(2L, BAND_ID));

        assertEquals(BandErrorCode.ALREADY_ACCEPTED_MEMBER, exception.getBaseResponseCode());
    }

    @Test
    void rejectInvite_멤버_기록이_없으면_예외() {
        when(bandMemberRepository.findByBand_IdAndUser_Id(BAND_ID, 2L)).thenReturn(Optional.empty());

        BandException exception = assertThrows(BandException.class, () -> service.rejectInvite(2L, BAND_ID));

        assertEquals(BandErrorCode.BAND_MEMBER_NOT_FOUND, exception.getBaseResponseCode());
    }

    // ---------- removeMember ----------

    @Test
    void removeMember_오너는_제거할수_없다() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));

        BandException exception = assertThrows(BandException.class,
                () -> service.removeMember(OWNER_ID, BAND_ID, OWNER_ID));

        assertEquals(BandErrorCode.CANNOT_REMOVE_OWNER, exception.getBaseResponseCode());
    }

    @Test
    void removeMember_오너가_아니면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));

        BandException exception = assertThrows(BandException.class,
                () -> service.removeMember(999L, BAND_ID, 2L));

        assertEquals(BandErrorCode.NOT_BAND_OWNER, exception.getBaseResponseCode());
    }

    @Test
    void removeMember_대상_멤버_기록이_없으면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));
        when(bandMemberRepository.findByBand_IdAndUser_Id(BAND_ID, 2L)).thenReturn(Optional.empty());

        BandException exception = assertThrows(BandException.class,
                () -> service.removeMember(OWNER_ID, BAND_ID, 2L));

        assertEquals(BandErrorCode.BAND_MEMBER_NOT_FOUND, exception.getBaseResponseCode());
    }

    // ---------- searchInviteTargets ----------

    @Test
    void searchInviteTargets_키워드가_공백이면_예외() {
        BandException exception = assertThrows(BandException.class,
                () -> service.searchInviteTargets(BAND_ID, "   "));

        assertEquals(BandErrorCode.INVALID_MEMBER_SEARCH_KEYWORD, exception.getBaseResponseCode());
    }

    @Test
    void searchInviteTargets_밴드멤버가_아닌_유저는_초대가능으로_표시된다() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));
        User found = user(2L);
        when(userRepository.findByNameContaining("검색어")).thenReturn(List.of(found));
        when(bandMemberRepository.findWithUserByBandIdAndUserIdIn(eq(BAND_ID), anyList())).thenReturn(List.of());

        List<BandMemberSearchItem> result = service.searchInviteTargets(BAND_ID, "검색어");

        assertEquals(1, result.size());
        assertTrue(result.get(0).inviteAvailable());
    }

    // ---------- getMusicLink / saveMusicLink ----------

    @Test
    void getMusicLink_링크가_없으면_빈응답을_반환한다() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));
        when(musicLinkRepository.findByBand_Id(BAND_ID)).thenReturn(Optional.empty());

        MusicLinkResponse response = service.getMusicLink(BAND_ID);

        assertEquals(MusicLinkResponse.from(null), response);
    }

    @Test
    void saveMusicLink_밴드멤버가_아니면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));
        when(bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(BAND_ID, 2L, BandMemberStatus.ACCEPTED))
                .thenReturn(false);
        MusicLinkSaveRequest request = new MusicLinkSaveRequest(null, null, null, null, null, null);

        BandException exception = assertThrows(BandException.class,
                () -> service.saveMusicLink(2L, BAND_ID, request));

        assertEquals(BandErrorCode.NOT_BAND_MEMBER, exception.getBaseResponseCode());
    }

    @Test
    void saveMusicLink_etc플랫폼만_있고_링크가_없으면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));
        when(bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(BAND_ID, OWNER_ID, BandMemberStatus.ACCEPTED))
                .thenReturn(true);
        MusicLinkSaveRequest request = new MusicLinkSaveRequest(
                null, null, null, com.umc.bscene.domain.band.enums.EtcMusicPlatform.MELON, null, null
        );

        BandException exception = assertThrows(BandException.class,
                () -> service.saveMusicLink(OWNER_ID, BAND_ID, request));

        assertEquals(BandErrorCode.INVALID_ETC_MUSIC_LINK, exception.getBaseResponseCode());
    }

    @Test
    void saveMusicLink_기존링크가_있으면_수정만하고_저장하지않는다() {
        Band band = band(BAND_ID, OWNER_ID);
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band));
        when(bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(BAND_ID, OWNER_ID, BandMemberStatus.ACCEPTED))
                .thenReturn(true);
        MusicLink existing = MusicLink.builder().id(1L).band(band).build();
        when(musicLinkRepository.findByBand_Id(BAND_ID)).thenReturn(Optional.of(existing));
        MusicLinkSaveRequest request = new MusicLinkSaveRequest("spotify", null, null, null, null, null);

        MusicLinkResponse response = service.saveMusicLink(OWNER_ID, BAND_ID, request);

        assertEquals("spotify", response.spotifyUrl());
        verify(musicLinkRepository, never()).save(any());
    }

    @Test
    void saveMusicLink_링크가_없으면_새로_저장한다() {
        Band band = band(BAND_ID, OWNER_ID);
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band));
        when(bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(BAND_ID, OWNER_ID, BandMemberStatus.ACCEPTED))
                .thenReturn(true);
        when(musicLinkRepository.findByBand_Id(BAND_ID)).thenReturn(Optional.empty());
        when(musicLinkRepository.save(any(MusicLink.class))).thenAnswer(invocation -> invocation.getArgument(0));
        MusicLinkSaveRequest request = new MusicLinkSaveRequest("spotify", null, null, null, null, null);

        MusicLinkResponse response = service.saveMusicLink(OWNER_ID, BAND_ID, request);

        assertEquals("spotify", response.spotifyUrl());
        verify(musicLinkRepository).save(any(MusicLink.class));
    }

    // ---------- leaveBand ----------

    @Test
    void leaveBand_오너는_탈퇴할수_없다() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));

        BandException exception = assertThrows(BandException.class, () -> service.leaveBand(OWNER_ID, BAND_ID));

        assertEquals(BandErrorCode.BAND_OWNER_CANNOT_LEAVE, exception.getBaseResponseCode());
    }

    @Test
    void leaveBand_수락상태가_아니면_예외() {
        Band band = band(BAND_ID, OWNER_ID);
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band));
        BandMember invited = BandMember.builder().id(1L).band(band).user(user(2L))
                .status(BandMemberStatus.INVITED).build();
        when(bandMemberRepository.findByBand_IdAndUser_Id(BAND_ID, 2L)).thenReturn(Optional.of(invited));

        BandException exception = assertThrows(BandException.class, () -> service.leaveBand(2L, BAND_ID));

        assertEquals(BandErrorCode.BAND_PERMISSION_DENIED, exception.getBaseResponseCode());
    }

    @Test
    void leaveBand_멤버_기록이_없으면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));
        when(bandMemberRepository.findByBand_IdAndUser_Id(BAND_ID, 2L)).thenReturn(Optional.empty());

        BandException exception = assertThrows(BandException.class, () -> service.leaveBand(2L, BAND_ID));

        assertEquals(BandErrorCode.BAND_MEMBER_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void leaveBand_성공시_멤버가_삭제된다() {
        Band band = band(BAND_ID, OWNER_ID);
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band));
        BandMember accepted = BandMember.builder().id(1L).band(band).user(user(2L))
                .status(BandMemberStatus.ACCEPTED).build();
        when(bandMemberRepository.findByBand_IdAndUser_Id(BAND_ID, 2L)).thenReturn(Optional.of(accepted));

        service.leaveBand(2L, BAND_ID);

        verify(bandMemberRepository).delete(accepted);
    }

    @Test
    void leaveBand_탈퇴후_프로필을_다른_밴드에서도_사용중이면_삭제하지않는다() {
        Band band = band(BAND_ID, OWNER_ID);
        BandMemberProfile profile = profile(300L, 2L);
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band));
        BandMember accepted = BandMember.builder().id(1L).band(band).user(user(2L))
                .bandMemberProfile(profile).status(BandMemberStatus.ACCEPTED).build();
        when(bandMemberRepository.findByBand_IdAndUser_Id(BAND_ID, 2L)).thenReturn(Optional.of(accepted));
        when(bandMemberRepository.existsByBandMemberProfile_Id(300L)).thenReturn(true);

        service.leaveBand(2L, BAND_ID);

        verify(bandMemberRepository).delete(accepted);
        verify(bandMemberProfileRepository, never()).delete(any());
    }

    @Test
    void leaveBand_탈퇴후_프로필이_더이상_사용되지않으면_함께_삭제한다() {
        Band band = band(BAND_ID, OWNER_ID);
        BandMemberProfile profile = profile(300L, 2L);
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band));
        BandMember accepted = BandMember.builder().id(1L).band(band).user(user(2L))
                .bandMemberProfile(profile).status(BandMemberStatus.ACCEPTED).build();
        when(bandMemberRepository.findByBand_IdAndUser_Id(BAND_ID, 2L)).thenReturn(Optional.of(accepted));
        when(bandMemberRepository.existsByBandMemberProfile_Id(300L)).thenReturn(false);

        service.leaveBand(2L, BAND_ID);

        verify(bandMemberRepository).delete(accepted);
        verify(bandMemberProfileRepository).delete(profile);
    }

    // ---------- transferOwnership ----------

    @Test
    void transferOwnership_오너가_아니면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));
        BandOwnerTransferRequest request = new BandOwnerTransferRequest(2L);

        BandException exception = assertThrows(BandException.class,
                () -> service.transferOwnership(999L, BAND_ID, request));

        assertEquals(BandErrorCode.NOT_BAND_OWNER, exception.getBaseResponseCode());
    }

    @Test
    void transferOwnership_본인에게는_양도할수_없다() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));
        BandOwnerTransferRequest request = new BandOwnerTransferRequest(OWNER_ID);

        BandException exception = assertThrows(BandException.class,
                () -> service.transferOwnership(OWNER_ID, BAND_ID, request));

        assertEquals(BandErrorCode.CANNOT_TRANSFER_OWNER_TO_SELF, exception.getBaseResponseCode());
    }

    @Test
    void transferOwnership_대상_멤버_기록이_없으면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, OWNER_ID)));
        when(bandMemberRepository.findByBand_IdAndUser_Id(BAND_ID, 2L)).thenReturn(Optional.empty());
        BandOwnerTransferRequest request = new BandOwnerTransferRequest(2L);

        BandException exception = assertThrows(BandException.class,
                () -> service.transferOwnership(OWNER_ID, BAND_ID, request));

        assertEquals(BandErrorCode.BAND_MEMBER_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void transferOwnership_대상이_수락된_MEMBER가_아니면_예외() {
        Band band = band(BAND_ID, OWNER_ID);
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band));
        BandMember session = BandMember.builder().id(2L).band(band).user(user(2L))
                .status(BandMemberStatus.ACCEPTED).memberType(BandMemberType.SESSION).build();
        when(bandMemberRepository.findByBand_IdAndUser_Id(BAND_ID, 2L)).thenReturn(Optional.of(session));
        BandOwnerTransferRequest request = new BandOwnerTransferRequest(2L);

        BandException exception = assertThrows(BandException.class,
                () -> service.transferOwnership(OWNER_ID, BAND_ID, request));

        assertEquals(BandErrorCode.INVALID_OWNER_TRANSFER_TARGET, exception.getBaseResponseCode());
    }

    @Test
    void transferOwnership_성공시_밴드_오너가_변경된다() {
        Band band = band(BAND_ID, OWNER_ID);
        User newOwnerUser = user(2L);
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band));
        BandMember member = BandMember.builder().id(2L).band(band).user(newOwnerUser)
                .status(BandMemberStatus.ACCEPTED).memberType(BandMemberType.MEMBER).build();
        when(bandMemberRepository.findByBand_IdAndUser_Id(BAND_ID, 2L)).thenReturn(Optional.of(member));
        BandOwnerTransferRequest request = new BandOwnerTransferRequest(2L);

        service.transferOwnership(OWNER_ID, BAND_ID, request);

        assertEquals(2L, band.getOwner().getId());
    }

}
