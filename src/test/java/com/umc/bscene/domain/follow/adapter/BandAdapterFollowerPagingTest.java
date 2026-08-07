package com.umc.bscene.domain.follow.adapter;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.dto.FollowerBlock;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.entity.BandMemberProfile;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandMemberType;
import com.umc.bscene.domain.band.repository.BandMemberProfileRepository;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.follow.entity.Follow;
import com.umc.bscene.domain.follow.repository.FollowRepository;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.user.entity.FanProfile;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.Gender;
import com.umc.bscene.domain.user.repository.FanProfileRepository;
import com.umc.bscene.domain.user.repository.UserRepository;
import com.umc.bscene.global.config.JpaAuditingConfig;
import com.umc.bscene.global.response.CursorPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 밴드모드 팔로워 커서 페이징의 리포지토리 쿼리와 어댑터 페이징 로직 검증.
 * <p>
 * - BandRepository.findBandIdsByActiveProfile : 활성 프로필 + 수락된 정식 멤버 조건 필터링
 * - BandAdapter.findPagedMyBandFollowers : size+1 조회 기반 hasNext/nextCursor 계산, FanProfile left join
 */
@DataJpaTest
// 기본 내장 DB로 교체하면 test/resources/application.yml의 NON_KEYWORDS=USER(H2 예약어 User 테이블 허용) 설정이 무시되므로 교체 비활성화
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
@DisplayName("밴드 팔로워 커서 페이징")
class BandAdapterFollowerPagingTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BandRepository bandRepository;
    @Autowired
    private BandMemberRepository bandMemberRepository;
    @Autowired
    private BandMemberProfileRepository bandMemberProfileRepository;
    @Autowired
    private FanProfileRepository fanProfileRepository;
    @Autowired
    private FollowRepository followRepository;

    private BandAdapter bandAdapter;

    private int sequence = 0;

    @BeforeEach
    void setUp() {
        bandAdapter = new BandAdapter(followRepository);
    }

    private User saveUser() {
        sequence++;
        return userRepository.save(User.builder()
                .name("유저" + sequence)
                .birthDate(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .phone(String.format("010%08d", sequence))
                .build());
    }

    private Band saveBand(User owner) {
        sequence++;
        return bandRepository.save(Band.builder()
                .owner(owner)
                .name("밴드" + sequence)
                .genre(Genre.INDIE)
                .region(Region.SEOUL)
                .build());
    }

    private BandMemberProfile saveMemberProfile(User user, boolean active) {
        sequence++;
        return bandMemberProfileRepository.save(BandMemberProfile.builder()
                .user(user)
                .nickname("멤버프로필" + sequence)
                .part(Part.VOCAL)
                .active(active)
                .build());
    }

    private BandMember saveBandMember(
            Band band,
            User user,
            BandMemberProfile profile,
            BandMemberStatus status,
            BandMemberType memberType
    ) {
        return bandMemberRepository.save(BandMember.builder()
                .band(band)
                .user(user)
                .bandMemberProfile(profile)
                .status(status)
                .memberType(memberType)
                .build());
    }

    private FanProfile saveFanProfile(User user, String nickname, String imageUrl) {
        return fanProfileRepository.save(FanProfile.builder()
                .user(user)
                .nickname(nickname)
                .profileImageUrl(imageUrl)
                .build());
    }

    private Follow saveFollow(Band band, User user) {
        return followRepository.save(Follow.builder()
                .band(band)
                .user(user)
                .build());
    }

    @Nested
    @DisplayName("BandRepository.findBandIdsByActiveProfile")
    class FindBandIdsByActiveProfile {

        @Test
        @DisplayName("활성 프로필로 수락된 정식 멤버면 소속 밴드 id를 반환한다")
        void returnsBandOfActiveProfile() {
            User user = saveUser();
            Band band = saveBand(user);
            BandMemberProfile activeProfile = saveMemberProfile(user, true);
            saveBandMember(band, user, activeProfile, BandMemberStatus.ACCEPTED, BandMemberType.MEMBER);

            List<Long> bandIds = bandRepository.findBandIdsByActiveProfile(
                    user.getId(), BandMemberType.MEMBER, BandMemberStatus.ACCEPTED);

            assertThat(bandIds).containsExactly(band.getId());
        }

        @Test
        @DisplayName("비활성 프로필로 소속된 밴드는 반환하지 않는다")
        void excludesInactiveProfile() {
            User user = saveUser();
            Band band = saveBand(user);
            BandMemberProfile inactiveProfile = saveMemberProfile(user, false);
            saveBandMember(band, user, inactiveProfile, BandMemberStatus.ACCEPTED, BandMemberType.MEMBER);

            List<Long> bandIds = bandRepository.findBandIdsByActiveProfile(
                    user.getId(), BandMemberType.MEMBER, BandMemberStatus.ACCEPTED);

            assertThat(bandIds).isEmpty();
        }

        @Test
        @DisplayName("아직 수락하지 않은(INVITED) 밴드는 반환하지 않는다")
        void excludesInvitedMember() {
            User user = saveUser();
            Band band = saveBand(user);
            BandMemberProfile activeProfile = saveMemberProfile(user, true);
            saveBandMember(band, user, activeProfile, BandMemberStatus.INVITED, BandMemberType.MEMBER);

            List<Long> bandIds = bandRepository.findBandIdsByActiveProfile(
                    user.getId(), BandMemberType.MEMBER, BandMemberStatus.ACCEPTED);

            assertThat(bandIds).isEmpty();
        }

        @Test
        @DisplayName("세션(SESSION) 멤버로 소속된 밴드는 반환하지 않는다")
        void excludesSessionMember() {
            User user = saveUser();
            Band band = saveBand(user);
            BandMemberProfile activeProfile = saveMemberProfile(user, true);
            saveBandMember(band, user, activeProfile, BandMemberStatus.ACCEPTED, BandMemberType.SESSION);

            List<Long> bandIds = bandRepository.findBandIdsByActiveProfile(
                    user.getId(), BandMemberType.MEMBER, BandMemberStatus.ACCEPTED);

            assertThat(bandIds).isEmpty();
        }

        @Test
        @DisplayName("같은 활성 프로필로 여러 밴드에 소속되면 먼저 가입한 밴드가 앞에 온다")
        void ordersByMembershipCreation() {
            User user = saveUser();
            Band firstBand = saveBand(user);
            Band secondBand = saveBand(user);
            BandMemberProfile activeProfile = saveMemberProfile(user, true);
            saveBandMember(firstBand, user, activeProfile, BandMemberStatus.ACCEPTED, BandMemberType.MEMBER);
            saveBandMember(secondBand, user, activeProfile, BandMemberStatus.ACCEPTED, BandMemberType.MEMBER);

            List<Long> bandIds = bandRepository.findBandIdsByActiveProfile(
                    user.getId(), BandMemberType.MEMBER, BandMemberStatus.ACCEPTED);

            assertThat(bandIds).containsExactly(firstBand.getId(), secondBand.getId());
        }
    }

    @Nested
    @DisplayName("BandAdapter.findPagedMyBandFollowers")
    class FindPagedMyBandFollowers {

        @Test
        @DisplayName("첫 페이지는 최신 팔로우 순으로 size개를 반환하고 마지막 항목의 팔로우 id를 커서로 준다")
        void firstPageReturnsLatestFollowsWithCursor() {
            Band band = saveBand(saveUser());

            User fan1 = saveUser();
            User fan2 = saveUser();
            User fan3 = saveUser();
            saveFanProfile(fan1, "팬1", "url1");
            saveFanProfile(fan2, "팬2", "url2");
            saveFanProfile(fan3, "팬3", "url3");
            saveFollow(band, fan1);
            saveFollow(band, fan2);
            Follow thirdFollow = saveFollow(band, fan3);
            saveFollow(band, saveUser());
            Follow latestFollow = saveFollow(band, saveUser());

            CursorPage<FollowerBlock> page = bandAdapter.findPagedMyBandFollowers(band.getId(), null, 3);

            assertThat(page.getItems()).hasSize(3);
            assertThat(page.getItems().getFirst().userId()).isEqualTo(latestFollow.getUser().getId());
            assertThat(page.getItems().getLast().userId()).isEqualTo(fan3.getId());
            assertThat(page.getPageInfo().hasNext()).isTrue();
            assertThat(page.getPageInfo().nextCursor()).isEqualTo(thirdFollow.getId());
        }

        @Test
        @DisplayName("커서를 넘기면 그 이전 팔로우만 반환하고 마지막 페이지면 커서가 null이다")
        void secondPageUsesCursorAndEndsPaging() {
            Band band = saveBand(saveUser());

            User fan1 = saveUser();
            User fan2 = saveUser();
            saveFanProfile(fan1, "팬1", "url1");
            saveFanProfile(fan2, "팬2", "url2");
            saveFollow(band, fan1);
            saveFollow(band, fan2);
            Follow cursorFollow = saveFollow(band, saveUser());

            CursorPage<FollowerBlock> page = bandAdapter.findPagedMyBandFollowers(
                    band.getId(), cursorFollow.getId(), 3);

            assertThat(page.getItems())
                    .extracting(FollowerBlock::userId)
                    .containsExactly(fan2.getId(), fan1.getId());
            assertThat(page.getPageInfo().hasNext()).isFalse();
            assertThat(page.getPageInfo().nextCursor()).isNull();
        }

        @Test
        @DisplayName("팔로워의 팬 프로필 닉네임과 이미지를 함께 반환한다")
        void includesFanProfileFields() {
            Band band = saveBand(saveUser());
            User fan = saveUser();
            saveFanProfile(fan, "락덕후", "https://img.example.com/fan.png");
            saveFollow(band, fan);

            CursorPage<FollowerBlock> page = bandAdapter.findPagedMyBandFollowers(band.getId(), null, 10);

            assertThat(page.getItems()).containsExactly(
                    new FollowerBlock(fan.getId(), "https://img.example.com/fan.png", "락덕후"));
        }

        @Test
        @DisplayName("팬 프로필이 없는 팔로워도 누락되지 않고 닉네임/이미지는 null로 반환한다")
        void keepsFollowerWithoutFanProfile() {
            Band band = saveBand(saveUser());
            User fanWithoutProfile = saveUser();
            saveFollow(band, fanWithoutProfile);

            CursorPage<FollowerBlock> page = bandAdapter.findPagedMyBandFollowers(band.getId(), null, 10);

            assertThat(page.getItems()).containsExactly(
                    new FollowerBlock(fanWithoutProfile.getId(), null, null));
        }

        @Test
        @DisplayName("팔로워 수가 size와 같으면 다음 페이지가 없다")
        void exactPageSizeHasNoNextPage() {
            Band band = saveBand(saveUser());
            saveFollow(band, saveUser());
            saveFollow(band, saveUser());

            CursorPage<FollowerBlock> page = bandAdapter.findPagedMyBandFollowers(band.getId(), null, 2);

            assertThat(page.getItems()).hasSize(2);
            assertThat(page.getPageInfo().hasNext()).isFalse();
            assertThat(page.getPageInfo().nextCursor()).isNull();
        }

        @Test
        @DisplayName("다른 밴드의 팔로워는 포함하지 않는다")
        void excludesOtherBandFollowers() {
            Band myBand = saveBand(saveUser());
            Band otherBand = saveBand(saveUser());
            User myFan = saveUser();
            saveFollow(myBand, myFan);
            saveFollow(otherBand, saveUser());

            CursorPage<FollowerBlock> page = bandAdapter.findPagedMyBandFollowers(myBand.getId(), null, 10);

            assertThat(page.getItems())
                    .extracting(FollowerBlock::userId)
                    .containsExactly(myFan.getId());
        }

        @Test
        @DisplayName("팔로워가 없으면 빈 페이지를 반환한다")
        void returnsEmptyPageWhenNoFollowers() {
            Band band = saveBand(saveUser());

            CursorPage<FollowerBlock> page = bandAdapter.findPagedMyBandFollowers(band.getId(), null, 10);

            assertThat(page.getItems()).isEmpty();
            assertThat(page.getPageInfo().hasNext()).isFalse();
            assertThat(page.getPageInfo().nextCursor()).isNull();
        }
    }
}
