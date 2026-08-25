package com.umc.bscene.domain.band.service;

import com.umc.bscene.domain.band.dto.BandPushMessage;
import com.umc.bscene.domain.band.dto.BandVerifyAcceptResult;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandCreationRequest;
import com.umc.bscene.domain.band.entity.BandMember;
import com.umc.bscene.domain.band.entity.BandMemberProfile;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.enums.BandStatus;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.port.FollowPort;
import com.umc.bscene.domain.band.port.NotifyPort;
import com.umc.bscene.domain.band.port.PostCommentPort;
import com.umc.bscene.domain.band.port.RecommendationPort;
import com.umc.bscene.domain.band.port.StreamPort;
import com.umc.bscene.domain.band.repository.BandCreationRequestRepository;
import com.umc.bscene.domain.band.repository.BandInviteLinkRepository;
import com.umc.bscene.domain.band.repository.BandMemberProfileRepository;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.repository.MusicLinkRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.search.event.BandChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
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
    private final BandMemberProfileRepository bandMemberProfileRepository;
    private final MusicLinkRepository musicLinkRepository;
    private final BandInviteLinkRepository bandInviteLinkRepository;
    private final FollowPort followPort;
    private final StreamPort streamPort;
    private final NotifyPort notifyPort;
    private final PostCommentPort postCommentPort;
    private final RecommendationPort recommendationPort;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 검수 수락. PENDING 요청만 처리되므로 운영진의 중복/동시 클릭에도 한 번만 반영된다.
     * 동명의 기존 ACCEPTED 밴드가 있으면 replaceConfirmed=true일 때만 삭제 후 교체하고,
     * 아니면 대상 밴드 정보를 담아 NEEDS_REPLACE_CONFIRM을 반환한다.
     * 교체 여부 판정이 이 트랜잭션(비관적 락) 안에서 이루어지므로,
     * 확인 카드가 뜬 뒤 상황이 바뀌어도 확인 없이 삭제되는 창이 없다.
     */
    @Transactional
    public BandVerifyAcceptResult accept(Long creationRequestId, String processedBy, boolean replaceConfirmed) {
        BandCreationRequest creationRequest = bandCreationRequestRepository
                .findPendingById(creationRequestId)
                .orElse(null);
        if (creationRequest == null) {
            return BandVerifyAcceptResult.alreadyProcessed();
        }

        Band band = creationRequest.getBand();

        Optional<Band> existingSameName = bandRepository
                .findByNameAndStatus(band.getName(), BandStatus.ACCEPTED);

        if (existingSameName.isPresent() && !replaceConfirmed) {
            Band existing = existingSameName.get();
            // 운영진이 더미인지 실제 활동 밴드인지 판단할 수 있도록 대상 정보를 함께 반환
            return BandVerifyAcceptResult.needsReplaceConfirm(new BandVerifyAcceptResult.ReplaceTarget(
                    existing.getId(),
                    existing.getName(),
                    bandMemberRepository.countByBand_IdAndStatus(existing.getId(), BandMemberStatus.ACCEPTED),
                    followPort.countFollowersByBandId(existing.getId()),
                    existing.getCreatedAt()
            ));
        }

        existingSameName.ifPresent(existingBand -> {
            Long existingBandId = existingBand.getId();
            Long displacedOwnerId = existingBand.getOwner().getId();
            String displacedBandName = existingBand.getName();

            deleteBand(existingBand);
            // Hibernate는 UPDATE를 DELETE보다 먼저 내보내므로, 삭제를 먼저 flush하지 않으면
            // 아래 band.accept()의 상태 변경이 (name, ACCEPTED) 복합 유니크를 위반한다
            bandRepository.flush();
            // 삭제된 기존 밴드의 검색 문서 정리 (색인 대상이 없으면 문서 삭제됨)
            eventPublisher.publishEvent(new BandChangedEvent(existingBandId));
            // 교체로 삭제된 밴드의 오너에게 알림 - 대상이 더미가 아닌 실제 밴드였던 경우를 대비한 안전장치
            notifyAfterCommit(displacedOwnerId, BandPushMessage.bandReplaced(displacedBandName, band.getId()));
        });

        band.accept();
        creationRequest.markAccepted(processedBy);

        eventPublisher.publishEvent(new BandChangedEvent(band.getId()));
        notifyAfterCommit(creationRequest.getRequesterId(), BandPushMessage.verifyAccepted(band.getId()));

        return BandVerifyAcceptResult.accepted(band.getId());
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

    // 검수 플로우 한정 삭제 — 검수 전(PENDING) 밴드가 만들 수 있는 연관 데이터
    // (명의 댓글, 멤버, 고아 멤버 프로필, 음악 링크, 팔로우, 초대 링크, 추천 파생 데이터, 과거 검수 이력 FK)를 정리한다.
    // 게시물·공연·세션모집 등 활동 데이터가 있는 밴드는 FK 제약으로 삭제가 실패하고 전체가 롤백된다(의도된 안전장치).
    // 단, audio_stream.band_id는 FK가 아니라 라이브 이력은 이 그물에 걸리지 않으므로 여기서 직접 차단한다.
    private void deleteBand(Band band) {
        Long bandId = band.getId();

        if (streamPort.hasLiveHistory(bandId)) {
            throw new BandException(BandErrorCode.BAND_HAS_LIVE_HISTORY);
        }

        deleteMembersAndOrphanProfiles(bandId);
        musicLinkRepository.deleteByBand_Id(bandId);
        followPort.deleteAllByBandId(bandId);
        bandInviteLinkRepository.deleteByBand_Id(bandId);
        recommendationPort.deleteAllByBandId(bandId);
        // 검수를 통과했던 밴드는 자기 검수 이력이 FK로 남아 삭제가 막히므로 스냅샷(bandName)만 남기고 연결 해제
        bandCreationRequestRepository.detachBandReferences(bandId);
        bandRepository.delete(band);
    }

    // 일반 탈퇴 경로(BandService.deleteBandMemberAndOrphanProfile)와 같은 규칙:
    // 밴드 명의 댓글 -> 멤버 -> 다른 멤버십이 쓰지 않는 고아 프로필 순으로 정리한다
    private void deleteMembersAndOrphanProfiles(Long bandId) {
        List<BandMember> members = bandMemberRepository.findWithProfileByBand_IdOrderByIdAsc(bandId);
        if (members.isEmpty()) {
            return;
        }

        for (BandMember member : members) {
            postCommentPort.deleteBandComments(bandId, member.getUser().getId());
        }

        bandMemberRepository.deleteAll(members);
        bandMemberRepository.flush();

        for (BandMember member : members) {
            BandMemberProfile profile = member.getBandMemberProfile();
            if (profile != null
                    && !bandMemberRepository.existsByBandMemberProfile_Id(profile.getId())) {
                bandMemberProfileRepository.delete(profile);
            }
        }
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
