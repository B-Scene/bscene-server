package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.StreamPushMessage;
import com.umc.bscene.domain.stream.dto.response.BandSummaryResponse;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.entity.LiveAlarm;
import com.umc.bscene.domain.stream.port.BandMemberPort;
import com.umc.bscene.domain.stream.port.NotifyPort;
import com.umc.bscene.domain.stream.port.UserTermsPort;
import com.umc.bscene.domain.stream.repository.LiveAlarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StreamReminderService {

    private final LiveAlarmRepository liveAlarmRepository;
    private final BandMemberPort bandMemberPort;
    private final UserTermsPort userTermsPort;
    private final NotifyPort notifyPort;

    // 현재부터 30분 이내에 시작하는 라이브의 알림 설정 사용자에게 리마인드 발송
    @Transactional
    public void sendUpcomingReminders(LocalDateTime now) {
        LocalDateTime reminderLimit = now.plusMinutes(30);

        List<LiveAlarm> targets = liveAlarmRepository.findReminderTargets(now, reminderLimit);

        Map<Long, List<LiveAlarm>> targetsByStreamId = targets.stream()
                .collect(Collectors.groupingBy(target -> target.getAudioStream().getId()));

        for (List<LiveAlarm> streamTargets : targetsByStreamId.values()) {
            sendReminder(streamTargets, now);
        }
    }

    // 같은 라이브에 알림을 설정한 사용자들을 모아 한 번의 알림 발송 요청으로 전달
    private void sendReminder(List<LiveAlarm> targets, LocalDateTime sentAt) {
        AudioStream stream = targets.getFirst().getAudioStream();
        Optional<BandSummaryResponse> bandSummary =
                bandMemberPort.getBandSummaryByBandId(stream.getBandId());

        if (bandSummary.isPresent()) {
            List<Long> userIds = targets.stream()
                    .map(target -> target.getUser().getId())
                    .toList();

            List<Long> receiverIds = userTermsPort.filterNotificationAgreedUserIds(userIds);

            if (!receiverIds.isEmpty()) {
                BandSummaryResponse band = bandSummary.get();
                StreamPushMessage message = StreamPushMessage.reminder(
                        band.bandName(),
                        stream.getTitle(),
                        stream.getId()
                );

                notifyPort.notify(receiverIds, message);
            }
        }

        // 수신 대상이 없더라도 매분 같은 알림을 다시 조회하지 않도록 처리 완료 시각 기록
        targets.forEach(target -> target.markReminderSent(sentAt));
    }
}
