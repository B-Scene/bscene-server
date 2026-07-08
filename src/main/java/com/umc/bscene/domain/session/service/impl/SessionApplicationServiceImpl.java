package com.umc.bscene.domain.session.service.impl;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandMemberType;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.repository.SessionApplicationRepository;
import com.umc.bscene.domain.session.service.SessionApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionApplicationServiceImpl implements SessionApplicationService {

    private final SessionApplicationRepository sessionApplicationRepository;
    private final BandMemberRepository bandMemberRepository;

    @Override
    public Long updateStatus(Long applicationId, Boolean isApproved) {
        SessionApplication application = sessionApplicationRepository.findById(applicationId)
                .orElseThrow();

        if (isApproved) {
            application.accept();

            Band band = application.getSessionRecruitment().getBand();

            BandMember bandMember = BandMember.builder()
                    .band(band)
                    .user(application.getUser())
                    .bandProfile(application.getBandProfile())
                    .status(BandMemberStatus.ACCEPTED)
                    .memberType(BandMemberType.SESSION_MEMBER)
                    .build();

            bandMemberRepository.save(bandMember);

            return band.getId();
        }

        application.reject();

        return application.getSessionRecruitment().getBand().getId();
    }
}