package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.band.dto.BandVerifyMessage;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandCreationRequest;
import com.umc.bscene.domain.band.port.DiscordVerifyPort;
import com.umc.bscene.domain.band.repository.BandCreationRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BandVerifyMessenger {

    private final BandCreationRequestRepository bandCreationRequestRepository;
    private final DiscordVerifyPort discordVerifyPort;

    // createBand 커밋 후(afterCommit) 호출 — 이 시점엔 원 트랜잭션이 이미 커밋되어 REQUIRED로 참여하면
    // discordMessageId 변경이 flush되지 않으므로 새 트랜잭션(REQUIRES_NEW)에서 실행한다.
    // 전송 실패가 밴드 생성 자체에 영향을 주지 않도록 예외를 밖으로 던지지 않는다
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendVerifyMessage(Long creationRequestId) {
        try {
            BandCreationRequest creationRequest = bandCreationRequestRepository
                    .findById(creationRequestId)
                    .orElse(null);

            if (creationRequest == null || creationRequest.getBand() == null) {
                log.error("검수 요청을 찾을 수 없어 Discord 메시지를 보내지 못함. requestId = {}", creationRequestId);
                return;
            }

            // 이미 전송됐거나 처리된 요청은 스킵 - 재시도 스케줄러와 커밋 훅 전송이 겹쳐도 중복 발송되지 않는다
            if (creationRequest.getDiscordMessageId() != null || creationRequest.getResolvedAt() != null) {
                return;
            }

            String messageId = discordVerifyPort.sendVerifyMessage(toMessage(creationRequest));
            if (messageId == null) {
                log.error("Discord 검수 메시지 전송 실패 — 재전송 필요. requestId = {}", creationRequestId);
                return;
            }

            creationRequest.attachDiscordMessage(messageId);
        } catch (Exception e) {
            log.error("Discord 검수 메시지 처리 중 오류. requestId = {}", creationRequestId, e);
        }
    }

    // PENDING 중 밴드 정보가 수정되면 검수 카드도 갱신 - 운영진이 옛 정보를 보고 승인하는 것 방지.
    // updateBandProfile 커밋 후(afterCommit) 호출되므로 새 트랜잭션에서 최신 데이터를 읽는다
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public void updateVerifyMessage(Long bandId) {
        try {
            BandCreationRequest creationRequest = bandCreationRequestRepository
                    .findByBand_IdAndResolvedAtIsNull(bandId)
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
