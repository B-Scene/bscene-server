package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.band.dto.BandPushMessage;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandCreationRequest;
import com.umc.bscene.domain.band.enums.BandStatus;
import com.umc.bscene.domain.band.port.FollowPort;
import com.umc.bscene.domain.band.port.NotifyPort;
import com.umc.bscene.domain.band.repository.BandCreationRequestRepository;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.repository.MusicLinkRepository;
import com.umc.bscene.domain.search.event.BandChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Discord 검수(실존 확인) 수락/거절 처리
@Slf4j
@Service
@RequiredArgsConstructor
public class BandVerifyService {

    private final BandCreationRequestRepository bandCreationRequestRepository;
    private final BandRepository bandRepository;
    private final BandMemberRepository bandMemberRepository;
    private final MusicLinkRepository musicLinkRepository;
    private final FollowPort followPort;
    private final NotifyPort notifyPort;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 검수 수락. PENDING 요청만 처리되므로 운영진의 중복/동시 클릭에도 한 번만 반영된다.
     * 동명의 기존 ACCEPTED 밴드(더미)가 있으면 삭제 후 교체한다.
     *
     * @return 승인된 bandId. 이미 처리된 요청이면 empty
     */
    @Transactional
    public Optional<Long> accept(Long creationRequestId, String processedBy) {
        BandCreationRequest creationRequest = bandCreationRequestRepository
                .findPendingById(creationRequestId)
                .orElse(null);
        if (creationRequest == null) {
            return Optional.empty();
        }

        Band band = creationRequest.getBand();

        bandRepository.findByNameAndStatus(band.getName(), BandStatus.ACCEPTED)
                .ifPresent(existingBand -> {
                    Long existingBandId = existingBand.getId();
                    deleteBand(existingBand);
                    // Hibernate는 UPDATE를 DELETE보다 먼저 내보내므로, 삭제를 먼저 flush하지 않으면
                    // 아래 band.accept()의 상태 변경이 (name, ACCEPTED) 복합 유니크를 위반한다
                    bandRepository.flush();
                    // 삭제된 기존 밴드의 검색 문서 정리 (색인 대상이 없으면 문서 삭제됨)
                    eventPublisher.publishEvent(new BandChangedEvent(existingBandId));
                });

        band.accept();
        creationRequest.markAccepted(processedBy);

        eventPublisher.publishEvent(new BandChangedEvent(band.getId()));
        notifyAfterCommit(creationRequest.getRequesterId(), BandPushMessage.verifyAccepted(band.getId()));

        return Optional.of(band.getId());
    }

    /**
     * 수락 확인 단계용: 이 요청을 수락하면 교체(삭제)될 동명 ACCEPTED 밴드가 있는지 확인.
     * 요청이 없거나 이미 처리된 경우 false — 실제 처리 가능 여부는 accept()가 다시 판단한다.
     */
    @Transactional(readOnly = true)
    public boolean hasAcceptedSameName(Long creationRequestId) {
        BandCreationRequest creationRequest = bandCreationRequestRepository
                .findById(creationRequestId)
                .orElse(null);
        if (creationRequest == null || creationRequest.getBand() == null) {
            return false;
        }
        return bandRepository.existsByNameAndStatus(
                creationRequest.getBand().getName(),
                BandStatus.ACCEPTED
        );
    }

    /**
     * 검수 거절. 밴드 row는 삭제하고 요청에 사유·처리자 이력만 남긴다.
     *
     * @return 처리됐으면 true, 이미 처리된 요청이면 false
     */
    @Transactional
    public boolean reject(Long creationRequestId, String rejectedReason, String processedBy) {
        BandCreationRequest creationRequest = bandCreationRequestRepository
                .findPendingById(creationRequestId)
                .orElse(null);
        if (creationRequest == null) {
            return false;
        }

        Band band = creationRequest.getBand();
        Long bandId = band.getId();
        Long requesterId = creationRequest.getRequesterId();

        creationRequest.markRejected(rejectedReason, processedBy);
        deleteBand(band);

        // 수정 API 등으로 색인됐을 수 있는 문서 정리
        eventPublisher.publishEvent(new BandChangedEvent(bandId));
        notifyAfterCommit(requesterId, BandPushMessage.verifyRejected(creationRequestId));

        return true;
    }

    // 검수 플로우 한정 삭제 — PENDING 밴드/활동 없는 더미가 가진 연관(멤버, 음악 링크, 팔로우)만 정리한다.
    // 게시물·공연 등 활동 데이터가 있는 밴드는 FK 제약으로 삭제가 실패하고 전체가 롤백된다(의도된 안전장치).
    private void deleteBand(Band band) {
        bandMemberRepository.deleteByBand_Id(band.getId());
        musicLinkRepository.deleteByBand_Id(band.getId());
        followPort.deleteAllByBandId(band.getId());
        bandRepository.delete(band);
    }

    private void notifyAfterCommit(Long receiverId, BandPushMessage message) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notifyPort.notify(receiverId, message);
                    }
                }
        );
    }
}
