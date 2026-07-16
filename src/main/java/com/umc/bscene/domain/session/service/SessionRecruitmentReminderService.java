package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.session.dto.SessionPushMessage;
import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.port.BandMemberPort;
import com.umc.bscene.domain.session.port.NotifyPort;
import com.umc.bscene.domain.session.repository.SessionRecruitmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionRecruitmentReminderService {

    private final SessionRecruitmentRepository sessionRecruitmentRepository;
    private final BandMemberPort bandMemberPort;
    private final NotifyPort notifyPort;

    // 현재부터 24시간 이내에 마감되는 모집 공고 알림 발송
    @Transactional
    public void sendDeadlineReminders(LocalDateTime now) {
        LocalDateTime reminderLimit = now.plusHours(24);

        List<SessionRecruitment> targets =
                sessionRecruitmentRepository.findDeadlineReminderTargets(
                        now,
                        reminderLimit
                );

        for (SessionRecruitment recruitment : targets) {
            sendReminder(recruitment, now);
        }
    }

    // 해당 모집 공고의 밴드 구성원 전체에게 마감 임박 알림 발송
    private void sendReminder(
            SessionRecruitment recruitment,
            LocalDateTime sentAt
    ) {
        List<Long> receiverIds = bandMemberPort.getAcceptedMemberUserIds(
                recruitment.getBand().getId()
        );

        if (!receiverIds.isEmpty()) {
            SessionPushMessage message = SessionPushMessage.deadlineReminder(
                    recruitment.getRecruitmentTitle(),
                    recruitment.getSessionRecruitmentId()
            );

            notifyPort.notify(receiverIds, message);
        }

        // 수신자가 없어도 매분 반복 조회되지 않도록 처리 완료 시각 기록
        recruitment.markDeadlineReminderSent(sentAt);
    }
}