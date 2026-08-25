package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.band.dto.BandVerifyMessage;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandCreationRequest;
import com.umc.bscene.domain.band.port.DiscordVerifyPort;
import com.umc.bscene.domain.band.repository.BandCreationRequestRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BandVerifyMessenger {

    private final BandCreationRequestRepository bandCreationRequestRepository;
    private final DiscordVerifyPort discordVerifyPort;

    // createBand 커밋 후(afterCommit) 또는 재시도 스케줄러에서 호출.
    // Discord 왕복(블로킹)을 트랜잭션·요청 스레드 밖에서 수행하기 위해 @Async + 무트랜잭션으로 실행하고,
    // 조회는 fetch join으로(지연 로딩 불필요), 메시지 ID 저장은 조건부 UPDATE(원자적 claim)로 처리한다.
    // 전송 실패가 밴드 생성 자체에 영향을 주지 않도록 예외를 밖으로 던지지 않는다
    @Async
    public void sendVerifyMessage(Long creationRequestId) {
        try {
            BandCreationRequest creationRequest = bandCreationRequestRepository
                    .findWithBandById(creationRequestId)
                    .orElse(null);

            if (creationRequest == null || creationRequest.getBand() == null) {
                log.error("검수 요청을 찾을 수 없어 Discord 메시지를 보내지 못함. requestId = {}", creationRequestId);
                return;
            }

            // 이미 전송됐거나 처리된 요청은 스킵 - 커밋 훅과 재시도 스케줄러가 겹칠 때의 1차 방어선
            if (creationRequest.getDiscordMessageId() != null || creationRequest.getResolvedAt() != null) {
                return;
            }

            String messageId = discordVerifyPort.sendVerifyMessage(toMessage(creationRequest));
            if (messageId == null) {
                log.error("Discord 검수 메시지 전송 실패 — 재전송 필요. requestId = {}", creationRequestId);
                return;
            }

            // 2차 방어선: 위 체크 이후 다른 전송이 끼어들었으면 0이 반환된다 (동시 전송 감지)
            int claimed = bandCreationRequestRepository
                    .attachDiscordMessageIfUnsent(creationRequestId, messageId);
            if (claimed == 0) {
                log.warn("검수 메시지가 이미 다른 전송에서 등록됨 - 중복 카드 수동 정리 필요. "
                        + "requestId = {}, orphanMessageId = {}", creationRequestId, messageId);
            }
        } catch (Exception e) {
            log.error("Discord 검수 메시지 처리 중 오류. requestId = {}", creationRequestId, e);
        }
    }

    // PENDING 중 밴드 정보가 수정되면 검수 카드도 갱신 - 운영진이 옛 정보를 보고 승인하는 것 방지.
    // updateBandProfile 커밋 후(afterCommit) 호출되므로 커밋된 최신 데이터를 읽는다
    @Async
    public void updateVerifyMessage(Long bandId) {
        try {
            BandCreationRequest creationRequest = bandCreationRequestRepository
                    .findWithBandByBandIdAndResolvedAtIsNull(bandId)
                    .orElse(null);

            if (creationRequest == null
                    || creationRequest.getBand() == null
                    || creationRequest.getDiscordMessageId() == null) {
                return;
            }

            discordVerifyPort.updateVerifyMessage(
                    creationRequest.getDiscordMessageId(),
                    toMessage(creationRequest)
            );
        } catch (Exception e) {
            log.error("Discord 검수 메시지 갱신 중 오류. bandId = {}", bandId, e);
        }
    }

    private BandVerifyMessage toMessage(BandCreationRequest creationRequest) {
        Band band = creationRequest.getBand();
        return new BandVerifyMessage(
                creationRequest.getId(),
                band.getName(),
                band.getGenre().getName(),
                band.getRegion().getName(),
                band.getDescription(),
                band.getProfileImageUrl()
        );
    }
}
