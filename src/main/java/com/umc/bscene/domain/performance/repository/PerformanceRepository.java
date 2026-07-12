package com.umc.bscene.domain.performance.repository;

import com.umc.bscene.domain.performance.entity.Performance;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {

    List<Performance> findByBand_IdAndStatusOrderByPerformanceDateAsc(Long bandId, PerformanceStatus status);

    // 후보 밴드 중 최근 30일 내 공연 이력이 있는 밴드 id만 추려서 N+1 조회를 피함
    @Query("SELECT DISTINCT p.band.id FROM Performance p " +
            "WHERE p.band.id IN :bandIds AND p.status = :status AND p.performanceDate >= :since")
    List<Long> findBandIdsWithRecentPerformance(
            @Param("bandIds") List<Long> bandIds,
            @Param("status") PerformanceStatus status,
            @Param("since") LocalDate since
    );

    Long countByBand_IdAndStatus(Long bandId, PerformanceStatus status);

    // FanHomeAdapter에서 사용 : 팔로우한 밴드들의 아직 시작하지 않은 ACTIVE 공연을 시작 일시가 가까운 순으로 조회
    @Query("SELECT p FROM Performance p " +
            "WHERE p.band.id IN :bandIds AND p.status = :status " +
            "AND (p.performanceDate > :today " +
            "     OR (p.performanceDate = :today AND (p.startTime IS NULL OR p.startTime >= :now))) " +
            "ORDER BY p.performanceDate ASC, p.startTime ASC")
    List<Performance> findUpcomingByBandIds(
            @Param("bandIds") List<Long> bandIds,
            @Param("status") PerformanceStatus status,
            @Param("today") LocalDate today,
            @Param("now") LocalTime now,
            Pageable pageable
    );

    // FanHomeAdapter에서 사용 : 아직 시작하지 않은 ACTIVE 공연을 관심 등록 수가 많은 순으로 조회 (추천 공연)
    @Query("SELECT p FROM Performance p " +
            "LEFT JOIN PerformanceInterest pi ON pi.performance = p " +
            "WHERE p.status = :status " +
            "AND (p.performanceDate > :today " +
            "     OR (p.performanceDate = :today AND (p.startTime IS NULL OR p.startTime >= :now))) " +
            "GROUP BY p " +
            "ORDER BY COUNT(pi) DESC, p.id DESC")
    List<Performance> findPopularByInterest(
            @Param("status") PerformanceStatus status,
            @Param("today") LocalDate today,
            @Param("now") LocalTime now,
            Pageable pageable
    );

    // 다가오는 공연 목록(임박순) : 팔로우 밴드의 아직 시작 안 한 ACTIVE 공연을 날짜·시각 가까운 순으로
    @Query("SELECT p FROM Performance p " +
            "WHERE p.band.id IN :bandIds AND p.status = :status " +
            "AND (p.performanceDate > :today " +
            "     OR (p.performanceDate = :today AND (p.startTime IS NULL OR p.startTime >= :now))) " +
            "ORDER BY p.performanceDate ASC, p.startTime ASC, p.id ASC")
    Slice<Performance> findUpcomingImminent(
            @Param("bandIds") List<Long> bandIds,
            @Param("status") PerformanceStatus status,
            @Param("today") LocalDate today,
            @Param("now") LocalTime now,
            Pageable pageable
    );

    // 다가오는 공연 목록(최신순) : 최근 등록된 순
    @Query("SELECT p FROM Performance p " +
            "WHERE p.band.id IN :bandIds AND p.status = :status " +
            "AND (p.performanceDate > :today " +
            "     OR (p.performanceDate = :today AND (p.startTime IS NULL OR p.startTime >= :now))) " +
            "ORDER BY p.createdAt DESC, p.id DESC")
    Slice<Performance> findUpcomingLatest(
            @Param("bandIds") List<Long> bandIds,
            @Param("status") PerformanceStatus status,
            @Param("today") LocalDate today,
            @Param("now") LocalTime now,
            Pageable pageable
    );

    // 다가오는 공연 목록(인기순) : 관심 등록 수 많은 순
    @Query("SELECT p FROM Performance p " +
            "LEFT JOIN PerformanceInterest pi ON pi.performance = p " +
            "WHERE p.band.id IN :bandIds AND p.status = :status " +
            "AND (p.performanceDate > :today " +
            "     OR (p.performanceDate = :today AND (p.startTime IS NULL OR p.startTime >= :now))) " +
            "GROUP BY p " +
            "ORDER BY COUNT(pi) DESC, p.id DESC")
    Slice<Performance> findUpcomingPopular(
            @Param("bandIds") List<Long> bandIds,
            @Param("status") PerformanceStatus status,
            @Param("today") LocalDate today,
            @Param("now") LocalTime now,
            Pageable pageable
    );
}
