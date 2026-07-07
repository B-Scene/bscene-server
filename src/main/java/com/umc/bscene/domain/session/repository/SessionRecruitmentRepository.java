package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SessionRegion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SessionRecruitmentRepository extends JpaRepository<SessionRecruitment, Long> {

    // 기존: 단건 조회/수정/삭제용
    Optional<SessionRecruitment> findBySessionRecruitmentIdAndDeletedAtIsNull(Long sessionRecruitmentId);

    // 전체 조회
    List<SessionRecruitment> findByDeletedAtIsNullAndDeadlineAtAfterOrderBySessionRecruitmentIdDesc(
            LocalDateTime now,
            Pageable pageable
    );

    // 전체 조회 + 커서
    List<SessionRecruitment> findByDeletedAtIsNullAndDeadlineAtAfterAndSessionRecruitmentIdLessThanOrderBySessionRecruitmentIdDesc(
            LocalDateTime now,
            Long cursorId,
            Pageable pageable
    );

    // 필터 조회
    List<SessionRecruitment> findByDeletedAtIsNullAndDeadlineAtAfterAndPartAndGenreAndRegionOrderBySessionRecruitmentIdDesc(
            LocalDateTime now,
            Part part,
            SessionGenre genre,
            SessionRegion region,
            Pageable pageable
    );

    // 필터 조회 + 커서
    List<SessionRecruitment> findByDeletedAtIsNullAndDeadlineAtAfterAndPartAndGenreAndRegionAndSessionRecruitmentIdLessThanOrderBySessionRecruitmentIdDesc(
            LocalDateTime now,
            Part part,
            SessionGenre genre,
            SessionRegion region,
            Long cursorId,
            Pageable pageable
    );

    // 검색어 조회
    List<SessionRecruitment> findByDeletedAtIsNullAndDeadlineAtAfterAndRecruitmentTitleContainingOrderBySessionRecruitmentIdDesc(
            LocalDateTime now,
            String keyword,
            Pageable pageable
    );

    // 검색어 조회 + 커서
    List<SessionRecruitment> findByDeletedAtIsNullAndDeadlineAtAfterAndRecruitmentTitleContainingAndSessionRecruitmentIdLessThanOrderBySessionRecruitmentIdDesc(
            LocalDateTime now,
            String keyword,
            Long cursorId,
            Pageable pageable
    );
}