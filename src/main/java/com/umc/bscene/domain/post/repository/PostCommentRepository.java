package com.umc.bscene.domain.post.repository;

import com.umc.bscene.domain.post.entity.PostComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    // 댓글 목록 : 등록순(오래된 댓글이 위), 커서(마지막 댓글 id) 기반 무한스크롤
    // excludedUserIds(차단한 유저 + 본인)의 댓글은 제외 — 서비스에서 항상 본인 id를 포함시키므로 빈 컬렉션이 전달되지 않음
    @Query("""
            SELECT c
            FROM PostComment c
            JOIN FETCH c.user
            WHERE c.post.id = :postId
              AND (:cursor IS NULL OR c.id > :cursor)
              AND c.user.id NOT IN :excludedUserIds
            ORDER BY c.id ASC
            """)
    Slice<PostComment> findComments(
            @Param("postId") Long postId,
            @Param("cursor") Long cursor,
            @Param("excludedUserIds") Collection<Long> excludedUserIds,
            Pageable pageable
    );

    // 내가 쓴 댓글 전체 : 등록순 (목록 첫 페이지에서 맨 위 고정 표시용)
    @Query("""
            SELECT c
            FROM PostComment c
            JOIN FETCH c.user
            WHERE c.post.id = :postId
              AND c.user.id = :userId
            ORDER BY c.id ASC
            """)
    List<PostComment> findMyComments(@Param("postId") Long postId, @Param("userId") Long userId);

    // 댓글 수 : 차단 여부와 관계없이 전체 댓글 수 (상세 페이지 카운트용)
    long countByPost_Id(Long postId);
}
