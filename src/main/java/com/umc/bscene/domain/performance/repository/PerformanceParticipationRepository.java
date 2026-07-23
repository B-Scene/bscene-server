package com.umc.bscene.domain.performance.repository;

import com.umc.bscene.domain.performance.entity.PerformanceParticipation;
import com.umc.bscene.domain.performance.enums.ParticipationStatus;
import com.umc.bscene.domain.performance.enums.PerformanceStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface PerformanceParticipationRepository extends JpaRepository<PerformanceParticipation, Long> {

    // 사용자와 공연 사이에 참여 기록(알림)이 존재하는지 확인
    boolean existsByPerformance_IdAndUser_Id(Long performanceId, Long userId);

    // 사용자와 공연 사이의 참여 기록 조회 (참여완료 처리용)
    Optional<PerformanceParticipation> findByPerformance_IdAndUser_Id(Long performanceId, Long userId);

    // 사용자와 공연 사이의 특정 상태 참여 기록을 삭제 (알림 해제는 SCHEDULED만 삭제 → 참여완료 이력 보존)
    void deleteByPerformance_IdAndUser_IdAndStatus(Long performanceId, Long userId, ParticipationStatus status);

    // 공연 id 목록에 대한 사용자의 참여 기록을 일괄 조회 (관심 공연 목록의 참여 상태 표시용, N+1 방지)
    List<PerformanceParticipation> findAllByUser_IdAndPerformance_IdIn(Long userId, List<Long> performanceIds);

    // 마이페이지 참여 공연 수 : 참여 완료(COMPLETED) 상태 공연 수 (삭제된 공연 제외)
    long countByUser_IdAndStatusAndPerformance_Status(
            Long userId, ParticipationStatus status, PerformanceStatus performanceStatus);

    // 공연 참여 기록 총 개수 : 연도 필터가 적용된 참여 완료 공연 수 (목록 상단 "총 참여 공연 N회"용, 삭제된 공연 제외)
    @Query("SELECT COUNT(pp) FROM PerformanceParticipation pp " +
            "JOIN pp.performance p " +
            "WHERE pp.user.id = :userId " +
            "AND pp.status = :status " +
            "AND p.status = :performanceStatus " +
            "AND (:startDate IS NULL OR p.performanceDate >= :startDate) " +
            "AND (:endDate IS NULL OR p.performanceDate <= :endDate)")
    long countCompletedHistory(
            @Param("userId") Long userId,
            @Param("status") ParticipationStatus status,
            @Param("performanceStatus") PerformanceStatus performanceStatus,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // 공연 참여 기록 목록 : 참여 완료 공연을 날짜/시간 빠른 순(같으면 제목순)으로 조회 (삭제된 공연 제외, 연도 필터)
    @Query("SELECT pp FROM PerformanceParticipation pp " +
            "JOIN FETCH pp.performance p " +
            "WHERE pp.user.id = :userId " +
            "AND pp.status = :status " +
            "AND p.status = :performanceStatus " +
            "AND (:startDate IS NULL OR p.performanceDate >= :startDate) " +
            "AND (:endDate IS NULL OR p.performanceDate <= :endDate) " +
            "ORDER BY p.performanceDate ASC, p.startTime ASC, p.title ASC, p.id ASC")
    Slice<PerformanceParticipation> findCompletedHistory(
            @Param("userId") Long userId,
            @Param("status") ParticipationStatus status,
            @Param("performanceStatus") PerformanceStatus performanceStatus,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    // 현재부터 1시간 이내에 시작하며 아직 사전 알림을 발송하지 않은 참여 기록 조회
    @Query("""
        SELECT pp
        FROM PerformanceParticipation pp
        JOIN FETCH pp.performance p
        JOIN FETCH p.band
        JOIN FETCH pp.user
        WHERE pp.status = :participationStatus
          AND pp.reminderSentAt IS NULL
          AND p.status = :performanceStatus
          AND (
                p.performanceDate > :fromDate
                OR (
                    p.performanceDate = :fromDate
                    AND p.startTime > :fromTime
                )
          )
          AND (
                p.performanceDate < :toDate
                OR (
                    p.performanceDate = :toDate
                    AND p.startTime <= :toTime
                )
          )
        ORDER BY p.performanceDate ASC, p.startTime ASC, p.id ASC, pp.id ASC
        """)
    List<PerformanceParticipation> findReminderTargets(
            @Param("participationStatus") ParticipationStatus participationStatus,
            @Param("performanceStatus") PerformanceStatus performanceStatus,
            @Param("fromDate") LocalDate fromDate,
            @Param("fromTime") LocalTime fromTime,
            @Param("toDate") LocalDate toDate,
            @Param("toTime") LocalTime toTime
    );

    // 참여 확인 대기 목록 : 알림 설정(SCHEDULED) 상태이고 공연 시작시간이 이미 지난 공연 조회 (삭제된 공연 제외)
    @Query("""
        SELECT pp
        FROM PerformanceParticipation pp
        JOIN FETCH pp.performance p
        WHERE pp.user.id = :userId
          AND pp.status = :participationStatus
          AND p.status = :performanceStatus
          AND (
                p.performanceDate < :nowDate
                OR (
                    p.performanceDate = :nowDate
                    AND p.startTime <= :nowTime
                )
          )
        ORDER BY p.performanceDate ASC, p.startTime ASC, p.id ASC
        """)
    List<PerformanceParticipation> findPendingConfirmations(
            @Param("userId") Long userId,
            @Param("participationStatus") ParticipationStatus participationStatus,
            @Param("performanceStatus") PerformanceStatus performanceStatus,
            @Param("nowDate") LocalDate nowDate,
            @Param("nowTime") LocalTime nowTime
    );

    // 해당 공연의 알림을 설정한 사용자 ID 조회
    @Query("""
        SELECT pp.user.id
        FROM PerformanceParticipation pp
        WHERE pp.performance.id = :performanceId
          AND pp.status = :status
        """)
    List<Long> findUserIdsByPerformanceIdAndStatus(
            @Param("performanceId") Long performanceId,
            @Param("status") ParticipationStatus status
    );
}
