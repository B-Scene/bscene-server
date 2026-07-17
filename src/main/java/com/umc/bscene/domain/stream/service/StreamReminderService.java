package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.StreamPushMessage;
import com.umc.bscene.domain.stream.dto.response.BandSummaryResponse;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.entity.LiveAlarm;
import com.umc.bscene.domain.stream.port.BandMemberPort;
import com.umc.bscene.domain.stream.port.NotifyPort;
import com.umc.bscene.domain.stream.port.UserTermsPort;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import com.umc.bscene.domain.stream.repository.LiveAlarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class StreamReminderService {

    private final AudioStreamRepository audioStreamRepository;
    private final LiveAlarmRepository liveAlarmRepository;
    private final BandMemberPort bandMemberPort;
    private final UserTermsPort userTermsPort;
    private final NotifyPort notifyPort;

    // 라이브 시작 30분 전 팬과 밴드 구성원에게 리마인드 알림을 발송합니다.
    @Transactional
    public void sendUpcomingReminders(LocalDateTime now) {
        LocalDateTime reminderLimit = now.plusMinutes(30);

        List<LiveAlarm> fanTargets =
                liveAlarmRepository.findReminderTargets(now, reminderLimit);

        List<AudioStream> memberTargets =
                audioStreamRepository.findMemberReminderTargets(now, reminderLimit);

        Map<Long, List<LiveAlarm>> fanTargetsByStreamId = fanTargets.stream()
                .collect(Collectors.groupingBy(
                        target -> target.getAudioStream().getId()
                ));

        Set<Long> memberTargetIds = memberTargets.stream()
                .map(AudioStream::getId)
                .collect(Collectors.toSet());

        // 팬 또는 밴드 구성원 알림 대상에 포함된 라이브를 ID 기준으로 합칩니다.
        Map<Long, AudioStream> streamsById = new LinkedHashMap<>();

        fanTargets.forEach(target ->
                streamsById.putIfAbsent(
                        target.getAudioStream().getId(),
                        target.getAudioStream()
                )
        );

        memberTargets.forEach(stream ->
                streamsById.putIfAbsent(stream.getId(), stream)
        );

        for (AudioStream stream : streamsById.values()) {
            List<LiveAlarm> streamFanTargets =
                    fanTargetsByStreamId.getOrDefault(stream.getId(), List.of());

            sendReminder(
                    stream,
                    streamFanTargets,
                    memberTargetIds.contains(stream.getId()),
                    now
            );
        }
    }

    // 같은 라이브의 팬과 밴드 구성원을 합쳐 한 번의 알림 발송 요청으로 전달합니다.
    private void sendReminder(
            AudioStream stream,
            List<LiveAlarm> fanTargets,
            boolean memberReminderRequired,
            LocalDateTime sentAt
    ) {
        Optional<BandSummaryResponse> bandSummary =
                bandMemberPort.getBandSummaryByBandId(stream.getBandId());

        if (bandSummary.isPresent()) {
            List<Long> fanUserIds = fanTargets.stream()
                    .map(target -> target.getUser().getId())
                    .toList();

            List<Long> agreedFanIds =
                    userTermsPort.filterNotificationAgreedUserIds(fanUserIds);

            List<Long> memberIds = memberReminderRequired
                    ? bandMemberPort.getAcceptedMemberUserIds(stream.getBandId())
                    : List.of();

            List<Long> receiverIds = Stream.concat(
                            agreedFanIds.stream(),
                            memberIds.stream()
                    )
                    .filter(userId -> !userId.equals(stream.getBroadcasterId()))
                    .distinct()
                    .toList();

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

        // 팬 알림 설정별 발송 완료 상태를 기록합니다.
        fanTargets.forEach(target -> target.markReminderSent(sentAt));

        // 밴드 구성원 대상 발송 완료 상태는 라이브 단위로 기록합니다.
        if (memberReminderRequired) {
            stream.markMemberReminderSent(sentAt);
        }
    }
}
