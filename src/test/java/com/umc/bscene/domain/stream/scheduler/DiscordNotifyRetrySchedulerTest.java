package com.umc.bscene.domain.stream.scheduler;

import com.umc.bscene.domain.stream.entity.mapper.ReportHistory;
import com.umc.bscene.domain.stream.enums.ReportType;
import com.umc.bscene.domain.stream.repository.ReportHistoryRepository;
import com.umc.bscene.domain.stream.service.DiscordMessageSender;
import com.umc.bscene.support.StreamFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 디스코드 신고 알림 재발송 스케줄러 검증.
 * <p>
 * 핵심 불변식:
 * - 즉시 발송 경로와 겹치지 않도록 최근 5분 이내 건은 스캔에서 제외한다
 * - 7일이 지난 건은 스캔 대상에서 빠진다
 * - 한 건이라도 실패하면 (디스코드 장애로 판단) 이번 주기를 즉시 중단한다
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DiscordNotifyRetryScheduler")
class DiscordNotifyRetrySchedulerTest {

    private static final Duration IMMEDIATE_SEND_GRACE = Duration.ofMinutes(5);
    private static final Duration RESEND_EXPIRE = Duration.ofDays(7);

    @Mock
    private ReportHistoryRepository reportHistoryRepository;

    @Mock
    private DiscordMessageSender discordMessageSender;

    @Captor
    private ArgumentCaptor<LocalDateTime> startCaptor;

    @Captor
    private ArgumentCaptor<LocalDateTime> endCaptor;

    private DiscordNotifyRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new DiscordNotifyRetryScheduler(reportHistoryRepository, discordMessageSender);
    }

    private ReportHistory report(Long id) {
        return ReportHistory.builder()
                .id(id)
                .targetUser(StreamFixtures.fanUser(id + 100))
                .reporterId(id + 200)
                .reportType(ReportType.ABUSE)
                .chatMessage("문제 채팅 " + id)
                .comment("상세 설명 " + id)
                .build();
    }

    @Nested
    @DisplayName("스캔 구간")
    class ScanWindow {

        @Test
        @DisplayName("[현재 - 7일, 현재 - 5분] 구간의 미발송 건을 조회한다")
        void queriesSevenDayWindowExcludingLastFiveMinutes() {
            when(reportHistoryRepository
                    .findTop20ByDiscordNotifiedAtIsNullAndCreatedAtBetweenOrderByCreatedAtAsc(any(), any()))
                    .thenReturn(List.of());
            LocalDateTime before = LocalDateTime.now();

            scheduler.resendUnnotifiedReports();

            LocalDateTime after = LocalDateTime.now();
            verify(reportHistoryRepository)
                    .findTop20ByDiscordNotifiedAtIsNullAndCreatedAtBetweenOrderByCreatedAtAsc(
                            startCaptor.capture(), endCaptor.capture()
                    );

            LocalDateTime start = startCaptor.getValue();
            LocalDateTime end = endCaptor.getValue();
            assertThat(start).isBefore(end);
            assertThat(Duration.between(start, end)).isEqualTo(RESEND_EXPIRE.minus(IMMEDIATE_SEND_GRACE));
            assertThat(end).isBetween(before.minus(IMMEDIATE_SEND_GRACE), after.minus(IMMEDIATE_SEND_GRACE));
            assertThat(start).isBetween(before.minus(RESEND_EXPIRE), after.minus(RESEND_EXPIRE));
        }

        @Test
        @DisplayName("대상이 없으면 발송기를 전혀 호출하지 않는다")
        void doesNotSendWhenNoTargets() {
            when(reportHistoryRepository
                    .findTop20ByDiscordNotifiedAtIsNullAndCreatedAtBetweenOrderByCreatedAtAsc(any(), any()))
                    .thenReturn(List.of());

            scheduler.resendUnnotifiedReports();

            verifyNoInteractions(discordMessageSender);
        }
    }

    @Nested
    @DisplayName("재발송")
    class Resend {

        @Test
        @DisplayName("조회된 순서대로 모든 미발송 건을 발송한다")
        void sendsEveryTargetInOrder() {
            ReportHistory first = report(1L);
            ReportHistory second = report(2L);
            ReportHistory third = report(3L);
            when(reportHistoryRepository
                    .findTop20ByDiscordNotifiedAtIsNullAndCreatedAtBetweenOrderByCreatedAtAsc(any(), any()))
                    .thenReturn(List.of(first, second, third));

            scheduler.resendUnnotifiedReports();

            InOrder inOrder = inOrder(discordMessageSender);
            inOrder.verify(discordMessageSender).sendReportNotification(first);
            inOrder.verify(discordMessageSender).sendReportNotification(second);
            inOrder.verify(discordMessageSender).sendReportNotification(third);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("한 건이 실패하면 남은 건을 시도하지 않고 이번 주기를 중단한다")
        void stopsImmediatelyOnFirstFailure() {
            ReportHistory first = report(1L);
            ReportHistory second = report(2L);
            ReportHistory third = report(3L);
            when(reportHistoryRepository
                    .findTop20ByDiscordNotifiedAtIsNullAndCreatedAtBetweenOrderByCreatedAtAsc(any(), any()))
                    .thenReturn(List.of(first, second, third));
            /*
             * 특정 인스턴스만 스터빙하면, 매칭되지 않는 첫 건 호출에서 strict stubs가
             * PotentialStubbingProblem을 던지고 스케줄러의 catch(Exception)이 그걸 삼켜
             * "첫 건 실패"로 둔갑한다. 따라서 any()로 전체를 받고 id로 분기한다.
             */
            doAnswer(invocation -> {
                ReportHistory target = invocation.getArgument(0);
                if (target.getId().equals(2L))
                    throw new IllegalStateException("디스코드 장애");
                return null;
            }).when(discordMessageSender).sendReportNotification(any());

            scheduler.resendUnnotifiedReports();

            ArgumentCaptor<ReportHistory> sent = ArgumentCaptor.forClass(ReportHistory.class);
            verify(discordMessageSender, times(2)).sendReportNotification(sent.capture());
            assertThat(sent.getAllValues())
                    .extracting(ReportHistory::getId)
                    .as("두 번째 건에서 중단되므로 세 번째 건은 시도되지 않는다")
                    .containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("발송 실패 예외는 스케줄러 밖으로 전파되지 않는다")
        void swallowsSendFailure() {
            ReportHistory only = report(1L);
            when(reportHistoryRepository
                    .findTop20ByDiscordNotifiedAtIsNullAndCreatedAtBetweenOrderByCreatedAtAsc(any(), any()))
                    .thenReturn(List.of(only));
            doThrow(new RuntimeException("디스코드 장애")).when(discordMessageSender).sendReportNotification(only);

            assertThatCode(() -> scheduler.resendUnnotifiedReports()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("첫 건부터 실패하면 아무 건도 추가로 시도하지 않는다")
        void stopsWhenFirstTargetFails() {
            ReportHistory first = report(1L);
            ReportHistory second = report(2L);
            when(reportHistoryRepository
                    .findTop20ByDiscordNotifiedAtIsNullAndCreatedAtBetweenOrderByCreatedAtAsc(any(), any()))
                    .thenReturn(List.of(first, second));
            doThrow(new IllegalStateException("디스코드 장애")).when(discordMessageSender).sendReportNotification(first);

            scheduler.resendUnnotifiedReports();

            verify(discordMessageSender).sendReportNotification(first);
            verify(discordMessageSender, never()).sendReportNotification(second);
        }
    }
}
