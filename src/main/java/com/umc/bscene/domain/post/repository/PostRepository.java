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

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByBand_IdAndIdLessThanOrderByIdDesc(Long bandId, Long cursor, Pageable pageable);

    List<Post> findByBand_IdAndTypeAndIdLessThanOrderByIdDesc(Long bandId, PostType type, Long cursor, Pageable pageable);

    // 후보 밴드 중 최근 활동(포스트 작성) 이력이 있는 밴드 id만 추려서 N+1 조회를 피함
    @Query("SELECT DISTINCT p.band.id FROM Post p WHERE p.band.id IN :bandIds AND p.createdAt >= :since")
    List<Long> findBandIdsWithRecentPost(@Param("bandIds") List<Long> bandIds, @Param("since") LocalDateTime since);

    // FanHomeAdapter에서 사용 : 팔로우한 밴드들의 최근 소식을 최신순으로 조회 (밴드 정보 fetch join)
    @Query("SELECT p FROM Post p JOIN FETCH p.band b " +
            "WHERE b.id IN :bandIds " +
            "ORDER BY p.createdAt DESC, p.id DESC")
    List<Post> findRecentByBandIds(@Param("bandIds") List<Long> bandIds, Pageable pageable);

    // FanHomeAdapter에서 사용 : 여러 포스트의 미디어를 한 번에 조회 (sortOrder asc → 포스트별 첫 이미지 판별)
    @Query("SELECT m FROM PostMedia m WHERE m.post.id IN :postIds ORDER BY m.sortOrder ASC")
    List<PostMedia> findMediaByPostIds(@Param("postIds") List<Long> postIds);

    // FanHomeAdapter에서 사용 : 여러 포스트의 태그를 한 번에 조회
    @Query("SELECT t FROM PostTag t WHERE t.post.id IN :postIds")
    List<PostTag> findTagsByPostIds(@Param("postIds") List<Long> postIds);
}
