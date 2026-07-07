package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SessionRegion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionRecruitmentRepository extends JpaRepository<SessionRecruitment, Long> {

    // 기존: 단건 조회/수정/삭제용
    Optional<SessionRecruitment> findBySessionRecruitmentIdAndDeletedAtIsNull(Long sessionRecruitmentId);

    // 전체 조회
    List<SessionRecruitment> findByDeletedAtIsNullOrderBySessionRecruitmentIdDesc(Pageable pageable);

    // 전체 조회 + 커서
    List<SessionRecruitment> findByDeletedAtIsNullAndSessionRecruitmentIdLessThanOrderBySessionRecruitmentIdDesc(
            Long cursorId,
            Pageable pageable
    );

    // 필터 조회
    List<SessionRecruitment> findByDeletedAtIsNullAndPartAndGenreAndRegionOrderBySessionRecruitmentIdDesc(
            Part part,
            SessionGenre genre,
            SessionRegion region,
            Pageable pageable
    );

    // 필터 조회 + 커서
    List<SessionRecruitment> findByDeletedAtIsNullAndPartAndGenreAndRegionAndSessionRecruitmentIdLessThanOrderBySessionRecruitmentIdDesc(
            Part part,
            SessionGenre genre,
            SessionRegion region,
            Long cursorId,
            Pageable pageable
    );

    // 검색어 조회
    List<SessionRecruitment> findByDeletedAtIsNullAndRecruitmentTitleContainingOrderBySessionRecruitmentIdDesc(
            String keyword,
            Pageable pageable
    );

    // 검색어 조회 + 커서
    List<SessionRecruitment> findByDeletedAtIsNullAndRecruitmentTitleContainingAndSessionRecruitmentIdLessThanOrderBySessionRecruitmentIdDesc(
            String keyword,
            Long cursorId,
            Pageable pageable
    );
}