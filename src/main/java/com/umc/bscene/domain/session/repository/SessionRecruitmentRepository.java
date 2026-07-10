package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SessionGenre;
import com.umc.bscene.domain.session.enums.SessionRegion;
import com.umc.bscene.domain.session.enums.SkillLevel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SessionRecruitmentRepository extends JpaRepository<SessionRecruitment, Long> {

    // 기존: 단건 조회/수정/삭제용
    Optional<SessionRecruitment> findBySessionRecruitmentIdAndDeletedAtIsNull(Long sessionRecruitmentId);

    @Query("""
        SELECT sr
        FROM SessionRecruitment sr
        JOIN FETCH sr.band
        WHERE sr.deletedAt IS NULL
          AND sr.deadlineAt > :now
          AND (:part IS NULL OR sr.part = :part)
          AND (:skillLevel IS NULL OR sr.skillLevel = :skillLevel)
          AND (:genre IS NULL OR sr.genre = :genre)
          AND (:region IS NULL OR sr.region = :region)
          AND (:keyword IS NULL OR LOWER(sr.recruitmentTitle)
              LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:cursorId IS NULL OR sr.sessionRecruitmentId < :cursorId)
        ORDER BY sr.sessionRecruitmentId DESC
    """)
    List<SessionRecruitment> findRecruitments(
            @Param("now") LocalDateTime now,
            @Param("part") Part part,
            @Param("skillLevel") SkillLevel skillLevel,
            @Param("genre") SessionGenre genre,
            @Param("region") SessionRegion region,
            @Param("keyword") String keyword,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
