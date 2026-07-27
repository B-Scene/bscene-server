package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.request.DiscordWebhookRequest;
import com.umc.bscene.domain.stream.entity.mapper.ReportHistory;
import com.umc.bscene.domain.stream.enums.ReportType;
import com.umc.bscene.domain.stream.port.DiscordWebhookPort;
import com.umc.bscene.domain.stream.repository.ReportHistoryRepository;
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
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 디스코드 신고 알림 발송기 검증.
 * <p>
 * 핵심 불변식:
 * - 웹훅 발송이 성공한 뒤에야 발송 완료로 마킹한다 (실패 건은 재발송 스캔 대상으로 남아야 한다)
 * - 즉시 발송(@Async) 경로는 예외를 삼켜 신고 트랜잭션/스레드에 영향을 주지 않는다
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DiscordMessageSender")
class DiscordMessageSenderTest {

    private static final Long REPORT_ID = 500L;
    private static final Long REPORTER_ID = 4242L;
    private static final Long TARGET_USER_ID = 7777L;
    private static final String CHAT_MESSAGE = "문제가 된 채팅 내용";
    private static final String COMMENT = "반복적으로 욕설을 했습니다";

    @Mock
    private DiscordWebhookPort discordWebhookPort;

    @Mock
    private ReportHistoryRepository reportHistoryRepository;

    @Captor
    private ArgumentCaptor<DiscordWebhookRequest> requestCaptor;

    @Captor
    private ArgumentCaptor<LocalDateTime> notifiedAtCaptor;

    private DiscordMessageSender sender;

    private ReportHistory reportHistory;

    @BeforeEach
    void setUp() {
        sender = new DiscordMessageSender(discordWebhookPort, reportHistoryRepository);
        reportHistory = ReportHistory.builder()
                .id(REPORT_ID)
                .targetUser(StreamFixtures.fanUser(TARGET_USER_ID))
                .reporterId(REPORTER_ID)
                .reportType(ReportType.ABUSE)
                .chatMessage(CHAT_MESSAGE)
                .comment(COMMENT)
                .build();
    }

    @Nested
    @DisplayName("sendReportNotification()")
    class SendReportNotification {

        @Test
        @DisplayName("신고자/대상/유형/채팅/코멘트를 순서대로 채운 웹훅 페이로드를 만든다")
        void buildsPayloadFromReportHistory() {
            sender.sendReportNotification(reportHistory);

            verify(discordWebhookPort).send(requestCaptor.capture());
            String content = requestCaptor.getValue().content();
            assertThat(content)
                    .contains(String.valueOf(REPORTER_ID))
                    .contains(String.valueOf(TARGET_USER_ID))
                    .contains(ReportType.ABUSE.name())
                    .contains(CHAT_MESSAGE)
                    .contains(COMMENT);
            assertThat(content).containsSubsequence(
                    String.valueOf(REPORTER_ID),
                    String.valueOf(TARGET_USER_ID),
                    ReportType.ABUSE.name(),
                    CHAT_MESSAGE,
                    COMMENT
            );
        }

        @Test
        @DisplayName("발송 성공 후 해당 신고 건을 발송 완료 시각과 함께 마킹한다")
        void marksReportNotifiedAfterSend() {
            LocalDateTime before = LocalDateTime.now();

            sender.sendReportNotification(reportHistory);

            LocalDateTime after = LocalDateTime.now();
            verify(reportHistoryRepository).markDiscordNotified(eq(REPORT_ID), notifiedAtCaptor.capture());
            assertThat(notifiedAtCaptor.getValue()).isBetween(before, after);
        }

        @Test
        @DisplayName("마킹은 반드시 발송 이후에 일어난다")
        void sendsBeforeMarking() {
            sender.sendReportNotification(reportHistory);

            InOrder inOrder = inOrder(discordWebhookPort, reportHistoryRepository);
            inOrder.verify(discordWebhookPort).send(any(DiscordWebhookRequest.class));
            inOrder.verify(reportHistoryRepository).markDiscordNotified(eq(REPORT_ID), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("발송이 실패하면 예외를 전파하고 발송 완료로 마킹하지 않는다")
        void doesNotMarkWhenSendFails() {
            doThrow(new RestClientException("디스코드 웹훅 장애"))
                    .when(discordWebhookPort).send(any(DiscordWebhookRequest.class));

            assertThatThrownBy(() -> sender.sendReportNotification(reportHistory))
                    .isInstanceOf(RestClientException.class);

            verify(reportHistoryRepository, never()).markDiscordNotified(any(), any());
        }

        @Test
        @DisplayName("상세 코멘트가 null이어도 발송된다")
        void handlesNullComment() {
            ReportHistory withoutComment = ReportHistory.builder()
                    .id(REPORT_ID)
                    .targetUser(StreamFixtures.fanUser(TARGET_USER_ID))
                    .reporterId(REPORTER_ID)
                    .reportType(ReportType.ETC)
                    .chatMessage(CHAT_MESSAGE)
                    .comment(null)
                    .build();

            sender.sendReportNotification(withoutComment);

            verify(discordWebhookPort).send(requestCaptor.capture());
            assertThat(requestCaptor.getValue().content())
                    .contains(ReportType.ETC.name())
                    .contains(CHAT_MESSAGE);
            verify(reportHistoryRepository).markDiscordNotified(eq(REPORT_ID), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("sendReportNotificationAsync()")
    class SendReportNotificationAsync {

        @Test
        @DisplayName("존재하지 않는 신고 ID면 아무것도 발송하지 않는다")
        void doesNothingForUnknownId() {
            when(reportHistoryRepository.findById(999L)).thenReturn(Optional.empty());

            sender.sendReportNotificationAsync(999L);

            verifyNoInteractions(discordWebhookPort);
            verify(reportHistoryRepository, never()).markDiscordNotified(any(), any());
        }

        @Test
        @DisplayName("존재하는 신고 ID면 동기 발송 경로에 그대로 위임한다")
        void delegatesForKnownId() {
            when(reportHistoryRepository.findById(REPORT_ID)).thenReturn(Optional.of(reportHistory));

            sender.sendReportNotificationAsync(REPORT_ID);

            verify(discordWebhookPort).send(requestCaptor.capture());
            assertThat(requestCaptor.getValue().content()).contains(CHAT_MESSAGE);
            verify(reportHistoryRepository).markDiscordNotified(eq(REPORT_ID), any(LocalDateTime.class));
        }

        @Test
        @DisplayName("웹훅 예외를 삼켜 호출부로 전파하지 않는다 (재발송 스케줄러가 처리)")
        void swallowsWebhookFailure() {
            when(reportHistoryRepository.findById(REPORT_ID)).thenReturn(Optional.of(reportHistory));
            doThrow(new RestClientException("디스코드 웹훅 장애"))
                    .when(discordWebhookPort).send(any(DiscordWebhookRequest.class));

            assertThatCode(() -> sender.sendReportNotificationAsync(REPORT_ID)).doesNotThrowAnyException();

            verify(reportHistoryRepository, never()).markDiscordNotified(any(), any());
        }

        @Test
        @DisplayName("발송 성공 후 마킹이 실패해도 예외를 삼킨다 (미마킹 건은 재발송 스케줄러가 다시 집는다)")
        void swallowsMarkingFailureAfterSuccessfulSend() {
            when(reportHistoryRepository.findById(REPORT_ID)).thenReturn(Optional.of(reportHistory));
            doThrow(new RuntimeException("DB 커넥션 끊김"))
                    .when(reportHistoryRepository).markDiscordNotified(eq(REPORT_ID), any(LocalDateTime.class));

            assertThatCode(() -> sender.sendReportNotificationAsync(REPORT_ID)).doesNotThrowAnyException();

            verify(discordWebhookPort).send(any(DiscordWebhookRequest.class));
        }
    }
}
