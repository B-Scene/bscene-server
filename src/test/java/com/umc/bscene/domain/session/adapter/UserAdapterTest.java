package com.umc.bscene.domain.session.adapter;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.session.dto.application.response.BandsRecruitmentsSummaryResponse;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.entity.SessionApplicationSubmission;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.enums.ApplicationStatus;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.domain.session.enums.code.error.SessionErrorCode;
import com.umc.bscene.domain.session.exception.SessionException;
import com.umc.bscene.domain.session.repository.SessionApplicationSubmissionRepository;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import com.umc.bscene.domain.user.dto.response.session.SessionApplicationStatusResult;
import com.umc.bscene.domain.user.dto.response.session.SessionRecruitmentResponse;
import com.umc.bscene.domain.user.enums.RecruitmentStatusFilter;
import com.umc.bscene.global.response.CursorPage;
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
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * session 도메인이 user 도메인에 제공하는 포트 어댑터(SessionPort 구현) 단위 테스트.
 * <p>
 * 도메인 경계의 이음새이자 커서 페이지 + 플랫 행 그루핑이 들어있어 성능 튜닝 시 회귀가 나기 쉬운 지점이다.
 * 반환값 매핑뿐 아니라 "호출 형태"(쿼리를 몇 번, 어떤 인자로 호출하는지)까지 고정한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("session.UserAdapter (SessionPort 구현)")
class UserAdapterTest {

    private static final Long SAS_ID = 55L;
    private static final Long BAND_ID = 7L;
    private static final Long APPLICANT_ID = 100L;
    private static final Long DECIDER_ID = 200L;

    @Mock
    private SessionApplicationSubmissionRepository sasRepository;
    @Mock
    private SessionRecruitmentRepository srRepository;

    private UserAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserAdapter(sasRepository, srRepository);
    }

    private static Band band(Long id) {
        return Band.builder()
                .id(id)
                .owner(StreamFixtures.bandUser(999L))
                .name("밴드" + id)
                .genre(Genre.INDIE)
                .region(Region.SEOUL)
                .build();
    }

    private static SessionRecruitment recruitment(Long id, Band band, String title) {
        return SessionRecruitment.builder()
                .sessionRecruitmentId(id)
                .band(band)
                .summary("summary-" + id)
                .part(Part.GUITAR)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .genre(Genre.INDIE)
                .region(Region.SEOUL)
                .deadlineAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .qualification("qualification-" + id)
                .recruitmentTitle(title)
                .build();
    }

    private static SessionApplication application(Long userId, String nickname) {
        return SessionApplication.builder()
                .userId(userId)
                .nickname(nickname)
                .title("지원서 제목")
                .purpose("합주")
                .oneLineIntro("한 줄 소개")
                .part(Part.GUITAR)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .genre(Genre.INDIE)
                .region(Region.SEOUL)
                .intro("소개")
                .build();
    }

    /** applicationSubmissionId는 @GeneratedValue라 빌더로 지정할 수 없어 리플렉션으로 주입한다. */
    private static SessionApplicationSubmission submission(
            Long id, SessionRecruitment recruitment, SessionApplication application, ApplicationStatus status) {
        SessionApplicationSubmission submission = SessionApplicationSubmission.builder()
                .sessionRecruitment(recruitment)
                .sessionApplication(application)
                .status(status)
                .build();
        ReflectionTestUtils.setField(submission, "applicationSubmissionId", id);
        return submission;
    }

    private static BandsRecruitmentsSummaryResponse row(
            Long recruitmentId, Long applySubmissionId, String recruitPostTitle,
            String applierProfileImageUrl, String applierName, ApplicationStatus status) {
        return new BandsRecruitmentsSummaryResponse(
                recruitmentId,
                applySubmissionId,
                LocalDateTime.of(2026, 3, 1, 12, 0),
                recruitPostTitle,
                Part.GUITAR,
                Genre.INDIE,
                Region.SEOUL,
                applierProfileImageUrl,
                applierName,
                Part.BASS,
                SkillLevel.INTERMEDIATE,
                Region.BUSAN,
                status
        );
    }

    private static void assertSessionError(ThrowingCallable callable, SessionErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(SessionException.class)
                .extracting(thrown -> ((SessionException) thrown).getBaseResponseCode())
                .isEqualTo(expected);
    }

    @Nested
    @DisplayName("findBandIdBySessionApplicationSubmission")
    class FindBandId {

        @Test
        @DisplayName("조회된 밴드 ID를 그대로 반환하며 쿼리는 1회만 실행한다")
        void returnsBandId() {
            when(sasRepository.findBandIdBySessionApplicationSubmissionId(SAS_ID)).thenReturn(BAND_ID);

            assertThat(adapter.findBandIdBySessionApplicationSubmission(SAS_ID)).isEqualTo(BAND_ID);

            verify(sasRepository, times(1)).findBandIdBySessionApplicationSubmissionId(SAS_ID);
            verifyNoInteractions(srRepository);
        }

        @Test
        @DisplayName("공고가 삭제되어 밴드를 찾을 수 없으면 SESSION_RECRUITMENT_NOT_FOUND")
        void failsWhenNull() {
            when(sasRepository.findBandIdBySessionApplicationSubmissionId(SAS_ID)).thenReturn(null);

            assertSessionError(() -> adapter.findBandIdBySessionApplicationSubmission(SAS_ID),
                    SessionErrorCode.SESSION_RECRUITMENT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("decideApplicationSubmission")
    class DecideApplicationSubmission {

        @Test
        @DisplayName("수락하면 PENDING -> BAND_ACCEPTED로 1회 전이하고 결과의 모든 필드를 매핑한다")
        void approves() {
            SessionApplicationSubmission target = submission(SAS_ID,
                    recruitment(31L, band(BAND_ID), "기타 세션 모집"),
                    application(APPLICANT_ID, "지원자닉"),
                    ApplicationStatus.PENDING);
            when(sasRepository.findWithApplicationById(SAS_ID)).thenReturn(Optional.of(target));
            when(sasRepository.transitionStatus(SAS_ID, ApplicationStatus.PENDING, ApplicationStatus.BAND_ACCEPTED))
                    .thenReturn(1);

            SessionApplicationStatusResult result =
                    adapter.decideApplicationSubmission(SAS_ID, DECIDER_ID, true);

            assertThat(result.applicationSubmissionId()).isEqualTo(SAS_ID);
            assertThat(result.bandId()).isEqualTo(BAND_ID);
            assertThat(result.applicantUserId()).isEqualTo(APPLICANT_ID);
            assertThat(result.applicationNickname()).isEqualTo("지원자닉");
            assertThat(result.recruitmentTitle()).isEqualTo("기타 세션 모집");

            verify(sasRepository, times(1)).findWithApplicationById(SAS_ID);
            verify(sasRepository, times(1))
                    .transitionStatus(SAS_ID, ApplicationStatus.PENDING, ApplicationStatus.BAND_ACCEPTED);
        }

        @Test
        @DisplayName("거절하면 PENDING -> REJECTED로 전이한다")
        void rejects() {
            SessionApplicationSubmission target = submission(SAS_ID,
                    recruitment(31L, band(BAND_ID), "기타 세션 모집"),
                    application(APPLICANT_ID, "지원자닉"),
                    ApplicationStatus.PENDING);
            when(sasRepository.findWithApplicationById(SAS_ID)).thenReturn(Optional.of(target));
            when(sasRepository.transitionStatus(anyLong(), any(), any())).thenReturn(1);

            SessionApplicationStatusResult result =
                    adapter.decideApplicationSubmission(SAS_ID, DECIDER_ID, false);

            assertThat(result.applicantUserId()).isEqualTo(APPLICANT_ID);

            ArgumentCaptor<ApplicationStatus> expected = ArgumentCaptor.forClass(ApplicationStatus.class);
            ArgumentCaptor<ApplicationStatus> next = ArgumentCaptor.forClass(ApplicationStatus.class);
            verify(sasRepository, times(1)).transitionStatus(eq(SAS_ID), expected.capture(), next.capture());
            assertThat(expected.getValue()).isEqualTo(ApplicationStatus.PENDING);
            assertThat(next.getValue()).isEqualTo(ApplicationStatus.REJECTED);
        }

        @Test
        @DisplayName("지원 내역이 없으면 APPLICATION_SUBMISSION_NOT_FOUND이고 전이를 시도하지 않는다")
        void failsWhenSubmissionMissing() {
            when(sasRepository.findWithApplicationById(SAS_ID)).thenReturn(Optional.empty());

            assertSessionError(() -> adapter.decideApplicationSubmission(SAS_ID, DECIDER_ID, true),
                    SessionErrorCode.APPLICATION_SUBMISSION_NOT_FOUND);

            verify(sasRepository, never()).transitionStatus(anyLong(), any(), any());
        }

        @Test
        @DisplayName("본인 지원 건이면 SELF_APPLICATION_DECISION_NOT_ALLOWED이고 전이를 시도하지 않는다")
        void failsForSelfDecision() {
            SessionApplicationSubmission target = submission(SAS_ID,
                    recruitment(31L, band(BAND_ID), "기타 세션 모집"),
                    application(APPLICANT_ID, "지원자닉"),
                    ApplicationStatus.PENDING);
            when(sasRepository.findWithApplicationById(SAS_ID)).thenReturn(Optional.of(target));

            assertSessionError(() -> adapter.decideApplicationSubmission(SAS_ID, APPLICANT_ID, true),
                    SessionErrorCode.SELF_APPLICATION_DECISION_NOT_ALLOWED);

            verify(sasRepository, never()).transitionStatus(anyLong(), any(), any());
        }

        @Test
        @DisplayName("이미 처리된 건이라 전이 행이 0이면 APPLICATION_SUBMISSION_ALREADY_PROCESSED")
        void failsWhenAlreadyProcessed() {
            SessionApplicationSubmission target = submission(SAS_ID,
                    recruitment(31L, band(BAND_ID), "기타 세션 모집"),
                    application(APPLICANT_ID, "지원자닉"),
                    ApplicationStatus.BAND_ACCEPTED);
            when(sasRepository.findWithApplicationById(SAS_ID)).thenReturn(Optional.of(target));
            when(sasRepository.transitionStatus(anyLong(), any(), any())).thenReturn(0);

            assertSessionError(() -> adapter.decideApplicationSubmission(SAS_ID, DECIDER_ID, true),
                    SessionErrorCode.APPLICATION_SUBMISSION_ALREADY_PROCESSED);
        }
    }

    @Nested
    @DisplayName("finalizeApplicationSubmission")
    class FinalizeApplicationSubmission {

        @Test
        @DisplayName("최종 수락하면 BAND_ACCEPTED -> ACCEPTED로 전이하고 결과의 모든 필드를 매핑한다")
        void accepts() {
            SessionApplicationSubmission target = submission(SAS_ID,
                    recruitment(31L, band(BAND_ID), "기타 세션 모집"),
                    application(APPLICANT_ID, "지원자닉"),
                    ApplicationStatus.BAND_ACCEPTED);
            when(sasRepository.findByApplicationSubmissionIdAndSessionApplication_UserId(SAS_ID, APPLICANT_ID))
                    .thenReturn(Optional.of(target));
            when(sasRepository.transitionStatus(SAS_ID, ApplicationStatus.BAND_ACCEPTED, ApplicationStatus.ACCEPTED))
                    .thenReturn(1);
            when(sasRepository.findBandIdBySessionApplicationSubmissionId(SAS_ID)).thenReturn(BAND_ID);

            SessionApplicationStatusResult result =
                    adapter.finalizeApplicationSubmission(SAS_ID, APPLICANT_ID, true);

            assertThat(result.applicationSubmissionId()).isEqualTo(SAS_ID);
            assertThat(result.bandId()).isEqualTo(BAND_ID);
            assertThat(result.applicantUserId()).isEqualTo(APPLICANT_ID);
            assertThat(result.applicationNickname()).isEqualTo("지원자닉");
            assertThat(result.recruitmentTitle()).isEqualTo("기타 세션 모집");

            verify(sasRepository, times(1))
                    .findByApplicationSubmissionIdAndSessionApplication_UserId(SAS_ID, APPLICANT_ID);
            verify(sasRepository, times(1))
                    .transitionStatus(SAS_ID, ApplicationStatus.BAND_ACCEPTED, ApplicationStatus.ACCEPTED);
            verify(sasRepository, times(1)).findBandIdBySessionApplicationSubmissionId(SAS_ID);
        }

        @Test
        @DisplayName("최종 거절하면 BAND_ACCEPTED -> REJECTED로 전이한다")
        void rejects() {
            SessionApplicationSubmission target = submission(SAS_ID,
                    recruitment(31L, band(BAND_ID), "기타 세션 모집"),
                    application(APPLICANT_ID, "지원자닉"),
                    ApplicationStatus.BAND_ACCEPTED);
            when(sasRepository.findByApplicationSubmissionIdAndSessionApplication_UserId(SAS_ID, APPLICANT_ID))
                    .thenReturn(Optional.of(target));
            when(sasRepository.transitionStatus(anyLong(), any(), any())).thenReturn(1);
            when(sasRepository.findBandIdBySessionApplicationSubmissionId(SAS_ID)).thenReturn(BAND_ID);

            adapter.finalizeApplicationSubmission(SAS_ID, APPLICANT_ID, false);

            verify(sasRepository, times(1))
                    .transitionStatus(SAS_ID, ApplicationStatus.BAND_ACCEPTED, ApplicationStatus.REJECTED);
        }

        @Test
        @DisplayName("결과의 bandId는 재조회 값이 아니라 지원 건에 연결된 공고의 밴드에서 온다")
        void bandIdComesFromEntityNotFromLookup() {
            SessionApplicationSubmission target = submission(SAS_ID,
                    recruitment(31L, band(BAND_ID), "기타 세션 모집"),
                    application(APPLICANT_ID, "지원자닉"),
                    ApplicationStatus.BAND_ACCEPTED);
            when(sasRepository.findByApplicationSubmissionIdAndSessionApplication_UserId(SAS_ID, APPLICANT_ID))
                    .thenReturn(Optional.of(target));
            when(sasRepository.transitionStatus(anyLong(), any(), any())).thenReturn(1);
            // 재조회 쿼리는 "공고가 아직 살아있는지" 확인용일 뿐 결과에 쓰이지 않는다
            when(sasRepository.findBandIdBySessionApplicationSubmissionId(SAS_ID)).thenReturn(999L);

            SessionApplicationStatusResult result =
                    adapter.finalizeApplicationSubmission(SAS_ID, APPLICANT_ID, true);

            assertThat(result.bandId()).isEqualTo(BAND_ID);
        }

        @Test
        @DisplayName("본인 지원 건이 아니면 APPLICATION_SUBMISSION_NOT_FOUND이고 전이·밴드 재조회를 하지 않는다")
        void failsWhenNotOwnSubmission() {
            when(sasRepository.findByApplicationSubmissionIdAndSessionApplication_UserId(SAS_ID, APPLICANT_ID))
                    .thenReturn(Optional.empty());

            assertSessionError(() -> adapter.finalizeApplicationSubmission(SAS_ID, APPLICANT_ID, true),
                    SessionErrorCode.APPLICATION_SUBMISSION_NOT_FOUND);

            verify(sasRepository, never()).transitionStatus(anyLong(), any(), any());
            verify(sasRepository, never()).findBandIdBySessionApplicationSubmissionId(anyLong());
        }

        @Test
        @DisplayName("BAND_ACCEPTED 상태가 아니어서 전이 행이 0이면 APPLICATION_SUBMISSION_NOT_CONFIRMABLE")
        void failsWhenNotConfirmable() {
            SessionApplicationSubmission target = submission(SAS_ID,
                    recruitment(31L, band(BAND_ID), "기타 세션 모집"),
                    application(APPLICANT_ID, "지원자닉"),
                    ApplicationStatus.PENDING);
            when(sasRepository.findByApplicationSubmissionIdAndSessionApplication_UserId(SAS_ID, APPLICANT_ID))
                    .thenReturn(Optional.of(target));
            when(sasRepository.transitionStatus(anyLong(), any(), any())).thenReturn(0);

            assertSessionError(() -> adapter.finalizeApplicationSubmission(SAS_ID, APPLICANT_ID, true),
                    SessionErrorCode.APPLICATION_SUBMISSION_NOT_CONFIRMABLE);

            verify(sasRepository, never()).findBandIdBySessionApplicationSubmissionId(anyLong());
        }

        @Test
        @DisplayName("전이 후 공고가 삭제되어 밴드를 찾을 수 없으면 SESSION_RECRUITMENT_NOT_FOUND")
        void failsWhenRecruitmentDeleted() {
            SessionApplicationSubmission target = submission(SAS_ID,
                    recruitment(31L, band(BAND_ID), "기타 세션 모집"),
                    application(APPLICANT_ID, "지원자닉"),
                    ApplicationStatus.BAND_ACCEPTED);
            when(sasRepository.findByApplicationSubmissionIdAndSessionApplication_UserId(SAS_ID, APPLICANT_ID))
                    .thenReturn(Optional.of(target));
            when(sasRepository.transitionStatus(anyLong(), any(), any())).thenReturn(1);
            when(sasRepository.findBandIdBySessionApplicationSubmissionId(SAS_ID)).thenReturn(null);

            assertSessionError(() -> adapter.finalizeApplicationSubmission(SAS_ID, APPLICANT_ID, true),
                    SessionErrorCode.SESSION_RECRUITMENT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("findRecruitmentsByBandId")
    class FindRecruitmentsByBandId {

        @Test
        @DisplayName("OPEN 필터는 진행 중 공고 쿼리를 쓰고, 지원자 조회는 페이지의 공고 ID 전체로 정확히 1회만 한다")
        void queriesApplicantsOnceForWholePage() {
            when(srRepository.findOpenRecruitmentIdsByBandId(eq(BAND_ID), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of(31L, 32L));
            when(sasRepository.findApplicantsByRecruitmentIds(anyCollection()))
                    .thenReturn(List.of(
                            row(31L, 501L,"기타 모집", "https://cdn.test/a.jpg", "지원자A", ApplicationStatus.PENDING),
                            row(32L, 502L,"베이스 모집", "https://cdn.test/b.jpg", "지원자B", ApplicationStatus.BAND_ACCEPTED),
                            row(31L, 503L,"기타 모집", "https://cdn.test/c.jpg", "지원자C", ApplicationStatus.PENDING)
                    ));

            CursorPage<SessionRecruitmentResponse> page =
                    adapter.findRecruitmentsByBandId(BAND_ID, RecruitmentStatusFilter.OPEN, 20L, 10);

            // 공고 수만큼 쿼리를 반복하면(N+1) 이 검증이 깨진다
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<Long>> ids = ArgumentCaptor.forClass(Collection.class);
            verify(sasRepository, times(1)).findApplicantsByRecruitmentIds(ids.capture());
            assertThat(ids.getValue()).containsExactly(31L, 32L);

            verify(srRepository, never())
                    .findClosedRecruitmentIdsByBandId(anyLong(), any(), any(), any(Pageable.class));

            assertThat(page.getItems()).extracting(SessionRecruitmentResponse::recruitmentPostId)
                    .containsExactly(31L, 32L);
        }

        @Test
        @DisplayName("페이지 쿼리에는 (bandId, 현재시각, cursorId, size+1 페이저블)이 전달된다")
        void passesPageQueryArguments() {
            when(srRepository.findOpenRecruitmentIdsByBandId(eq(BAND_ID), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of());

            LocalDateTime before = LocalDateTime.now();
            adapter.findRecruitmentsByBandId(BAND_ID, RecruitmentStatusFilter.OPEN, 20L, 10);
            LocalDateTime after = LocalDateTime.now();

            ArgumentCaptor<LocalDateTime> now = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<Long> cursorId = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(srRepository, times(1))
                    .findOpenRecruitmentIdsByBandId(eq(BAND_ID), now.capture(), cursorId.capture(), pageable.capture());

            assertThat(now.getValue()).isBetween(before, after);
            assertThat(cursorId.getValue()).isEqualTo(20L);
            assertThat(pageable.getValue().getPageNumber()).isZero();
            // hasNext 판별을 위해 항상 한 건 더 조회한다
            assertThat(pageable.getValue().getPageSize()).isEqualTo(11);
        }

        @Test
        @DisplayName("CLOSE 필터는 마감 공고 쿼리를 쓰고 진행 중 공고 쿼리는 호출하지 않는다")
        void usesClosedQueryForCloseFilter() {
            when(srRepository.findClosedRecruitmentIdsByBandId(eq(BAND_ID), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of());

            adapter.findRecruitmentsByBandId(BAND_ID, RecruitmentStatusFilter.CLOSE, null, 10);

            verify(srRepository, times(1))
                    .findClosedRecruitmentIdsByBandId(eq(BAND_ID), any(), any(), any(Pageable.class));
            verify(srRepository, never())
                    .findOpenRecruitmentIdsByBandId(anyLong(), any(), any(), any(Pageable.class));
        }

        @Test
        @DisplayName("공고가 하나도 없으면 빈 페이지를 반환하고 지원자 쿼리는 아예 실행하지 않는다")
        void shortCircuitsOnEmptyPage() {
            when(srRepository.findOpenRecruitmentIdsByBandId(eq(BAND_ID), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of());

            CursorPage<SessionRecruitmentResponse> page =
                    adapter.findRecruitmentsByBandId(BAND_ID, RecruitmentStatusFilter.OPEN, null, 10);

            assertThat(page.getItems()).isEmpty();
            assertThat(page.getPageInfo().hasNext()).isFalse();
            assertThat(page.getPageInfo().nextCursor()).isNull();
            verifyNoInteractions(sasRepository);
        }

        @Test
        @DisplayName("size보다 한 건 더 조회되면 마지막 한 건을 잘라내고 hasNext=true, 커서는 페이지의 마지막 공고 ID다")
        void trimsExtraRowAndSetsCursor() {
            when(srRepository.findOpenRecruitmentIdsByBandId(eq(BAND_ID), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of(31L, 32L, 33L));
            when(sasRepository.findApplicantsByRecruitmentIds(anyCollection()))
                    .thenReturn(List.of(
                            row(31L, 501L,"기타 모집", null, "지원자A", ApplicationStatus.PENDING),
                            row(32L, 502L,"베이스 모집", null, "지원자B", ApplicationStatus.PENDING)
                    ));

            CursorPage<SessionRecruitmentResponse> page =
                    adapter.findRecruitmentsByBandId(BAND_ID, RecruitmentStatusFilter.OPEN, null, 2);

            assertThat(page.getItems()).extracting(SessionRecruitmentResponse::recruitmentPostId)
                    .containsExactly(31L, 32L);
            assertThat(page.getPageInfo().hasNext()).isTrue();
            assertThat(page.getPageInfo().nextCursor()).isEqualTo(32L);

            // 잘라낸 33L은 지원자 조회 대상에서도 빠져야 한다
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<Long>> ids = ArgumentCaptor.forClass(Collection.class);
            verify(sasRepository, times(1)).findApplicantsByRecruitmentIds(ids.capture());
            assertThat(ids.getValue()).containsExactly(31L, 32L);
        }

        @Test
        @DisplayName("size 이하로 조회되면 hasNext=false, 커서는 null이다")
        void marksLastPage() {
            when(srRepository.findOpenRecruitmentIdsByBandId(eq(BAND_ID), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of(31L, 32L));
            when(sasRepository.findApplicantsByRecruitmentIds(anyCollection()))
                    .thenReturn(List.of(
                            row(31L, 501L,"기타 모집", null, "지원자A", ApplicationStatus.PENDING),
                            row(32L, 502L,"베이스 모집", null, "지원자B", ApplicationStatus.PENDING)
                    ));

            CursorPage<SessionRecruitmentResponse> page =
                    adapter.findRecruitmentsByBandId(BAND_ID, RecruitmentStatusFilter.OPEN, null, 10);

            assertThat(page.getPageInfo().hasNext()).isFalse();
            assertThat(page.getPageInfo().nextCursor()).isNull();
        }

        @Test
        @DisplayName("플랫 행이 섞여 들어와도 공고 순서는 페이지 쿼리 순서를 따르고 지원자는 행 순서를 유지한다")
        void groupsRowsInPageOrder() {
            when(srRepository.findOpenRecruitmentIdsByBandId(eq(BAND_ID), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of(33L, 31L, 32L));
            when(sasRepository.findApplicantsByRecruitmentIds(anyCollection()))
                    .thenReturn(List.of(
                            row(31L, 501L,"기타 모집", null, "지원자A", ApplicationStatus.PENDING),
                            row(32L, 502L,"베이스 모집", null, "지원자B", ApplicationStatus.PENDING),
                            row(33L, 503L,"드럼 모집", null, "지원자C", ApplicationStatus.PENDING),
                            row(31L, 504L,"기타 모집", null, "지원자D", ApplicationStatus.PENDING)
                    ));

            CursorPage<SessionRecruitmentResponse> page =
                    adapter.findRecruitmentsByBandId(BAND_ID, RecruitmentStatusFilter.OPEN, null, 10);

            assertThat(page.getItems()).extracting(SessionRecruitmentResponse::recruitmentPostId)
                    .containsExactly(33L, 31L, 32L);
            assertThat(page.getItems().get(1).recruiters())
                    .extracting(SessionRecruitmentResponse.Recruiter::name)
                    .containsExactly("지원자A", "지원자D");
        }

        @Test
        @DisplayName("같은 공고의 행이 여러 건이면 공고 공통 컬럼은 첫 행이 이긴다")
        void firstRowWinsForRecruitmentColumns() {
            when(srRepository.findOpenRecruitmentIdsByBandId(eq(BAND_ID), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of(31L));
            when(sasRepository.findApplicantsByRecruitmentIds(anyCollection()))
                    .thenReturn(List.of(
                            row(31L, 501L,"첫 행 제목", null, "지원자A", ApplicationStatus.PENDING),
                            row(31L, 502L,"둘째 행 제목", null, "지원자B", ApplicationStatus.PENDING)
                    ));

            CursorPage<SessionRecruitmentResponse> page =
                    adapter.findRecruitmentsByBandId(BAND_ID, RecruitmentStatusFilter.OPEN, null, 10);

            // 그루핑은 computeIfAbsent + add라 병합 없이 모든 행을 보존하고, 공고 컬럼은 group.get(0)에서 읽는다
            assertThat(page.getItems()).hasSize(1);
            assertThat(page.getItems().get(0).title()).isEqualTo("첫 행 제목");
            assertThat(page.getItems().get(0).recruiters()).hasSize(2);
        }

        @Test
        @DisplayName("지원자 행이 한 건도 없는 공고는 null이 아니라 결과에서 제외된다")
        void omitsRecruitmentsWithoutRows() {
            when(srRepository.findOpenRecruitmentIdsByBandId(eq(BAND_ID), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of(31L, 32L));
            when(sasRepository.findApplicantsByRecruitmentIds(anyCollection()))
                    .thenReturn(List.of(
                            row(32L, 502L,"베이스 모집", null, "지원자B", ApplicationStatus.PENDING)
                    ));

            CursorPage<SessionRecruitmentResponse> page =
                    adapter.findRecruitmentsByBandId(BAND_ID, RecruitmentStatusFilter.OPEN, null, 10);

            assertThat(page.getItems()).extracting(SessionRecruitmentResponse::recruitmentPostId)
                    .containsExactly(32L);
            assertThat(page.getItems()).doesNotContainNull();
        }

        @Test
        @DisplayName("공고·지원자 필드가 플랫 행에서 1:1로 매핑되고, 지원자 프로필 이미지가 null이면 null 그대로 내려간다")
        void mapsEveryField() {
            when(srRepository.findOpenRecruitmentIdsByBandId(eq(BAND_ID), any(), any(), any(Pageable.class)))
                    .thenReturn(List.of(31L));
            when(sasRepository.findApplicantsByRecruitmentIds(anyCollection()))
                    .thenReturn(List.of(
                            row(31L, 501L,"기타 모집", "https://cdn.test/a.jpg", "지원자A", ApplicationStatus.PENDING),
                            row(31L, 502L,"기타 모집", null, "지원자B", ApplicationStatus.BAND_ACCEPTED)
                    ));

            CursorPage<SessionRecruitmentResponse> page =
                    adapter.findRecruitmentsByBandId(BAND_ID, RecruitmentStatusFilter.OPEN, null, 10);

            SessionRecruitmentResponse item = page.getItems().get(0);
            assertThat(item.recruitmentPostId()).isEqualTo(31L);
            assertThat(item.dueDate()).isEqualTo(LocalDateTime.of(2026, 3, 1, 12, 0));
            assertThat(item.title()).isEqualTo("기타 모집");
            assertThat(item.part()).isEqualTo("기타");
            assertThat(item.genre()).isEqualTo("인디");
            assertThat(item.region()).isEqualTo("서울");
            assertThat(item.totalApplicants()).isEqualTo(2);

            SessionRecruitmentResponse.Recruiter first = item.recruiters().get(0);
            assertThat(first.applySubmissionId()).isEqualTo(501L);
            assertThat(first.profileImageUrl()).isEqualTo("https://cdn.test/a.jpg");
            assertThat(first.name()).isEqualTo("지원자A");
            assertThat(first.part()).isEqualTo("베이스");
            // level은 지원자의 SkillLevel description에서 온다
            assertThat(first.level()).isEqualTo("중급");
            assertThat(first.region()).isEqualTo("부산");
            assertThat(first.status()).isEqualTo(ApplicationStatus.PENDING);

            SessionRecruitmentResponse.Recruiter second = item.recruiters().get(1);
            assertThat(second.profileImageUrl()).isNull();
            assertThat(second.status()).isEqualTo(ApplicationStatus.BAND_ACCEPTED);
        }
    }
}
