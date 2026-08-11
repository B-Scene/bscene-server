package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.StreamPushMessage;
import com.umc.bscene.domain.stream.dto.response.BandSummaryResponse;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.entity.LiveAlarm;
import com.umc.bscene.domain.stream.port.BandMemberPort;
import com.umc.bscene.domain.stream.port.NotifyPort;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.domain.stream.repository.LiveAlarmRepository;
import com.umc.bscene.global.notification.enums.NotificationSettingType;
import com.umc.bscene.global.notification.message.PushMessage;
import com.umc.bscene.support.StreamFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("StreamReminderService 예정 라이브 30분 전 리마인드")
class StreamReminderServiceTest {

    private static final Long BROADCASTER_ID = 100L;
    private static final Long BAND_ID = 7L;
    private static final Long LIVE_ID = 55L;
    private static final String BAND_NAME = "밴드이름";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 26, 20, 0);
    private static final LocalDateTime SCHEDULED_AT = NOW.plusMinutes(20);

    @Mock
    private AudioStreamRepository audioStreamRepository;
    @Mock
    private LiveAlarmRepository liveAlarmRepository;
    @Mock
    private BandMemberPort bandMemberPort;
    @Mock
    private NotifyPort notifyPort;

    @Captor
    private ArgumentCaptor<List<Long>> receiverCaptor;
    @Captor
    private ArgumentCaptor<PushMessage> messageCaptor;

    private StreamReminderService reminderService;

    @BeforeEach
    void setUp() {
        reminderService = new StreamReminderService(
                audioStreamRepository,
                liveAlarmRepository,
                bandMemberPort,
                notifyPort
        );
    }

    @Test
    @DisplayName("팬 알림 대상에게 팬 리마인드를 보내고 알림별 발송 시각을 기록한다")
    void sendsFanReminderAndMarksAlarms() {
        AudioStream stream = scheduledStream(LIVE_ID);
        LiveAlarm alarm1 = alarm(1L, stream, 200L);
        LiveAlarm alarm2 = alarm(2L, stream, 300L);
        given(liveAlarmRepository.findReminderTargets(NOW, NOW.plusMinutes(30)))
                .willReturn(List.of(alarm1, alarm2));
        given(audioStreamRepository.findMemberReminderTargets(NOW, NOW.plusMinutes(30)))
                .willReturn(List.of());
        given(bandMemberPort.getBandSummaryByBandId(BAND_ID))
                .willReturn(Optional.of(new BandSummaryResponse(BAND_ID, BAND_NAME)));
        reminderService.sendUpcomingReminders(NOW);

        verify(notifyPort).notify(receiverCaptor.capture(), messageCaptor.capture());
        assertThat(receiverCaptor.getValue()).containsExactly(200L, 300L);
        StreamPushMessage message = (StreamPushMessage) messageCaptor.getValue();
        assertThat(message.settingType()).isEqualTo(NotificationSettingType.FAN_SCHEDULED_LIVE_REMINDER);
        assertThat(message.referenceId()).isEqualTo(LIVE_ID);
        assertThat(message.title()).contains(BAND_NAME);
        assertThat(alarm1.getReminderSentAt()).isEqualTo(NOW);
        assertThat(alarm2.getReminderSentAt()).isEqualTo(NOW);
        assertThat(stream.getMemberReminderSentAt()).isNull();
        verify(bandMemberPort, never()).getAcceptedMemberUserIds(BAND_ID);
    }

    @Test
    @DisplayName("밴드 구성원 알림 대상만 있으면 송출자를 뺀 정회원에게 밴드 리마인드를 보내고 라이브 단위로 발송 시각을 기록한다")
    void sendsMemberReminderExcludingBroadcasterAndMarksStream() {
        AudioStream stream = scheduledStream(LIVE_ID);
        given(liveAlarmRepository.findReminderTargets(NOW, NOW.plusMinutes(30)))
                .willReturn(List.of());
        given(audioStreamRepository.findMemberReminderTargets(NOW, NOW.plusMinutes(30)))
                .willReturn(List.of(stream));
        given(bandMemberPort.getBandSummaryByBandId(BAND_ID))
                .willReturn(Optional.of(new BandSummaryResponse(BAND_ID, BAND_NAME)));
        given(bandMemberPort.getAcceptedMemberUserIds(BAND_ID))
                .willReturn(List.of(BROADCASTER_ID, 400L, 500L));

        reminderService.sendUpcomingReminders(NOW);

        verify(notifyPort).notify(receiverCaptor.capture(), messageCaptor.capture());
        assertThat(receiverCaptor.getValue()).containsExactly(400L, 500L);
        StreamPushMessage message = (StreamPushMessage) messageCaptor.getValue();
        assertThat(message.settingType()).isEqualTo(NotificationSettingType.BAND_SCHEDULED_LIVE_REMINDER);
        assertThat(message.referenceId()).isEqualTo(LIVE_ID);
        assertThat(stream.getMemberReminderSentAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("팬 알림 대상이 밴드 구성원이기도 하면 밴드 리마인드로 한 번만 받는다")
    void fanWhoIsAlsoMemberReceivesOnlyBandReminder() {
        AudioStream stream = scheduledStream(LIVE_ID);
        LiveAlarm fanOnlyAlarm = alarm(1L, stream, 200L);
        LiveAlarm memberFanAlarm = alarm(2L, stream, 400L);
        given(liveAlarmRepository.findReminderTargets(NOW, NOW.plusMinutes(30)))
                .willReturn(List.of(fanOnlyAlarm, memberFanAlarm));
        given(audioStreamRepository.findMemberReminderTargets(NOW, NOW.plusMinutes(30)))
                .willReturn(List.of(stream));
        given(bandMemberPort.getBandSummaryByBandId(BAND_ID))
                .willReturn(Optional.of(new BandSummaryResponse(BAND_ID, BAND_NAME)));
        given(bandMemberPort.getAcceptedMemberUserIds(BAND_ID))
                .willReturn(List.of(400L, 500L));

        reminderService.sendUpcomingReminders(NOW);

        verify(notifyPort, times(2)).notify(receiverCaptor.capture(), messageCaptor.capture());
        List<List<Long>> receivers = receiverCaptor.getAllValues();
        List<PushMessage> messages = messageCaptor.getAllValues();

        StreamPushMessage fanMessage = (StreamPushMessage) messages.get(0);
        assertThat(fanMessage.settingType()).isEqualTo(NotificationSettingType.FAN_SCHEDULED_LIVE_REMINDER);
        assertThat(receivers.get(0)).containsExactly(200L);

        StreamPushMessage bandMessage = (StreamPushMessage) messages.get(1);
        assertThat(bandMessage.settingType()).isEqualTo(NotificationSettingType.BAND_SCHEDULED_LIVE_REMINDER);
        assertThat(receivers.get(1)).containsExactly(400L, 500L);
    }

    @Test
    @DisplayName("송출자는 팬 알림 대상이어도 리마인드를 받지 않는다")
    void broadcasterIsExcludedFromFanReceivers() {
        AudioStream stream = scheduledStream(LIVE_ID);
        LiveAlarm broadcasterAlarm = alarm(1L, stream, BROADCASTER_ID);
        given(liveAlarmRepository.findReminderTargets(NOW, NOW.plusMinutes(30)))
                .willReturn(List.of(broadcasterAlarm));
        given(audioStreamRepository.findMemberReminderTargets(NOW, NOW.plusMinutes(30)))
                .willReturn(List.of());
        given(bandMemberPort.getBandSummaryByBandId(BAND_ID))
                .willReturn(Optional.of(new BandSummaryResponse(BAND_ID, BAND_NAME)));
        reminderService.sendUpcomingReminders(NOW);

        verifyNoInteractions(notifyPort);
        assertThat(broadcasterAlarm.getReminderSentAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("팬 알림 대상은 별도 약관 동의 없이 모두 발송 대상에 포함된다")
    void allFanTargetsAreIncludedWithoutTermsAgreement() {
        AudioStream stream = scheduledStream(LIVE_ID);
        LiveAlarm firstAlarm = alarm(1L, stream, 200L);
        LiveAlarm secondAlarm = alarm(2L, stream, 300L);
        given(liveAlarmRepository.findReminderTargets(NOW, NOW.plusMinutes(30)))
                .willReturn(List.of(firstAlarm, secondAlarm));
        given(audioStreamRepository.findMemberReminderTargets(NOW, NOW.plusMinutes(30)))
                .willReturn(List.of());
        given(bandMemberPort.getBandSummaryByBandId(BAND_ID))
                .willReturn(Optional.of(new BandSummaryResponse(BAND_ID, BAND_NAME)));
        reminderService.sendUpcomingReminders(NOW);

        verify(notifyPort).notify(receiverCaptor.capture(), messageCaptor.capture());
        assertThat(receiverCaptor.getValue()).containsExactly(200L, 300L);
        assertThat(firstAlarm.getReminderSentAt()).isEqualTo(NOW);
        assertThat(secondAlarm.getReminderSentAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("활성 밴드 요약이 없으면 발송 없이 발송 시각만 기록해 같은 대상에게 재시도하지 않는다")
    void missingBandSummarySkipsNotifyButMarksSent() {
        AudioStream stream = scheduledStream(LIVE_ID);
        LiveAlarm alarm = alarm(1L, stream, 200L);
        given(liveAlarmRepository.findReminderTargets(NOW, NOW.plusMinutes(30)))
                .willReturn(List.of(alarm));
        given(audioStreamRepository.findMemberReminderTargets(NOW, NOW.plusMinutes(30)))
                .willReturn(List.of(stream));
        given(bandMemberPort.getBandSummaryByBandId(BAND_ID)).willReturn(Optional.empty());

        reminderService.sendUpcomingReminders(NOW);

        verifyNoInteractions(notifyPort);
        assertThat(alarm.getReminderSentAt()).isEqualTo(NOW);
        assertThat(stream.getMemberReminderSentAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("리마인드 대상 라이브가 없으면 아무 포트도 호출하지 않는다")
    void noTargetsMeansNoPortInteractions() {
        given(liveAlarmRepository.findReminderTargets(NOW, NOW.plusMinutes(30)))
                .willReturn(List.of());
        given(audioStreamRepository.findMemberReminderTargets(NOW, NOW.plusMinutes(30)))
                .willReturn(List.of());

        reminderService.sendUpcomingReminders(NOW);

        verifyNoInteractions(bandMemberPort, notifyPort);
    }

    @Test
    @DisplayName("정회원 목록에 중복이 있어도 밴드 리마인드는 한 번씩만 발송된다")
    void duplicateMemberIdsAreDeduplicated() {
        AudioStream stream = scheduledStream(LIVE_ID);
        given(liveAlarmRepository.findReminderTargets(NOW, NOW.plusMinutes(30)))
                .willReturn(List.of());
        given(audioStreamRepository.findMemberReminderTargets(NOW, NOW.plusMinutes(30)))
                .willReturn(List.of(stream));
        given(bandMemberPort.getBandSummaryByBandId(BAND_ID))
                .willReturn(Optional.of(new BandSummaryResponse(BAND_ID, BAND_NAME)));
        given(bandMemberPort.getAcceptedMemberUserIds(BAND_ID))
                .willReturn(List.of(400L, 400L, 500L));

        reminderService.sendUpcomingReminders(NOW);

        verify(notifyPort).notify(receiverCaptor.capture(), messageCaptor.capture());
        assertThat(receiverCaptor.getValue()).containsExactly(400L, 500L);
    }

    @Test
    @DisplayName("여러 라이브가 겹치면 라이브별로 팬 대상을 나눠 각각 발송한다")
    void groupsFanTargetsPerStream() {
        AudioStream stream1 = scheduledStream(LIVE_ID);
        AudioStream stream2 = StreamFixtures.scheduledStream(56L, 101L, 8L, SCHEDULED_AT);
        LiveAlarm alarmForStream1 = alarm(1L, stream1, 200L);
        LiveAlarm alarmForStream2 = alarm(2L, stream2, 300L);
        given(liveAlarmRepository.findReminderTargets(NOW, NOW.plusMinutes(30)))
                .willReturn(List.of(alarmForStream1, alarmForStream2));
        given(audioStreamRepository.findMemberReminderTargets(NOW, NOW.plusMinutes(30)))
                .willReturn(List.of());
        given(bandMemberPort.getBandSummaryByBandId(BAND_ID))
                .willReturn(Optional.of(new BandSummaryResponse(BAND_ID, BAND_NAME)));
        given(bandMemberPort.getBandSummaryByBandId(8L))
                .willReturn(Optional.of(new BandSummaryResponse(8L, "다른밴드")));
        reminderService.sendUpcomingReminders(NOW);

        verify(notifyPort, times(2)).notify(receiverCaptor.capture(), messageCaptor.capture());
        List<List<Long>> receivers = receiverCaptor.getAllValues();
        List<PushMessage> messages = messageCaptor.getAllValues();
        assertThat(receivers.get(0)).containsExactly(200L);
        assertThat(((StreamPushMessage) messages.get(0)).referenceId()).isEqualTo(LIVE_ID);
        assertThat(receivers.get(1)).containsExactly(300L);
        assertThat(((StreamPushMessage) messages.get(1)).referenceId()).isEqualTo(56L);
        assertThat(((StreamPushMessage) messages.get(1)).title()).contains("다른밴드");
    }

    private static AudioStream scheduledStream(Long id) {
        return StreamFixtures.scheduledStream(id, BROADCASTER_ID, BAND_ID, SCHEDULED_AT);
    }

    private static LiveAlarm alarm(Long id, AudioStream stream, Long userId) {
        return LiveAlarm.builder()
                .id(id)
                .audioStream(stream)
                .user(StreamFixtures.fanUser(userId))
                .build();
    }
}
