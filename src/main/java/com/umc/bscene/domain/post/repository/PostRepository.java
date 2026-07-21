package com.umc.bscene.domain.post.repository;

import com.umc.bscene.domain.post.entity.Post;
import com.umc.bscene.domain.post.entity.PostMedia;
import com.umc.bscene.domain.post.entity.PostTag;
import com.umc.bscene.domain.post.enums.PostType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByBand_IdAndIdLessThanOrderByIdDesc(Long bandId, Long cursor, Pageable pageable);

    List<Post> findByBand_IdAndTypeAndIdLessThanOrderByIdDesc(Long bandId, PostType type, Long cursor, Pageable pageable);

    // 후보 밴드 중 최근 활동(포스트 작성) 이력이 있는 밴드 id만 추려서 N+1 조회를 피함
    @Query("SELECT DISTINCT p.band.id FROM Post p WHERE p.band.id IN :bandIds AND p.createdAt >= :since")
    List<Long> findBandIdsWithRecentPost(@Param("bandIds") List<Long> bandIds, @Param("since") LocalDateTime since);

    // 후보 밴드별 가장 최근 포스트 작성일시 (추천 동점자 정렬용 tie-breaker)
    @Query("SELECT p.band.id, MAX(p.createdAt) FROM Post p WHERE p.band.id IN :bandIds GROUP BY p.band.id")
    List<Object[]> findLatestActivityAtByBandIds(@Param("bandIds") List<Long> bandIds);

    // FanHomeAdapter에서 사용 : 팔로우한 밴드 소식을 id 커서 기반 최신순으로 조회 (홈 미리보기 + 전체조회 공용, 밴드 정보 fetch join)
    // 첫 페이지는 cursor에 Long.MAX_VALUE를 넘겨 상위부터 조회 (id < :cursor 를 깨끗한 인덱스 range seek로 유지)
    @Query("SELECT p FROM Post p JOIN FETCH p.band b " +
            "WHERE b.id IN :bandIds " +
            "AND p.id < :cursor " +
            "ORDER BY p.id DESC")
    List<Post> findNewsByBandIds(
            @Param("bandIds") List<Long> bandIds,
            @Param("cursor") Long cursor,
            Pageable pageable
    );

    // FanHomeAdapter에서 사용 : 여러 포스트의 미디어를 한 번에 조회 (sortOrder asc → 포스트별 첫 이미지 판별)
    @Query("SELECT m FROM PostMedia m WHERE m.post.id IN :postIds ORDER BY m.sortOrder ASC")
    List<PostMedia> findMediaByPostIds(@Param("postIds") List<Long> postIds);

    // FanHomeAdapter에서 사용 : 여러 포스트의 태그를 한 번에 조회
    @Query("SELECT t FROM PostTag t WHERE t.post.id IN :postIds")
    List<PostTag> findTagsByPostIds(@Param("postIds") List<Long> postIds);

    // SearchAdapter에서 사용 : 검색 전체 색인용 게시물 (band·tags fetch join → 밴드명·태그 비정규화)
    @Query("SELECT DISTINCT p FROM Post p JOIN FETCH p.band LEFT JOIN FETCH p.tagList")
    List<Post> findAllWithBandAndTags();

    // SearchAdapter에서 사용 : 검색 단건 색인용 (band·tags fetch join)
    @Query("SELECT p FROM Post p JOIN FETCH p.band LEFT JOIN FETCH p.tagList WHERE p.id = :id")
    Optional<Post> findByIdWithBandAndTags(@Param("id") Long id);

    // SearchAdapter에서 사용 : 밴드 정보 변경 시 연쇄 재색인용 (band·tags fetch join)
    @Query("SELECT DISTINCT p FROM Post p JOIN FETCH p.band b LEFT JOIN FETCH p.tagList WHERE b.id = :bandId")
    List<Post> findAllByBandIdWithBandAndTags(@Param("bandId") Long bandId);
}
