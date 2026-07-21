package com.umc.bscene.domain.performance.service;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.performance.dto.PerformancePushMessage;
import com.umc.bscene.domain.performance.dto.request.PerformanceCreateRequest;
import com.umc.bscene.domain.performance.dto.request.PerformanceUpdateRequest;
import com.umc.bscene.domain.performance.dto.response.PerformanceDetailResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceListResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceSummaryResponse;
import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.performance.enums.ParticipationStatus;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.domain.performance.exception.PerformanceException;
import com.umc.bscene.domain.performance.port.FollowPort;
import com.umc.bscene.domain.performance.port.NotifyPort;
import com.umc.bscene.domain.performance.repository.PerformanceInterestRepository;
import com.umc.bscene.domain.performance.repository.PerformanceParticipationRepository;
import com.umc.bscene.domain.performance.repository.PerformanceRepository;
import com.umc.bscene.domain.performance.response.code.PerformanceErrorCode;
import com.umc.bscene.domain.search.event.PerformanceChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {

    private static final int MAX_TAG_COUNT = 8;

    private final PerformanceRepository performanceRepository;
    private final PerformanceInterestRepository performanceInterestRepository;
    private final BandRepository bandRepository;
    private final BandMemberRepository bandMemberRepository;
    private final FollowPort followPort;
    private final NotifyPort notifyPort;
    private final ApplicationEventPublisher eventPublisher;
    private final PerformanceParticipationRepository performanceParticipationRepository;

    // 공연 등록 (밴드 멤버만 가능, 지난 날짜 등록 불가)
    @Transactional
    public PerformanceResponse createPerformance(Long userId, Long bandId, PerformanceCreateRequest request) {
        Band band = getBand(bandId);
        validateBandMember(band, userId);
        validateNotPastDate(request.performanceDate());
        validateTagCount(request.tags());

        Performance performance = Performance.builder()
                .band(band)
                .title(request.title())
                .genre(request.genre())
                .performanceDate(request.performanceDate())
                .startTime(request.startTime())
                .region(request.region())
                .venue(request.venue())
                .description(request.description())
                .ticketPrice(request.ticketPrice())
                .ticketLink(request.ticketLink())
                .posterImageUrl(request.posterImageUrl())
                .ageRating(request.ageRating())
                .build();

        addTagList(performance, request.tags());

        Performance savedPerformance = performanceRepository.save(performance);

        // 검색 색인 동기화 (커밋 후 search 도메인 리스너가 비동기 처리)
        eventPublisher.publishEvent(new PerformanceChangedEvent(savedPerformance.getId()));

        notifyFollowersAfterCommit(savedPerformance);

        return PerformanceResponse.of(savedPerformance, 0L, false);
    }

    // 공연 등록 트랜잭션이 완료된 후 밴드 팔로워에게 알림을 발송
    private void notifyFollowersAfterCommit(Performance performance) {
        List<Long> receiverIds = followPort.getFollowerUserIdsByBandId(
                performance.getBand().getId()
        );

        if (receiverIds.isEmpty()) {
            return;
        }

        PerformancePushMessage message = PerformancePushMessage.created(
                performance.getBand().getName(),
                performance.getTitle(),
                performance.getId()
        );

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notifyPort.notify(receiverIds, message);
                    }
                }
        );
    }

    // 밴드의 공연 목록 조회
    public PerformanceListResponse getPerformances(Long bandId) {
        getBand(bandId);

        List<PerformanceSummaryResponse> performances = performanceRepository
                .findByBand_IdAndStatusOrderByPerformanceDateAsc(bandId, PerformanceStatus.ACTIVE).stream()
                .map(PerformanceSummaryResponse::from)
                .toList();

        return PerformanceListResponse.from(performances);
    }

    // 공연 상세 조회 (관심 등록 수, 요청자의 관심 등록 여부 포함)
    public PerformanceResponse getPerformanceDetail(Long userId, Long performanceId) {
        Performance performance = getActivePerformance(performanceId);

        long interestCount = performanceInterestRepository.countByPerformance_Id(performanceId);
        boolean isInterested = performanceInterestRepository.existsByPerformance_IdAndUser_Id(performanceId, userId);

        return PerformanceResponse.of(performance, interestCount, isInterested);
    }

    // 팬모드 공연 상세페이지 조회 : 공연정보/공연소개/캐스팅 + 관심·알림 버튼 상태
    public PerformanceDetailResponse getPerformanceDetailPage(Long userId, Long performanceId) {
        Performance performance = getActivePerformance(performanceId);

        long interestCount = performanceInterestRepository.countByPerformance_Id(performanceId);
        boolean isInterested = performanceInterestRepository.existsByPerformance_IdAndUser_Id(performanceId, userId);

        // 알림 종 아이콘 상태 : 참여 기록이 없으면 null (알림 미설정)
        String participationStatus = performanceParticipationRepository
                .findByPerformance_IdAndUser_Id(performanceId, userId)
                .map(participation -> participation.getStatus().name())
                .orElse(null);

        return PerformanceDetailResponse.of(performance, interestCount, isInterested, participationStatus);
    }

    // 공연 정보 수정 (등록한 밴드의 멤버만 가능)
    @Transactional
    public PerformanceResponse updatePerformance(Long userId, Long performanceId, PerformanceUpdateRequest request) {
        Performance performance = getActivePerformance(performanceId);
        validateBandMember(performance.getBand(), userId);
        if (request.performanceDate() != null) {
            validateNotPastDate(request.performanceDate());
        }
        validateTagCount(request.tags());

        performance.update(
                request.title(),
                request.genre(),
                request.performanceDate(),
                request.startTime(),
                request.venue(),
                request.ticketPrice(),
                request.ticketLink(),
                request.posterImageUrl(),
                request.ageRating()
        );

        if (request.tags() != null) {
            performance.clearTags();
            addTagList(performance, request.tags());
        }

        // 검색 색인 동기화 (커밋 후 search 도메인 리스너가 비동기 처리)
        eventPublisher.publishEvent(new PerformanceChangedEvent(performanceId));

        notifyPerformanceUpdateAfterCommit(performance);

        long interestCount = performanceInterestRepository.countByPerformance_Id(performanceId);
        boolean isInterested = performanceInterestRepository.existsByPerformance_IdAndUser_Id(performanceId, userId);

        return PerformanceResponse.of(performance, interestCount, isInterested);
    }

    // 공연 삭제 (등록한 밴드의 멤버만 가능, soft delete)
    @Transactional
    public void deletePerformance(Long userId, Long performanceId) {
        Performance performance = getActivePerformance(performanceId);
        validateBandMember(performance.getBand(), userId);

        performance.delete();

        // 검색 색인 동기화 (소프트 삭제 → 리스너가 검색 문서 삭제로 처리)
        eventPublisher.publishEvent(new PerformanceChangedEvent(performanceId));
    }

    private Band getBand(Long bandId) {
        return bandRepository.findById(bandId)
                .orElseThrow(() -> new BandException(BandErrorCode.BAND_NOT_FOUND));
    }

    private Performance getActivePerformance(Long performanceId) {
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new PerformanceException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND));

        if (performance.getStatus() != PerformanceStatus.ACTIVE) {
            throw new PerformanceException(PerformanceErrorCode.PERFORMANCE_NOT_FOUND);
        }

        return performance;
    }

    private void validateBandMember(Band band, Long userId) {
        if (!bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(band.getId(), userId, BandMemberStatus.ACCEPTED)) {
            throw new PerformanceException(PerformanceErrorCode.NOT_PERFORMANCE_BAND_MEMBER);
        }
    }

    private void validateNotPastDate(LocalDate performanceDate) {
        if (performanceDate.isBefore(LocalDate.now())) {
            throw new PerformanceException(PerformanceErrorCode.PAST_DATE_NOT_ALLOWED);
        }
    }

    private void addTagList(Performance performance, List<String> tags) {
        if (tags == null) return;
        for (String tag : tags) {
            performance.addTag(tag);
        }
    }

    private void validateTagCount(List<String> tags) {
        if (tags != null && tags.size() > MAX_TAG_COUNT) {
            throw new PerformanceException(PerformanceErrorCode.TAG_LIMIT_EXCEEDED);
        }
    }

    // 공연 정보 수정 트랜잭션 완료 후 알림 설정 사용자에게 알림 발송
    private void notifyPerformanceUpdateAfterCommit(Performance performance) {
        List<Long> receiverIds =
                performanceParticipationRepository.findUserIdsByPerformanceIdAndStatus(
                        performance.getId(),
                        ParticipationStatus.SCHEDULED
                );

        if (receiverIds.isEmpty()) {
            return;
        }

        PerformancePushMessage message = PerformancePushMessage.updated(
                performance.getBand().getName(),
                performance.getTitle(),
                performance.getId()
        );

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notifyPort.notify(receiverIds, message);
                    }
                }
        );
    }
}
