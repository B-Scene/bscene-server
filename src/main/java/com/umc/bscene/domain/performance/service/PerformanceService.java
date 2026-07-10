package com.umc.bscene.domain.performance.service;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.performance.dto.request.PerformanceCreateRequest;
import com.umc.bscene.domain.performance.dto.request.PerformanceUpdateRequest;
import com.umc.bscene.domain.performance.dto.response.PerformanceListResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceResponse;
import com.umc.bscene.domain.performance.dto.response.PerformanceSummaryResponse;
import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import com.umc.bscene.domain.performance.exception.PerformanceException;
import com.umc.bscene.domain.performance.repository.PerformanceRepository;
import com.umc.bscene.domain.performance.response.code.PerformanceErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final BandRepository bandRepository;
    private final BandMemberRepository bandMemberRepository;

    // 공연 등록 (밴드 멤버만 가능, 지난 날짜 등록 불가)
    @Transactional
    public PerformanceSummaryResponse createPerformance(Long userId, Long bandId, PerformanceCreateRequest request) {
        Band band = getBand(bandId);
        validateBandMember(band, userId);
        validateNotPastDate(request.performanceDate());

        Performance performance = Performance.builder()
                .band(band)
                .title(request.title())
                .performanceDate(request.performanceDate())
                .startTime(request.startTime())
                .region(request.region())
                .venue(request.venue())
                .description(request.description())
                .ticketPrice(request.ticketPrice())
                .ticketLink(request.ticketLink())
                .posterImageUrl(request.posterImageUrl())
                .build();

        return PerformanceSummaryResponse.from(performanceRepository.save(performance));
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

    // 공연 상세 조회
    public PerformanceResponse getPerformanceDetail(Long performanceId) {
        return PerformanceResponse.from(getActivePerformance(performanceId));
    }

    // 공연 정보 수정 (등록한 밴드의 멤버만 가능)
    @Transactional
    public PerformanceResponse updatePerformance(Long userId, Long performanceId, PerformanceUpdateRequest request) {
        Performance performance = getActivePerformance(performanceId);
        validateBandMember(performance.getBand(), userId);
        if (request.performanceDate() != null) {
            validateNotPastDate(request.performanceDate());
        }

        performance.update(
                request.title(),
                request.performanceDate(),
                request.startTime(),
                request.venue(),
                request.ticketPrice(),
                request.ticketLink(),
                request.posterImageUrl()
        );

        return PerformanceResponse.from(performance);
    }

    // 공연 삭제 (등록한 밴드의 멤버만 가능, soft delete)
    @Transactional
    public void deletePerformance(Long userId, Long performanceId) {
        Performance performance = getActivePerformance(performanceId);
        validateBandMember(performance.getBand(), userId);

        performance.delete();
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
}
