package com.umc.bscene.domain.post.repository;

import com.umc.bscene.domain.post.entity.Post;
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
}
