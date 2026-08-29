package com.umc.bscene.domain.stream.adapter;

import com.umc.bscene.domain.band.port.StreamPort;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.domain.stream.enums.code.error.StreamErrorCode;
import com.umc.bscene.domain.stream.exception.StreamException;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
public class BandAdapter implements StreamPort {

    private final AudioStreamRepository audioStreamRepository;

    @Override
    public Optional<Long> findOpenLiveId(Long bandId) {
        AudioStream audioStream = audioStreamRepository.findByBandIdAndStatus(bandId, StreamStatus.OPEN)
                .orElse(null);

        if (audioStream == null) {
            return Optional.empty();
        }
        return audioStream.getId().describeConstable();
    }

    @Override
    public boolean hasLiveHistory(Long bandId) {
        return audioStreamRepository.existsByBandId(bandId);
    }
}
