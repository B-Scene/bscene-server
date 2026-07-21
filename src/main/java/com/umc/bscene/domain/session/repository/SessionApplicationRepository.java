package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.session.enums.SkillLevel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SessionApplicationRepository extends JpaRepository<SessionApplication, Long> {

    long countByUserIdAndDeletedAtIsNull(Long userId);

    List<SessionApplication> findAllByUserIdAndDeletedAtIsNullOrderBySessionApplicationIdAsc(
            Long userId
    );

    Optional<SessionApplication> findBySessionApplicationIdAndUserIdAndDeletedAtIsNull(
            Long sessionApplicationId,
            Long userId
    );

    @Query("""
        SELECT DISTINCT sa
        FROM SessionApplication sa
        LEFT JOIN FETCH sa.portfolioLinks
        WHERE sa.userId = :userId
          AND sa.deletedAt IS NULL
        ORDER BY sa.sessionApplicationId ASC
    """)
    List<SessionApplication> findAllByUserIdWithPortfolioLinks(@Param("userId") Long userId);

    @Query("""
        SELECT DISTINCT sa
        FROM SessionApplication sa
        LEFT JOIN FETCH sa.portfolioLinks
        WHERE sa.sessionApplicationId = :sessionApplicationId
          AND sa.userId = :userId
          AND sa.deletedAt IS NULL
    """)
    Optional<SessionApplication> findByIdAndUserIdWithPortfolioLinks(
            @Param("sessionApplicationId") Long sessionApplicationId,
            @Param("userId") Long userId
    );

    @Query("""
        SELECT DISTINCT sa
        FROM SessionApplication sa
        LEFT JOIN FETCH sa.portfolioLinks
        WHERE sa.sessionApplicationId = :sessionApplicationId
          AND sa.purpose = :purpose
          AND sa.isPublic = true
          AND sa.deletedAt IS NULL
    """)
    Optional<SessionApplication> findPublicDetailWithPortfolioLinks(
            @Param("sessionApplicationId") Long sessionApplicationId,
            @Param("purpose") String purpose
    );

    @Query("""
        SELECT sa
        FROM SessionApplication sa
        LEFT JOIN User u ON u.id = sa.userId
        WHERE sa.deletedAt IS NULL
          AND sa.purpose = :purpose
          AND sa.isPublic = true
          AND sa.userId <> :viewerUserId
          AND (:region IS NULL OR sa.region = :region)
          AND (:skillLevel IS NULL OR sa.skillLevel = :skillLevel)
          AND (:part IS NULL OR sa.part = :part)
          AND (:genre IS NULL OR sa.genre = :genre)
          AND (:recommendationEnabled = false
               OR sa.genre = :recommendedGenre
               OR sa.region = :recommendedRegion)
          AND (:keyword IS NULL
               OR LOWER(COALESCE(u.name, sa.nickname)) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(sa.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(sa.oneLineIntro) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:cursorId IS NULL OR sa.sessionApplicationId < :cursorId)
        ORDER BY sa.sessionApplicationId DESC
    """)
    List<SessionApplication> searchDefaultApplications(
            @Param("viewerUserId") Long viewerUserId,
            @Param("purpose") String purpose,
            @Param("region") Region region,
            @Param("skillLevel") SkillLevel skillLevel,
            @Param("part") Part part,
            @Param("genre") Genre genre,
            @Param("recommendationEnabled") boolean recommendationEnabled,
            @Param("recommendedGenre") Genre recommendedGenre,
            @Param("recommendedRegion") Region recommendedRegion,
            @Param("keyword") String keyword,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    Optional<SessionApplication>
    findFirstByUserIdAndPurposeAndDeletedAtIsNullOrderBySessionApplicationIdDesc(
            Long userId,
            String purpose
    );

    @Query("""
        SELECT CASE WHEN COUNT(sa) > 0 THEN true ELSE false END
        FROM SessionApplication sa
        WHERE sa.deletedAt IS NULL
          AND sa.purpose = :purpose
          AND sa.isPublic = true
          AND sa.userId <> :viewerUserId
          AND (sa.genre = :genre OR sa.region = :region)
    """)
    boolean existsRecommendedApplications(
            @Param("viewerUserId") Long viewerUserId,
            @Param("purpose") String purpose,
            @Param("genre") Genre genre,
            @Param("region") Region region
    );
}
