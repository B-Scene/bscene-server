package com.umc.bscene.domain.band.adapter;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
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
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.support.StreamFixtures;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * band 도메인이 user 도메인에 제공하는 포트 어댑터(BandPort 구현) 단위 테스트.
 * <p>
 * 도메인 경계의 이음새라 성능 튜닝(배치 조회 전환) 시 회귀가 나기 쉬운 지점이다.
 * 따라서 반환값 매핑뿐 아니라 "호출 형태"(어떤 리포지토리를, 몇 번, 어떤 인자로 호출하는지)까지 고정한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("band.UserAdapter (BandPort 구현)")
class UserAdapterTest {

    private static final Long USER_ID = 100L;
    private static final Long BAND_ID = 7L;

    @Mock
    private BandMemberProfileRepository bandMemberProfileRepository;
    @Mock
    private BandMemberRepository bandMemberRepository;
    @Mock
    private BandRepository bandRepository;
    @Mock
    private FollowPort followPort;
    @Mock
    private SessionPort sessionPort;
    @Mock
    private PerformancePort performancePort;

    private UserAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserAdapter(
                bandMemberProfileRepository,
                bandMemberRepository,
                bandRepository,
                followPort,
                sessionPort,
                performancePort
        );
    }

    private static Band band(Long id, String name, String profileImageUrl) {
        return Band.builder()
                .id(id)
                .owner(StreamFixtures.bandUser(999L))
                .name(name)
                .genre(Genre.INDIE)
                .region(Region.SEOUL)
                .profileImageUrl(profileImageUrl)
                .description("description-" + id)
                .build();
    }

    private static BandMemberProfile profile(Long id, String nickname, Part part, boolean active) {
        return BandMemberProfile.builder()
                .id(id)
                .nickname(nickname)
                .part(part)
                .user(StreamFixtures.bandUser(USER_ID))
                .active(active)
                .build();
    }

    private static BandMember member(Long id, Band band, BandMemberProfile profile,
                                     BandMemberStatus status, BandMemberType memberType) {
        return BandMember.builder()
                .id(id)
                .band(band)
                .user(StreamFixtures.bandUser(USER_ID))
                .bandMemberProfile(profile)
                .status(status)
                .memberType(memberType)
                .build();
    }

    private static void assertBandError(ThrowingCallable callable, BandErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BandException.class)
                .extracting(thrown -> ((BandException) thrown).getBaseResponseCode())
                .isEqualTo(expected);
    }

    @Nested
    @DisplayName("validateActiveBandMember")
    class ValidateActiveBandMember {

        @Test
        @DisplayName("ACCEPTED 정회원이고 활성 프로필이면 통과하며, 멤버 조회는 (bandId, userId, ACCEPTED)로 1회만 한다")
        void passesForActiveMember() {
            BandMember bandMember = member(1L, band(BAND_ID, "밴드A", null),
                    profile(11L, "닉A", Part.GUITAR, true),
                    BandMemberStatus.ACCEPTED, BandMemberType.MEMBER);
            when(bandMemberRepository.findByBand_IdAndUser_IdAndStatus(BAND_ID, USER_ID, BandMemberStatus.ACCEPTED))
                    .thenReturn(Optional.of(bandMember));

            assertThatCode(() -> adapter.validateActiveBandMember(USER_ID, BAND_ID))
                    .doesNotThrowAnyException();

            // 인자 순서(bandId, userId)가 뒤바뀌면 조용히 다른 행을 보게 되므로 순서까지 고정
            verify(bandMemberRepository, times(1))
                    .findByBand_IdAndUser_IdAndStatus(BAND_ID, USER_ID, BandMemberStatus.ACCEPTED);
            verifyNoInteractions(bandMemberProfileRepository, bandRepository, followPort, sessionPort, performancePort);
        }

        @Test
        @DisplayName("소속이 없으면 BAND_PERMISSION_DENIED")
        void rejectsNonMember() {
            when(bandMemberRepository.findByBand_IdAndUser_IdAndStatus(BAND_ID, USER_ID, BandMemberStatus.ACCEPTED))
                    .thenReturn(Optional.empty());

            assertBandError(() -> adapter.validateActiveBandMember(USER_ID, BAND_ID),
                    BandErrorCode.BAND_PERMISSION_DENIED);
        }

        @Test
        @DisplayName("세션 멤버(memberType=SESSION)면 BAND_PERMISSION_DENIED")
        void rejectsSessionMember() {
            BandMember bandMember = member(1L, band(BAND_ID, "밴드A", null),
                    profile(11L, "닉A", Part.GUITAR, true),
                    BandMemberStatus.ACCEPTED, BandMemberType.SESSION);
            when(bandMemberRepository.findByBand_IdAndUser_IdAndStatus(BAND_ID, USER_ID, BandMemberStatus.ACCEPTED))
                    .thenReturn(Optional.of(bandMember));

            assertBandError(() -> adapter.validateActiveBandMember(USER_ID, BAND_ID),
                    BandErrorCode.BAND_PERMISSION_DENIED);
        }

        @Test
        @DisplayName("멤버 프로필이 없으면 BAND_MODE_REQUIRED")
        void rejectsWhenProfileMissing() {
            BandMember bandMember = member(1L, band(BAND_ID, "밴드A", null), null,
                    BandMemberStatus.ACCEPTED, BandMemberType.MEMBER);
            when(bandMemberRepository.findByBand_IdAndUser_IdAndStatus(BAND_ID, USER_ID, BandMemberStatus.ACCEPTED))
                    .thenReturn(Optional.of(bandMember));

            assertBandError(() -> adapter.validateActiveBandMember(USER_ID, BAND_ID),
                    BandErrorCode.BAND_MODE_REQUIRED);
        }

        @Test
        @DisplayName("프로필이 비활성(active=false)이면 BAND_MODE_REQUIRED")
        void rejectsWhenProfileInactive() {
            BandMember bandMember = member(1L, band(BAND_ID, "밴드A", null),
                    profile(11L, "닉A", Part.GUITAR, false),
                    BandMemberStatus.ACCEPTED, BandMemberType.MEMBER);
            when(bandMemberRepository.findByBand_IdAndUser_IdAndStatus(BAND_ID, USER_ID, BandMemberStatus.ACCEPTED))
                    .thenReturn(Optional.of(bandMember));

            assertBandError(() -> adapter.validateActiveBandMember(USER_ID, BAND_ID),
                    BandErrorCode.BAND_MODE_REQUIRED);
        }
    }

    @Nested
    @DisplayName("registerSessionMember")
    class RegisterSessionMember {

        @Test
        @DisplayName("프로필과 멤버를 각각 1회 저장하고, 프로필은 지원서의 닉네임·파트로 pre-fill 되며 active는 false다")
        void savesProfileAndMember() {
            User applicant = StreamFixtures.bandUser(USER_ID);
            Band band = band(BAND_ID, "밴드A", "https://cdn.test/band.jpg");
            when(bandMemberRepository.existsByBand_IdAndUser_Id(BAND_ID, USER_ID)).thenReturn(false);
            when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band));
            when(bandMemberProfileRepository.save(any(BandMemberProfile.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            adapter.registerSessionMember(BAND_ID, applicant, "지원닉", Part.BASS);

            ArgumentCaptor<BandMemberProfile> savedProfile = ArgumentCaptor.forClass(BandMemberProfile.class);
            verify(bandMemberProfileRepository, times(1)).save(savedProfile.capture());
            assertThat(savedProfile.getValue().getNickname()).isEqualTo("지원닉");
            assertThat(savedProfile.getValue().getPart()).isEqualTo(Part.BASS);
            assertThat(savedProfile.getValue().getUser()).isSameAs(applicant);
            assertThat(savedProfile.getValue().getActive()).isFalse();

            ArgumentCaptor<BandMember> savedMember = ArgumentCaptor.forClass(BandMember.class);
            verify(bandMemberRepository, times(1)).save(savedMember.capture());
            assertThat(savedMember.getValue().getBand()).isSameAs(band);
            assertThat(savedMember.getValue().getUser()).isSameAs(applicant);
            // 저장된 프로필 인스턴스가 그대로 멤버에 연결되어야 한다
            assertThat(savedMember.getValue().getBandMemberProfile()).isSameAs(savedProfile.getValue());
            assertThat(savedMember.getValue().getStatus()).isEqualTo(BandMemberStatus.ACCEPTED);
            assertThat(savedMember.getValue().getMemberType()).isEqualTo(BandMemberType.SESSION);

            verify(bandRepository, times(1)).findById(BAND_ID);
        }

        @Test
        @DisplayName("이미 소속된 유저면 아무 것도 저장하지 않고 즉시 반환한다")
        void shortCircuitsWhenAlreadyMember() {
            User applicant = StreamFixtures.bandUser(USER_ID);
            when(bandMemberRepository.existsByBand_IdAndUser_Id(BAND_ID, USER_ID)).thenReturn(true);

            adapter.registerSessionMember(BAND_ID, applicant, "지원닉", Part.BASS);

            verify(bandMemberRepository, never()).save(any());
            verifyNoInteractions(bandRepository, bandMemberProfileRepository);
        }

        @Test
        @DisplayName("밴드가 없으면 BAND_NOT_FOUND이고 프로필·멤버 모두 저장하지 않는다")
        void failsWhenBandMissing() {
            User applicant = StreamFixtures.bandUser(USER_ID);
            when(bandMemberRepository.existsByBand_IdAndUser_Id(BAND_ID, USER_ID)).thenReturn(false);
            when(bandRepository.findById(BAND_ID)).thenReturn(Optional.empty());

            assertBandError(() -> adapter.registerSessionMember(BAND_ID, applicant, "지원닉", Part.BASS),
                    BandErrorCode.BAND_NOT_FOUND);

            verify(bandMemberProfileRepository, never()).save(any());
            verify(bandMemberRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getActiveBandMemberProfile")
    class GetActiveBandMemberProfile {

        @Test
        @DisplayName("활성 프로필과 소속 밴드를 조합해 모든 필드를 매핑한다")
        void mapsAllFields() {
            BandMemberProfile profile = profile(11L, "닉A", Part.DRUM, true);
            Band band = band(BAND_ID, "밴드A", "https://cdn.test/band.jpg");
            BandMember bandMember = member(1L, band, profile, BandMemberStatus.ACCEPTED, BandMemberType.MEMBER);
            when(bandMemberProfileRepository.findByUser_IdAndActiveTrue(USER_ID)).thenReturn(Optional.of(profile));
            when(bandMemberRepository.findWithBandByUser_IdInAndStatus(any(), any()))
                    .thenReturn(List.of(bandMember));
            when(followPort.countFollowersByBandId(BAND_ID)).thenReturn(12L);
            when(sessionPort.getActiveSessionApplicantCount(BAND_ID)).thenReturn(3L);
            when(performancePort.countPerformancesByBandIdAndStatus(BAND_ID)).thenReturn(5L);

            BandMemberResponse response = adapter.getActiveBandMemberProfile(USER_ID);

            assertThat(response.bandMemberProfileId()).isEqualTo(11L);
            assertThat(response.profileImageUrl()).isEqualTo("https://cdn.test/band.jpg");
            assertThat(response.nickname()).isEqualTo("닉A");
            assertThat(response.bandName()).isEqualTo("밴드A");
            // parts는 Part enum의 한글 description 한 건짜리 리스트
            assertThat(response.parts()).containsExactly("드럼");
            assertThat(response.follower()).isEqualTo(12);
            assertThat(response.applicant()).isEqualTo(3);
            assertThat(response.performance()).isEqualTo(5);
            assertThat(response.isBandMember()).isTrue();
        }

        @Test
        @DisplayName("멤버 조회는 userId 하나짜리 컬렉션으로 1회만 하고, 여러 건이 오면 첫 번째 밴드를 쓴다")
        void queriesOnceAndUsesFirstMember() {
            BandMemberProfile profile = profile(11L, "닉A", Part.VOCAL, true);
            Band first = band(BAND_ID, "첫번째밴드", "https://cdn.test/first.jpg");
            Band second = band(8L, "두번째밴드", "https://cdn.test/second.jpg");
            when(bandMemberProfileRepository.findByUser_IdAndActiveTrue(USER_ID)).thenReturn(Optional.of(profile));
            when(bandMemberRepository.findWithBandByUser_IdInAndStatus(any(), any()))
                    .thenReturn(List.of(
                            member(1L, first, profile, BandMemberStatus.ACCEPTED, BandMemberType.MEMBER),
                            member(2L, second, profile, BandMemberStatus.ACCEPTED, BandMemberType.SESSION)
                    ));
            when(followPort.countFollowersByBandId(BAND_ID)).thenReturn(1L);
            when(sessionPort.getActiveSessionApplicantCount(BAND_ID)).thenReturn(1L);
            when(performancePort.countPerformancesByBandIdAndStatus(BAND_ID)).thenReturn(1L);

            BandMemberResponse response = adapter.getActiveBandMemberProfile(USER_ID);

            assertThat(response.bandName()).isEqualTo("첫번째밴드");
            assertThat(response.isBandMember()).isTrue();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<Long>> userIds = ArgumentCaptor.forClass(Collection.class);
            ArgumentCaptor<BandMemberStatus> status = ArgumentCaptor.forClass(BandMemberStatus.class);
            verify(bandMemberRepository, times(1))
                    .findWithBandByUser_IdInAndStatus(userIds.capture(), status.capture());
            assertThat(userIds.getValue()).containsExactly(USER_ID);
            assertThat(status.getValue()).isEqualTo(BandMemberStatus.ACCEPTED);

            // 각 밴드마다 카운트를 다시 조회하지 않는다 (첫 번째 밴드에 대해서만 1회씩)
            verify(followPort, times(1)).countFollowersByBandId(BAND_ID);
            verify(sessionPort, times(1)).getActiveSessionApplicantCount(BAND_ID);
            verify(performancePort, times(1)).countPerformancesByBandIdAndStatus(BAND_ID);
        }

        @Test
        @DisplayName("밴드 프로필 이미지가 null이면 빈 문자열이 아니라 null 그대로 내려간다")
        void keepsNullProfileImageUrl() {
            BandMemberProfile profile = profile(11L, "닉A", Part.KEYBOARD, true);
            Band band = band(BAND_ID, "밴드A", null);
            when(bandMemberProfileRepository.findByUser_IdAndActiveTrue(USER_ID)).thenReturn(Optional.of(profile));
            when(bandMemberRepository.findWithBandByUser_IdInAndStatus(any(), any()))
                    .thenReturn(List.of(member(1L, band, profile, BandMemberStatus.ACCEPTED, BandMemberType.MEMBER)));
            when(followPort.countFollowersByBandId(BAND_ID)).thenReturn(0L);
            when(sessionPort.getActiveSessionApplicantCount(BAND_ID)).thenReturn(0L);
            when(performancePort.countPerformancesByBandIdAndStatus(BAND_ID)).thenReturn(0L);

            BandMemberResponse response = adapter.getActiveBandMemberProfile(USER_ID);

            assertThat(response.profileImageUrl()).isNull();
            assertThat(response.bandName()).isEqualTo("밴드A");
        }

        @Test
        @DisplayName("멤버에 밴드가 연결되어 있지 않으면 밴드 관련 필드는 모두 null이고 카운트 포트를 호출하지 않는다")
        void mapsNullBandBranch() {
            BandMemberProfile profile = profile(11L, "닉A", Part.ETC, true);
            when(bandMemberProfileRepository.findByUser_IdAndActiveTrue(USER_ID)).thenReturn(Optional.of(profile));
            when(bandMemberRepository.findWithBandByUser_IdInAndStatus(any(), any()))
                    .thenReturn(List.of(member(1L, null, profile, BandMemberStatus.ACCEPTED, BandMemberType.SESSION)));

            BandMemberResponse response = adapter.getActiveBandMemberProfile(USER_ID);

            assertThat(response.bandMemberProfileId()).isEqualTo(11L);
            assertThat(response.nickname()).isEqualTo("닉A");
            assertThat(response.parts()).containsExactly("etc");
            assertThat(response.profileImageUrl()).isNull();
            assertThat(response.bandName()).isNull();
            assertThat(response.follower()).isNull();
            assertThat(response.applicant()).isNull();
            assertThat(response.performance()).isNull();
            assertThat(response.isBandMember()).isFalse();

            verifyNoInteractions(followPort, sessionPort, performancePort);
        }

        @Test
        @DisplayName("활성 프로필이 없으면 BAND_MEMBER_PROFILE_NOT_FOUND이고 멤버 조회는 하지 않는다")
        void failsWhenActiveProfileMissing() {
            when(bandMemberProfileRepository.findByUser_IdAndActiveTrue(USER_ID)).thenReturn(Optional.empty());

            assertBandError(() -> adapter.getActiveBandMemberProfile(USER_ID),
                    BandErrorCode.BAND_MEMBER_PROFILE_NOT_FOUND);

            verifyNoInteractions(bandMemberRepository, bandRepository, followPort, sessionPort, performancePort);
        }

        @Test
        @DisplayName("소속 밴드 멤버가 한 건도 없으면 도메인 예외가 아니라 assert/NPE로 터진다 (현재 구현 그대로 고정)")
        void blowsUpWhenNoBandMember() {
            BandMemberProfile profile = profile(11L, "닉A", Part.GUITAR, true);
            when(bandMemberProfileRepository.findByUser_IdAndActiveTrue(USER_ID)).thenReturn(Optional.of(profile));
            when(bandMemberRepository.findWithBandByUser_IdInAndStatus(any(), any())).thenReturn(List.of());

            // 구현이 orElse(null) 후 `assert bandMember != null`로 처리한다.
            // -ea가 켜진 실행(Gradle 기본)에서는 AssertionError, 꺼져 있으면 NPE가 된다.
            assertThatThrownBy(() -> adapter.getActiveBandMemberProfile(USER_ID))
                    .isInstanceOfAny(AssertionError.class, NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("getActiveBandMemberProfile_BandIdIdByUserId")
    class GetActiveBandId {

        @Test
        @DisplayName("활성 프로필로 활동 중인 정회원의 밴드 ID를 반환한다")
        void returnsBandId() {
            BandMember bandMember = member(1L, band(BAND_ID, "밴드A", null),
                    profile(11L, "닉A", Part.GUITAR, true),
                    BandMemberStatus.ACCEPTED, BandMemberType.MEMBER);
            when(bandMemberRepository.findWithBandByUser_IdAndActiveProfile(USER_ID, true, BandMemberStatus.ACCEPTED))
                    .thenReturn(Optional.of(bandMember));

            assertThat(adapter.getActiveBandMemberProfile_BandIdIdByUserId(USER_ID)).isEqualTo(BAND_ID);

            verify(bandMemberRepository, times(1))
                    .findWithBandByUser_IdAndActiveProfile(USER_ID, true, BandMemberStatus.ACCEPTED);
        }

        @Test
        @DisplayName("활성 프로필 기반 소속이 없으면 BAND_MEMBER_NOT_FOUND")
        void failsWhenMemberMissing() {
            when(bandMemberRepository.findWithBandByUser_IdAndActiveProfile(USER_ID, true, BandMemberStatus.ACCEPTED))
                    .thenReturn(Optional.empty());

            assertBandError(() -> adapter.getActiveBandMemberProfile_BandIdIdByUserId(USER_ID),
                    BandErrorCode.BAND_MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("세션 멤버면 BAND_PERMISSION_DENIED")
        void failsForSessionMember() {
            BandMember bandMember = member(1L, band(BAND_ID, "밴드A", null),
                    profile(11L, "닉A", Part.GUITAR, true),
                    BandMemberStatus.ACCEPTED, BandMemberType.SESSION);
            when(bandMemberRepository.findWithBandByUser_IdAndActiveProfile(USER_ID, true, BandMemberStatus.ACCEPTED))
                    .thenReturn(Optional.of(bandMember));

            assertBandError(() -> adapter.getActiveBandMemberProfile_BandIdIdByUserId(USER_ID),
                    BandErrorCode.BAND_PERMISSION_DENIED);
        }
    }

    @Nested
    @DisplayName("getAssociatedBandProfiles")
    class GetAssociatedBandProfiles {

        @Test
        @DisplayName("ACCEPTED 조건으로 1회 조회한 결과를 순서 그대로 위임 반환한다")
        void delegatesToRepository() {
            MyBandProfile first = new MyBandProfile(1L, 11L, "https://cdn.test/a.jpg", "밴드A",
                    Genre.INDIE, Region.SEOUL, true);
            MyBandProfile second = new MyBandProfile(2L, 12L, null, "밴드B",
                    Genre.JAZZ, Region.BUSAN, false);
            when(bandMemberRepository.getMyBandProfiles(USER_ID, BandMemberStatus.ACCEPTED))
                    .thenReturn(List.of(first, second));

            assertThat(adapter.getAssociatedBandProfiles(USER_ID)).containsExactly(first, second);

            verify(bandMemberRepository, times(1)).getMyBandProfiles(USER_ID, BandMemberStatus.ACCEPTED);
            verifyNoInteractions(bandMemberProfileRepository, bandRepository, followPort, sessionPort, performancePort);
        }
    }

    @Nested
    @DisplayName("changeProfileByProfileId")
    class ChangeProfileByProfileId {

        @Test
        @DisplayName("기존 활성 프로필을 비활성화하고 대상 프로필을 활성화한다")
        void swapsActiveProfile() {
            BandMemberProfile target = profile(12L, "닉B", Part.BASS, false);
            BandMemberProfile currentlyActive = profile(11L, "닉A", Part.GUITAR, true);
            when(bandMemberProfileRepository.findByIdAndUser_Id(12L, USER_ID)).thenReturn(Optional.of(target));
            when(bandMemberProfileRepository.findByUser_IdAndActiveTrue(USER_ID))
                    .thenReturn(Optional.of(currentlyActive));

            adapter.changeProfileByProfileId(USER_ID, 12L);

            assertThat(currentlyActive.getActive()).isFalse();
            assertThat(target.getActive()).isTrue();
            // 소유권 검증 쿼리는 (profileId, userId) 순서로 정확히 1회
            verify(bandMemberProfileRepository, times(1)).findByIdAndUser_Id(12L, USER_ID);
            verify(bandMemberProfileRepository, times(1)).findByUser_IdAndActiveTrue(USER_ID);
        }

        @Test
        @DisplayName("기존 활성 프로필이 없어도 대상 프로필만 활성화한다")
        void activatesWhenNoCurrentActive() {
            BandMemberProfile target = profile(12L, "닉B", Part.BASS, false);
            when(bandMemberProfileRepository.findByIdAndUser_Id(12L, USER_ID)).thenReturn(Optional.of(target));
            when(bandMemberProfileRepository.findByUser_IdAndActiveTrue(USER_ID)).thenReturn(Optional.empty());

            adapter.changeProfileByProfileId(USER_ID, 12L);

            assertThat(target.getActive()).isTrue();
        }

        @Test
        @DisplayName("이미 활성인 프로필을 다시 지정해도 최종 상태는 활성이다")
        void staysActiveWhenSameProfile() {
            BandMemberProfile target = profile(12L, "닉B", Part.BASS, true);
            when(bandMemberProfileRepository.findByIdAndUser_Id(12L, USER_ID)).thenReturn(Optional.of(target));
            when(bandMemberProfileRepository.findByUser_IdAndActiveTrue(USER_ID)).thenReturn(Optional.of(target));

            adapter.changeProfileByProfileId(USER_ID, 12L);

            assertThat(target.getActive()).isTrue();
        }

        @Test
        @DisplayName("본인 소유가 아니면 PROFILE_ACTIVATION_FORBIDDEN이고 기존 활성 프로필은 건드리지 않는다")
        void failsWhenNotOwned() {
            when(bandMemberProfileRepository.findByIdAndUser_Id(12L, USER_ID)).thenReturn(Optional.empty());

            assertBandError(() -> adapter.changeProfileByProfileId(USER_ID, 12L),
                    BandErrorCode.PROFILE_ACTIVATION_FORBIDDEN);

            verify(bandMemberProfileRepository, never()).findByUser_IdAndActiveTrue(any());
        }
    }

    @Nested
    @DisplayName("deactivateCurrentActiveProfile")
    class DeactivateCurrentActiveProfile {

        @Test
        @DisplayName("활성 프로필이 있으면 비활성화한다")
        void deactivatesActiveProfile() {
            BandMemberProfile active = profile(11L, "닉A", Part.GUITAR, true);
            when(bandMemberProfileRepository.findByUser_IdAndActiveTrue(USER_ID)).thenReturn(Optional.of(active));

            adapter.deactivateCurrentActiveProfile(USER_ID);

            assertThat(active.getActive()).isFalse();
            verify(bandMemberProfileRepository, times(1)).findByUser_IdAndActiveTrue(USER_ID);
        }

        @Test
        @DisplayName("활성 프로필이 없으면 예외 없이 아무 일도 하지 않는다")
        void doesNothingWhenNoActiveProfile() {
            when(bandMemberProfileRepository.findByUser_IdAndActiveTrue(USER_ID)).thenReturn(Optional.empty());

            assertThatCode(() -> adapter.deactivateCurrentActiveProfile(USER_ID)).doesNotThrowAnyException();

            verifyNoInteractions(bandMemberRepository, bandRepository);
        }
    }

    @Nested
    @DisplayName("getAcceptedMemberUserIds")
    class GetAcceptedMemberUserIds {

        @Test
        @DisplayName("ACCEPTED 조건으로 1회 조회하고 결과 순서를 유지한다")
        void delegatesToRepository() {
            when(bandMemberRepository.findUserIdsByBandIdAndStatus(BAND_ID, BandMemberStatus.ACCEPTED))
                    .thenReturn(List.of(101L, 102L, 103L));

            assertThat(adapter.getAcceptedMemberUserIds(BAND_ID)).containsExactly(101L, 102L, 103L);

            verify(bandMemberRepository, times(1))
                    .findUserIdsByBandIdAndStatus(BAND_ID, BandMemberStatus.ACCEPTED);
        }

        @Test
        @DisplayName("멤버가 없으면 빈 리스트를 그대로 반환한다")
        void returnsEmptyList() {
            when(bandMemberRepository.findUserIdsByBandIdAndStatus(BAND_ID, BandMemberStatus.ACCEPTED))
                    .thenReturn(List.of());

            assertThat(adapter.getAcceptedMemberUserIds(BAND_ID)).isEmpty();
        }
    }
}
