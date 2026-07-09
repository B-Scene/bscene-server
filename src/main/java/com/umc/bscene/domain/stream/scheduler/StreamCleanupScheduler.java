package com.umc.bscene.domain.stream.scheduler;

import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class StreamCleanupScheduler {

    private final AudioStreamRepository audioStreamRepository;

    private static final Duration GRACE = Duration.ofSeconds(30);

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cancelAbandonedScheduled() {
        LocalDateTime threshold = LocalDateTime.now().minus(GRACE);
        List<AudioStream> abandoned = audioStreamRepository.findAbandonedScheduled(threshold);

        abandoned.forEach(AudioStream::cancel);
    }
}
