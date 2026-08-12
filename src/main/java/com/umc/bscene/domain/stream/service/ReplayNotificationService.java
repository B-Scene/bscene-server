package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.StreamPushMessage;
import com.umc.bscene.domain.stream.dto.response.BandSummaryResponse;
import com.umc.bscene.domain.stream.entity.AudioStream;
import com.umc.bscene.domain.stream.port.BandMemberPort;
import com.umc.bscene.domain.stream.port.FollowPort;
import com.umc.bscene.domain.stream.port.NotifyPort;
import com.umc.bscene.domain.stream.repository.AudioStreamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReplayNotificationService {

    private final AudioStreamRepository audioStreamRepository;
    private final BandMemberPort bandMemberPort;
    private final FollowPort followPort;
    private final NotifyPort notifyPort;

    // 모든 다시보기 세그먼트 등록 완료 후 팔로워에게 한 번만 알림 발송
    @Transactional
    public void notifyReplayReady(AudioStream stream) {
        Optional<BandSummaryResponse> bandSummary =
                bandMemberPort.getBandSummaryByBandId(stream.getBandId());

        List<Long> receiverIds = bandSummary
                .map(band -> followPort.getFollowerUserIdsByBandId(band.bandId()))
                .filter(followerIds -> !followerIds.isEmpty())
                .orElseGet(List::of);

        int marked = audioStreamRepository.markReplayNotificationSentIfAbsent(
                stream.getId(),
                LocalDateTime.now()
        );

        if (marked == 0 || receiverIds.isEmpty()) {
            return;
        }

        BandSummaryResponse band = bandSummary.orElseThrow();
        StreamPushMessage message = StreamPushMessage.replayReady(
                band.bandName(),
                stream.getTitle(),
                stream.getId()
        );

        notifyPort.notify(receiverIds, message);
    }
}
