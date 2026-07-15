package com.umc.bscene.domain.performance.service;

import com.umc.bscene.domain.performance.dto.PerformancePushMessage;
import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.performance.entity.PerformanceParticipation;
import com.umc.bscene.domain.performance.enums.ParticipationStatus;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.domain.performance.port.NotifyPort;
import com.umc.bscene.domain.performance.repository.PerformanceParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PerformanceReminderService {

    private final PerformanceParticipationRepository performanceParticipationRepository;
    private final NotifyPort notifyPort;

    // 현재부터 1시간 이내에 시작하는 공연의 알림 설정 사용자에게 푸시 알림 발송
    @Transactional
    public void sendUpcomingReminders(LocalDateTime now) {
        LocalDateTime reminderLimit = now.plusHours(1);

        List<PerformanceParticipation> targets =
                performanceParticipationRepository.findReminderTargets(
                        ParticipationStatus.SCHEDULED,
                        PerformanceStatus.ACTIVE,
                        now.toLocalDate(),
                        now.toLocalTime(),
                        reminderLimit.toLocalDate(),
                        reminderLimit.toLocalTime()
                );

        Map<Long, List<PerformanceParticipation>> targetsByPerformanceId =
                targets.stream()
                        .collect(Collectors.groupingBy(
                                target -> target.getPerformance().getId()
                        ));

        for (List<PerformanceParticipation> performanceTargets
                : targetsByPerformanceId.values()) {
            sendReminder(performanceTargets, now);
        }
    }

    // 같은 공연을 설정한 사용자들을 모아 한 번의 알림 발송 요청으로 전달
    private void sendReminder(
            List<PerformanceParticipation> targets,
            LocalDateTime sentAt
    ) {
        Performance performance = targets.getFirst().getPerformance();

        List<Long> receiverIds = targets.stream()
                .map(target -> target.getUser().getId())
                .toList();

        PerformancePushMessage message = PerformancePushMessage.reminder(
                performance.getBand().getName(),
                performance.getTitle(),
                performance.getId()
        );

        notifyPort.notify(receiverIds, message);

        targets.forEach(target -> target.markReminderSent(sentAt));
    }
}