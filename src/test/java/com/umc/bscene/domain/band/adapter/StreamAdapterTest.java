package com.umc.bscene.domain.band.adapter;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.entity.BandMemberProfile;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandMemberType;
import com.umc.bscene.domain.band.enums.BandStatus;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.stream.dto.CoHostCandidateInfo;
import com.umc.bscene.domain.stream.dto.response.BandInfoForGetLiveResponse;
import com.umc.bscene.domain.stream.dto.response.BandSummaryResponse;
import com.umc.bscene.domain.stream.dto.response.LiveMembersResponse.LiveMemberProfileResponse;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.support.StreamFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * band 도메인이 stream 도메인에 제공하는 포트 어댑터(BandMemberPort 구현) 단위 테스트.
 * <p>
 * 라이브 조회/알림 경로에서 쓰이는 배치성 매핑(dedup, 순서 유지, 누락 필터링)이 많아
 * 리팩터링 시 조용히 깨지기 쉬운 지점이다. 반환값 매핑을 구체 값으로 고정한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("band.StreamAdapter (BandMemberPort 구현)")
class StreamAdapterTest {

    private static final Long BAND_ID = 7L;
    private static final Long DEFAULT_OWNER_ID = 999L;

    @Mock
    private BandMemberRepository bandMemberRepository;
    @Mock
    private BandRepository bandRepository;

    private StreamAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new StreamAdapter(bandMemberRepository, bandRepository);
    }

    private static Band band(Long id, String name, String profileImageUrl) {
        return band(id, name, profileImageUrl, DEFAULT_OWNER_ID);
    }

    // 검수 통과 밴드 (Band.builder 기본값은 PENDING - 라이브 활동이 차단됨)
    private static Band band(Long id, String name, String profileImageUrl, Long ownerId) {
        return Band.builder()
                .id(id)
                .owner(StreamFixtures.bandUser(ownerId))
                .name(name)
                .genre(Genre.INDIE)
                .region(Region.SEOUL)
                .profileImageUrl(profileImageUrl)
                .description("description-" + id)
                .status(BandStatus.ACCEPTED)
                .build();
    }

    private static Band pendingBand(Long id, String name) {
        return Band.builder()
                .id(id)
                .owner(StreamFixtures.bandUser(DEFAULT_OWNER_ID))
                .name(name)
                .genre(Genre.INDIE)
                .region(Region.SEOUL)
                .status(BandStatus.PENDING)
                .build();
    }

    private static BandMemberProfile profile(Long id, String nickname, Part part) {
        return BandMemberProfile.builder()
                .id(id)
                .nickname(nickname)
                .part(part)
                .user(StreamFixtures.bandUser(id))
                .active(true)
                .build();
    }

    private static BandMember member(Long id, Band band, BandMemberProfile profile, User user, BandMemberStatus status) {
        return BandMember.builder()
                .id(id)
                .band(band)
                .user(user)
                .bandMemberProfile(profile)
                .status(status)
                .build();
    }

    @Nested
    @DisplayName("getBandNameWithBandProfileByBroadcasterId")
    class GetBandNameWithBandProfileByBroadcasterId {

        @Test
        @DisplayName("broadcasterIds가 비어있으면 리포지토리를 호출하지 않고 빈 리스트를 반환한다")
        void returnsEmptyWithoutRepoCallWhenBroadcasterIdsEmpty() {
            List<BandInfoForGetLiveResponse> result = adapter.getBandNameWithBandProfileByBroadcasterId(Set.of());

            assertThat(result).isEmpty();
            verifyNoInteractions(bandMemberRepository);
        }

        @Test
        @DisplayName("정상 매핑 시 broadcasterId, 밴드명, 밴드 프로필 이미지(null 포함)를 그대로 반환한다")
        void mapsBandInfoIncludingNullProfileImage() {
            BandMember m1 = member(1L, band(BAND_ID, "밴드A", "https://cdn.test/a.jpg"),
                    profile(11L, "닉A", Part.GUITAR), StreamFixtures.bandUser(100L), BandMemberStatus.ACCEPTED);
            BandMember m2 = member(2L, band(8L, "밴드B", null),
                    profile(12L, "닉B", Part.BASS), StreamFixtures.bandUser(200L), BandMemberStatus.ACCEPTED);
            when(bandMemberRepository.findWithBandByUser_IdInAndStatus(Set.of(100L, 200L), BandMemberStatus.ACCEPTED))
                    .thenReturn(List.of(m1, m2));

            List<BandInfoForGetLiveResponse> result =
                    adapter.getBandNameWithBandProfileByBroadcasterId(Set.of(100L, 200L));

            // HashMap 기반 dedup을 거치므로 순서가 아닌 내용으로 검증한다
            assertThat(result).containsExactlyInAnyOrder(
                    new BandInfoForGetLiveResponse(100L, "밴드A", "https://cdn.test/a.jpg"),
                    new BandInfoForGetLiveResponse(200L, "밴드B", null)
            );
        }

        @Test
        @DisplayName("동일 broadcasterId가 중복되면 첫 번째 항목만 유지한다")
        void dedupsByBroadcasterIdKeepingFirst() {
            BandMember first = member(1L, band(BAND_ID, "첫번째밴드", "https://cdn.test/first.jpg"),
                    profile(11L, "닉A", Part.GUITAR), StreamFixtures.bandUser(100L), BandMemberStatus.ACCEPTED);
            BandMember second = member(2L, band(8L, "두번째밴드", "https://cdn.test/second.jpg"),
                    profile(12L, "닉B", Part.BASS), StreamFixtures.bandUser(100L), BandMemberStatus.ACCEPTED);
            when(bandMemberRepository.findWithBandByUser_IdInAndStatus(Set.of(100L), BandMemberStatus.ACCEPTED))
                    .thenReturn(List.of(first, second));

            List<BandInfoForGetLiveResponse> result =
                    adapter.getBandNameWithBandProfileByBroadcasterId(Set.of(100L));

            assertThat(result).containsExactly(
                    new BandInfoForGetLiveResponse(100L, "첫번째밴드", "https://cdn.test/first.jpg")
            );
        }
    }

    @Nested
    @DisplayName("getBandInfoByBandIds")
    class GetBandInfoByBandIds {

        @Test
        @DisplayName("bandIds가 비어있으면 리포지토리를 호출하지 않고 빈 Map을 반환한다")
        void returnsEmptyWithoutRepoCallWhenBandIdsEmpty() {
            Map<Long, BandInfoForGetLiveResponse.BandInfo> result = adapter.getBandInfoByBandIds(Set.of());

            assertThat(result).isEmpty();
            verifyNoInteractions(bandRepository);
        }

        @Test
        @DisplayName("bandId를 key로 밴드명과 프로필 이미지(null 포함)를 매핑하고, 없는 밴드는 결과에서 제외한다")
        void mapsBandInfoByBandIdExcludingMissingBands() {
            when(bandRepository.findAllById(Set.of(7L, 8L, 9L))).thenReturn(List.of(
                    band(7L, "밴드A", "https://cdn.test/a.jpg"),
                    band(8L, "밴드B", null)
            ));

            Map<Long, BandInfoForGetLiveResponse.BandInfo> result =
                    adapter.getBandInfoByBandIds(Set.of(7L, 8L, 9L));

            assertThat(result).containsOnly(
                    Map.entry(7L, new BandInfoForGetLiveResponse.BandInfo("밴드A", "https://cdn.test/a.jpg")),
                    Map.entry(8L, new BandInfoForGetLiveResponse.BandInfo("밴드B", null))
            );
        }
    }

    @Nested
    @DisplayName("getBandSummaryByBroadcasterId")
    class GetBandSummaryByBroadcasterId {

        @Test
        @DisplayName("활성 멤버십이 있으면 밴드 요약 정보를 반환한다")
        void returnsBandSummaryWhenActiveMembershipExists() {
            BandMember bandMember = member(1L, band(BAND_ID, "밴드A", null),
                    profile(11L, "닉A", Part.GUITAR), StreamFixtures.bandUser(100L), BandMemberStatus.ACCEPTED);
            when(bandMemberRepository.findWithBandByUser_IdInAndStatus(Set.of(100L), BandMemberStatus.ACCEPTED))
                    .thenReturn(List.of(bandMember));

            Optional<BandSummaryResponse> result = adapter.getBandSummaryByBroadcasterId(100L);

            assertThat(result).contains(new BandSummaryResponse(BAND_ID, "밴드A"));
        }

        @Test
        @DisplayName("활성 멤버십이 없으면 빈 Optional을 반환한다")
        void returnsEmptyWhenNoActiveMembership() {
            when(bandMemberRepository.findWithBandByUser_IdInAndStatus(Set.of(100L), BandMemberStatus.ACCEPTED))
                    .thenReturn(List.of());

            assertThat(adapter.getBandSummaryByBroadcasterId(100L)).isEmpty();
        }

        @Test
        @DisplayName("검수 중(PENDING) 밴드는 라이브 이력이 생기면 검수 거절이 불가능해지므로 빈 Optional을 반환한다")
        void returnsEmptyWhenBandIsPending() {
            BandMember bandMember = member(1L, pendingBand(BAND_ID, "검수중밴드"),
                    profile(11L, "닉A", Part.GUITAR), StreamFixtures.bandUser(100L), BandMemberStatus.ACCEPTED);
            when(bandMemberRepository.findWithBandByUser_IdInAndStatus(Set.of(100L), BandMemberStatus.ACCEPTED))
                    .thenReturn(List.of(bandMember));

            assertThat(adapter.getBandSummaryByBroadcasterId(100L)).isEmpty();
        }
    }

    @Nested
    @DisplayName("getBandSummaryByBandId")
    class GetBandSummaryByBandId {

        @Test
        @DisplayName("밴드가 존재하면 밴드 요약 정보를 반환한다")
        void returnsBandSummaryWhenBandExists() {
            when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band(BAND_ID, "밴드A", null)));

            assertThat(adapter.getBandSummaryByBandId(BAND_ID)).contains(new BandSummaryResponse(BAND_ID, "밴드A"));
        }

        @Test
        @DisplayName("밴드가 없으면 빈 Optional을 반환한다")
        void returnsEmptyWhenBandMissing() {
            when(bandRepository.findById(BAND_ID)).thenReturn(Optional.empty());

            assertThat(adapter.getBandSummaryByBandId(BAND_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCoHostCandidatesByBandId")
    class GetCoHostCandidatesByBandId {

        @Test
        @DisplayName("밴드 정회원을 공동 진행 후보 정보로 매핑한다")
        void mapsCoHostCandidates() {
            Band band = band(BAND_ID, "밴드A", "https://cdn.test/a.jpg");
            BandMemberProfile profile = profile(11L, "닉A", Part.GUITAR);
            BandMember bandMember = member(21L, band, profile, StreamFixtures.bandUser(100L), BandMemberStatus.ACCEPTED);
            when(bandMemberRepository.findWithUserAndProfileByBand_IdAndStatusAndMemberType(
                            BAND_ID,
                            BandMemberStatus.ACCEPTED,
                            BandMemberType.MEMBER
                    ))
                    .thenReturn(List.of(bandMember));

            List<CoHostCandidateInfo> result = adapter.getCoHostCandidatesByBandId(BAND_ID);

            assertThat(result).containsExactly(
                    new CoHostCandidateInfo(100L, 21L, 11L, "https://cdn.test/a.jpg", "닉A", Part.GUITAR)
            );
        }

        @Test
        @DisplayName("정회원이 없으면 빈 리스트를 반환한다")
        void returnsEmptyWhenNoMembers() {
            when(bandMemberRepository.findWithUserAndProfileByBand_IdAndStatusAndMemberType(
                    BAND_ID,
                    BandMemberStatus.ACCEPTED,
                    BandMemberType.MEMBER
            ))
                    .thenReturn(List.of());

            assertThat(adapter.getCoHostCandidatesByBandId(BAND_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("isActiveRegularMemberOfBand")
    class IsActiveRegularMemberOfBand {

        @Test
        @DisplayName("동일 밴드의 활성 멤버십이면 true를 반환한다")
        void returnsTrueForSameBandActiveMembership() {
            BandMember bandMember = member(1L, band(BAND_ID, "밴드A", null),
                    profile(11L, "닉A", Part.GUITAR), StreamFixtures.bandUser(100L), BandMemberStatus.ACCEPTED);
            when(bandMemberRepository.findWithBandByUser_IdInAndStatus(Set.of(100L), BandMemberStatus.ACCEPTED))
                    .thenReturn(List.of(bandMember));

            assertThat(adapter.isActiveRegularMemberOfBand(BAND_ID, 100L)).isTrue();
        }

        @Test
        @DisplayName("다른 밴드의 활성 멤버십이면 false를 반환한다")
        void returnsFalseForDifferentBandActiveMembership() {
            BandMember bandMember = member(1L, band(8L, "다른밴드", null),
                    profile(11L, "닉A", Part.GUITAR), StreamFixtures.bandUser(100L), BandMemberStatus.ACCEPTED);
            when(bandMemberRepository.findWithBandByUser_IdInAndStatus(Set.of(100L), BandMemberStatus.ACCEPTED))
                    .thenReturn(List.of(bandMember));

            assertThat(adapter.isActiveRegularMemberOfBand(BAND_ID, 100L)).isFalse();
        }

        @Test
        @DisplayName("멤버십이 없으면 false를 반환한다")
        void returnsFalseWhenNoMembership() {
            when(bandMemberRepository.findWithBandByUser_IdInAndStatus(Set.of(100L), BandMemberStatus.ACCEPTED))
                    .thenReturn(List.of());

            assertThat(adapter.isActiveRegularMemberOfBand(BAND_ID, 100L)).isFalse();
        }
    }

    @Nested
    @DisplayName("getLiveMemberProfiles")
    class GetLiveMemberProfiles {

        @Test
        @DisplayName("userIds가 비어있으면 리포지토리를 호출하지 않고 빈 리스트를 반환한다")
        void returnsEmptyWithoutRepoCallWhenUserIdsEmpty() {
            List<LiveMemberProfileResponse> result = adapter.getLiveMemberProfiles(BAND_ID, List.of());

            assertThat(result).isEmpty();
            verifyNoInteractions(bandMemberRepository);
        }

        @Test
        @DisplayName("userIds 순서대로 반환하고 밴드 멤버십이 없는 유저는 결과에서 제외하며 필드를 매핑한다")
        void mapsInUserIdsOrderAndFiltersMissingMembership() {
            Band band = band(BAND_ID, "밴드A", "https://cdn.test/a.jpg");
            BandMemberProfile profileA = profile(11L, "닉A", Part.GUITAR);
            BandMemberProfile profileB = profile(12L, "닉B", Part.BASS);
            BandMember memberA = member(21L, band, profileA, StreamFixtures.bandUser(100L), BandMemberStatus.ACCEPTED);
            BandMember memberB = member(22L, band, profileB, StreamFixtures.bandUser(200L), BandMemberStatus.ACCEPTED);
            // 리포지토리는 순서를 보장하지 않고, userIds 중 300L은 이 밴드의 멤버십 행이 없다
            when(bandMemberRepository.findWithBandAndProfileByBand_IdAndUser_IdIn(BAND_ID, List.of(200L, 100L, 300L)))
                    .thenReturn(List.of(memberA, memberB));

            List<LiveMemberProfileResponse> result =
                    adapter.getLiveMemberProfiles(BAND_ID, List.of(200L, 100L, 300L));

            assertThat(result).containsExactly(
                    new LiveMemberProfileResponse("https://cdn.test/a.jpg", "닉B", "밴드A", List.of(Part.BASS), false),
                    new LiveMemberProfileResponse("https://cdn.test/a.jpg", "닉A", "밴드A", List.of(Part.GUITAR), false)
            );
        }

        @Test
        @DisplayName("동일 유저의 멤버십 행이 중복되면 첫 번째 행을 사용한다")
        void keepsFirstRowOnDuplicateUser() {
            Band band = band(BAND_ID, "밴드A", null);
            BandMemberProfile firstProfile = profile(11L, "첫번째닉", Part.GUITAR);
            BandMemberProfile secondProfile = profile(12L, "두번째닉", Part.BASS);
            BandMember first = member(21L, band, firstProfile, StreamFixtures.bandUser(100L), BandMemberStatus.ACCEPTED);
            BandMember second = member(22L, band, secondProfile, StreamFixtures.bandUser(100L), BandMemberStatus.ACCEPTED);
            when(bandMemberRepository.findWithBandAndProfileByBand_IdAndUser_IdIn(BAND_ID, List.of(100L)))
                    .thenReturn(List.of(first, second));

            List<LiveMemberProfileResponse> result = adapter.getLiveMemberProfiles(BAND_ID, List.of(100L));

            assertThat(result).containsExactly(
                    new LiveMemberProfileResponse(null, "첫번째닉", "밴드A", List.of(Part.GUITAR), false)
            );
        }

        @Test
        @DisplayName("밴드 오너와 멤버의 userId가 같으면 isLeader는 true다")
        void marksLeaderWhenOwnerMatchesMember() {
            Band band = band(BAND_ID, "밴드A", null, 100L);
            BandMemberProfile profile = profile(11L, "닉A", Part.GUITAR);
            BandMember bandMember = member(21L, band, profile, StreamFixtures.bandUser(100L), BandMemberStatus.ACCEPTED);
            when(bandMemberRepository.findWithBandAndProfileByBand_IdAndUser_IdIn(BAND_ID, List.of(100L)))
                    .thenReturn(List.of(bandMember));

            List<LiveMemberProfileResponse> result = adapter.getLiveMemberProfiles(BAND_ID, List.of(100L));

            assertThat(result).extracting(LiveMemberProfileResponse::isLeader).containsExactly(true);
        }

        @Test
        @DisplayName("밴드 오너와 멤버의 userId가 다르면 isLeader는 false다")
        void marksNonLeaderWhenOwnerDiffersFromMember() {
            Band band = band(BAND_ID, "밴드A", null, 999L);
            BandMemberProfile profile = profile(11L, "닉A", Part.GUITAR);
            BandMember bandMember = member(21L, band, profile, StreamFixtures.bandUser(100L), BandMemberStatus.ACCEPTED);
            when(bandMemberRepository.findWithBandAndProfileByBand_IdAndUser_IdIn(BAND_ID, List.of(100L)))
                    .thenReturn(List.of(bandMember));

            List<LiveMemberProfileResponse> result = adapter.getLiveMemberProfiles(BAND_ID, List.of(100L));

            assertThat(result).extracting(LiveMemberProfileResponse::isLeader).containsExactly(false);
        }
    }

    @Nested
    @DisplayName("getAcceptedMemberUserIds")
    class GetAcceptedMemberUserIds {

        @Test
        @DisplayName("중복된 유저 ID가 있으면 distinct 처리한다")
        void distinctsUserIds() {
            Band band = band(BAND_ID, "밴드A", null);
            BandMember m1 = member(1L, band, null, StreamFixtures.bandUser(100L), BandMemberStatus.ACCEPTED);
            BandMember m2 = member(2L, band, null, StreamFixtures.bandUser(100L), BandMemberStatus.ACCEPTED);
            BandMember m3 = member(3L, band, null, StreamFixtures.bandUser(200L), BandMemberStatus.ACCEPTED);
            when(bandMemberRepository.findByBand_IdAndStatus(BAND_ID, BandMemberStatus.ACCEPTED))
                    .thenReturn(List.of(m1, m2, m3));

            assertThat(adapter.getAcceptedMemberUserIds(BAND_ID)).containsExactly(100L, 200L);
        }

        @Test
        @DisplayName("멤버가 없으면 빈 리스트를 반환한다")
        void returnsEmptyWhenNoMembers() {
            when(bandMemberRepository.findByBand_IdAndStatus(BAND_ID, BandMemberStatus.ACCEPTED))
                    .thenReturn(List.of());

            assertThat(adapter.getAcceptedMemberUserIds(BAND_ID)).isEmpty();
        }
    }
}
