package com.umc.bscene.domain.stream.adapter;

import com.umc.bscene.domain.band.port.StreamPort;
import com.umc.bscene.domain.stream.enums.StreamStatus;
import com.umc.bscene.domain.stream.enums.code.error.StreamErrorCode;
import com.umc.bscene.domain.stream.exception.StreamException;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class BandAdapter implements StreamPort {

    private final AudioStreamRepository audioStreamRepository;

    @Override
    public Optional<Long> findOpenLiveId(Long bandId) {
        return Optional.of(
                audioStreamRepository.findByBandIdAndStatus(bandId, StreamStatus.OPEN)
                .orElseThrow(() -> new StreamException(StreamErrorCode.AUDIO_STREAM_NOT_FOUND))
                .getId()
        );
    }
}
