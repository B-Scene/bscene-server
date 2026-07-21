package com.umc.bscene.domain.post.service;

import com.umc.bscene.domain.post.dto.response.PostDetailResponse;
import com.umc.bscene.domain.post.entity.Post;
import com.umc.bscene.domain.post.exception.PostException;
import com.umc.bscene.domain.post.repository.PostCommentRepository;
import com.umc.bscene.domain.post.repository.PostLikeRepository;
import com.umc.bscene.domain.post.repository.PostRepository;
import com.umc.bscene.domain.post.response.code.PostErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostDetailService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCommentRepository postCommentRepository;

    // 팬모드 게시물 상세페이지 조회 : 밴드 정보 + 미디어/본문/태그 + 좋아요·댓글 카운트와 하트 상태
    public PostDetailResponse getPostDetailPage(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.POST_NOT_FOUND));

        long likeCount = postLikeRepository.countByPost_Id(postId);
        long commentCount = postCommentRepository.countByPost_Id(postId);
        boolean isLiked = postLikeRepository.existsByPost_IdAndUser_Id(postId, userId);

        return PostDetailResponse.of(post, likeCount, commentCount, isLiked);
    }
}
