package com.umc.bscene.domain.session.adapter;

import com.umc.bscene.domain.band.port.SessionPort;
import com.umc.bscene.domain.session.repository.SessionApplicationSubmissionRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class BandAdapter implements SessionPort {

    private final SessionApplicationSubmissionRepository sessionApplicationSubmissionRepository;

    @Override
    public long getActiveSessionApplicantCount(Long bandId) {
        return sessionApplicationSubmissionRepository.countActiveApplicantsByBandId(bandId, LocalDateTime.now());
    }
}
