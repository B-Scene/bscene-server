package com.umc.bscene.domain.session.config;

import com.umc.bscene.domain.session.adapter.BandAdapter;
import com.umc.bscene.domain.session.adapter.UserAdapter;
import com.umc.bscene.domain.session.repository.SessionApplicationSubmissionRepository;
import com.umc.bscene.domain.session.repository.SessionBasicProfileRepository;
import com.umc.bscene.domain.session.scheduler.SessionRecruitmentReminderScheduler;
import com.umc.bscene.domain.session.service.SessionRecruitmentReminderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SessionConfig {

    @Bean
    public com.umc.bscene.domain.band.port.SessionPort SessionBandAdapter(SessionApplicationSubmissionRepository sessionApplicationSubmissionRepository) {
        return new BandAdapter(sessionApplicationSubmissionRepository);
    }

    @Bean
    public com.umc.bscene.domain.user.port.SessionPort SessionUserAdapter(
            SessionApplicationSubmissionRepository sessionApplicationSubmissionRepository,
            SessionBasicProfileRepository sessionBasicProfileRepository
    ) {
        return new UserAdapter(
                sessionApplicationSubmissionRepository,
                sessionBasicProfileRepository
        );
    }

    // 세션 모집 마감 24시간 전 알림 발송 스케줄러
    @Bean
    public SessionRecruitmentReminderScheduler sessionRecruitmentReminderScheduler(
            SessionRecruitmentReminderService reminderService
    ) {
        return new SessionRecruitmentReminderScheduler(reminderService);
    }
}