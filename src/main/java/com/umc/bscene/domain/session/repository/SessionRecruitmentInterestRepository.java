package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.session.entity.SessionRecruitmentInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Set;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface SessionRecruitmentInterestRepository
        extends JpaRepository<SessionRecruitmentInterest, Long> {

    boolean existsBySessionRecruitment_SessionRecruitmentIdAndUser_Id(
            Long sessionRecruitmentId,
            Long userId
    );

    void deleteBySessionRecruitment_SessionRecruitmentIdAndUser_Id(
            Long sessionRecruitmentId,
            Long userId
    );

    @Query("""
            SELECT interest.sessionRecruitment.sessionRecruitmentId
            FROM SessionRecruitmentInterest interest
            WHERE interest.user.id = :userId
              AND interest.sessionRecruitment.sessionRecruitmentId IN :recruitmentIds
            """)
    Set<Long> findInterestedRecruitmentIds(
            @Param("userId") Long userId,
            @Param("recruitmentIds") Collection<Long> recruitmentIds
    );

    @Query("""
        SELECT interest
        FROM SessionRecruitmentInterest interest
        JOIN FETCH interest.sessionRecruitment recruitment
        JOIN FETCH recruitment.band
        WHERE interest.user.id = :userId
          AND recruitment.deletedAt IS NULL
          AND (:cursorId IS NULL OR interest.sessionRecruitmentInterestId < :cursorId)
        ORDER BY interest.sessionRecruitmentInterestId DESC
    """)
    List<SessionRecruitmentInterest> findMyInterests(
            @Param("userId") Long userId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
