package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.session.entity.SessionRecruitment;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
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
    List<SessionRecruitment> findLatestRecruitments(
            @Param("now") LocalDateTime now,
            @Param("part") Part part,
            @Param("skillLevel") SkillLevel skillLevel,
            @Param("genre") Genre genre,
            @Param("region") Region region,
            @Param("keyword") String keyword,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

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
          AND (:cursorId IS NULL
              OR sr.deadlineAt > (
                  SELECT cursor.deadlineAt
                  FROM SessionRecruitment cursor
                  WHERE cursor.sessionRecruitmentId = :cursorId
              )
              OR (sr.deadlineAt = (
                  SELECT cursor.deadlineAt
                  FROM SessionRecruitment cursor
                  WHERE cursor.sessionRecruitmentId = :cursorId
              ) AND sr.sessionRecruitmentId < :cursorId))
        ORDER BY sr.deadlineAt ASC, sr.sessionRecruitmentId DESC
    """)
    List<SessionRecruitment> findImminentRecruitments(
            @Param("now") LocalDateTime now,
            @Param("part") Part part,
            @Param("skillLevel") SkillLevel skillLevel,
            @Param("genre") Genre genre,
            @Param("region") Region region,
            @Param("keyword") String keyword,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    // 현재부터 24시간 이내에 마감되며 아직 알림을 발송하지 않은 공고 조회
    @Query("""
        SELECT sr
        FROM SessionRecruitment sr
        JOIN FETCH sr.band
        WHERE sr.deletedAt IS NULL
          AND sr.deadlineReminderSentAt IS NULL
          AND sr.deadlineAt > :now
          AND sr.deadlineAt <= :reminderLimit
        ORDER BY sr.deadlineAt ASC, sr.sessionRecruitmentId ASC
    """)
    List<SessionRecruitment> findDeadlineReminderTargets(
            @Param("now") LocalDateTime now,
            @Param("reminderLimit") LocalDateTime reminderLimit
    );
}
