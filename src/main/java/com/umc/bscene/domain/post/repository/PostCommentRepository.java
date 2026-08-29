package com.umc.bscene.domain.post.repository;

import com.umc.bscene.domain.band.annotation.IncludesPendingBands;
import com.umc.bscene.domain.post.entity.PostComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    // 댓글 목록 : 등록순(오래된 댓글이 위), 커서(마지막 댓글 id) 기반 무한스크롤
    // 본인 댓글은 항상 제외(myComments로 분리)하고, 차단 필터는 팬 댓글에만 적용한다
    // excludedUserIds(차단한 유저 + 본인)는 서비스에서 항상 본인 id를 포함시키므로 빈 컬렉션이 전달되지 않음
    @Query("""
            SELECT c
            FROM PostComment c
            JOIN FETCH c.user
            LEFT JOIN FETCH c.bandMemberProfile
            WHERE c.post.id = :postId
              AND (:cursor IS NULL OR c.id > :cursor)
              AND c.user.id <> :viewerId
              AND (c.bandMemberProfile IS NOT NULL OR c.user.id NOT IN :excludedUserIds)
            ORDER BY c.id ASC
            """)
    Slice<PostComment> findComments(
            @Param("postId") Long postId,
            @Param("cursor") Long cursor,
            @Param("viewerId") Long viewerId,
            @Param("excludedUserIds") Collection<Long> excludedUserIds,
            Pageable pageable
    );

    // 내가 쓴 댓글 전체 : 등록순 (목록 첫 페이지에서 맨 위 고정 표시용, 밴드 명의 댓글 포함)
    @Query("""
            SELECT c
            FROM PostComment c
            JOIN FETCH c.user
            LEFT JOIN FETCH c.bandMemberProfile
            WHERE c.post.id = :postId
              AND c.user.id = :userId
            ORDER BY c.id ASC
            """)
    List<PostComment> findMyComments(@Param("postId") Long postId, @Param("userId") Long userId);

    // 댓글 수 : 차단 여부와 관계없이 전체 댓글 수 (상세 페이지 카운트용)
    long countByPost_Id(Long postId);

    // 밴드 멤버 제거(강퇴/자진 탈퇴) 시 : 해당 유저가 그 밴드 게시물에 밴드 명의로 쓴 댓글 일괄 삭제
    // (팬 명의 댓글은 남긴다 — 명의가 소멸하는 건 밴드 댓글뿐)
    @Modifying
    @Query("""
            DELETE FROM PostComment c
            WHERE c.user.id = :userId
              AND c.bandMemberProfile IS NOT NULL
              AND c.post.id IN (SELECT p.id FROM Post p WHERE p.band.id = :bandId)
            """)
    @IncludesPendingBands(reason = "정리용 삭제(멤버 탈퇴·검수 밴드 삭제) - 밴드 상태와 무관하게 남은 행을 지워야 한다")
    void deleteBandComments(@Param("bandId") Long bandId, @Param("userId") Long userId);

    // 멤버 프로필 삭제 시 : 그 프로필 명의로 쓴 댓글 일괄 삭제 (FK 제약 선해소)
    @Modifying
    @Query("DELETE FROM PostComment c WHERE c.bandMemberProfile.id = :profileId")
    void deleteByBandMemberProfileId(@Param("profileId") Long profileId);
}
