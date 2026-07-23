package com.umc.bscene.domain.post.repository;

import com.umc.bscene.domain.post.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    // 사용자가 콘텐츠에 좋아요를 눌렀는지 확인 (상세 페이지 하트 상태 + 중복 등록 방지)
    boolean existsByPost_IdAndUser_Id(Long postId, Long userId);

    // 콘텐츠의 좋아요 수 집계
    long countByPost_Id(Long postId);

    // 좋아요 해제 (없으면 아무 일도 하지 않음 — 멱등 처리)
    void deleteByPost_IdAndUser_Id(Long postId, Long userId);
}
