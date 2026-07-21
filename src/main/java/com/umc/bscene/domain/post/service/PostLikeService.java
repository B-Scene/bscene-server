package com.umc.bscene.domain.post.service;

import com.umc.bscene.domain.post.dto.response.PostLikeResponse;
import com.umc.bscene.domain.post.entity.Post;
import com.umc.bscene.domain.post.entity.PostLike;
import com.umc.bscene.domain.post.exception.PostException;
import com.umc.bscene.domain.post.repository.PostLikeRepository;
import com.umc.bscene.domain.post.repository.PostRepository;
import com.umc.bscene.domain.post.response.code.PostErrorCode;
import com.umc.bscene.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final UserRepository userRepository;

    // 좋아요 등록 (이미 좋아요한 콘텐츠면 409)
    @Transactional
    public PostLikeResponse setLike(Long userId, Long postId) {
        Post post = getPost(postId);

        if (postLikeRepository.existsByPost_IdAndUser_Id(postId, userId)) {
            throw new PostException(PostErrorCode.ALREADY_LIKED);
        }

        try {
            postLikeRepository.save(PostLike.builder()
                    .post(post)
                    .user(userRepository.getReferenceById(userId))
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 위 중복 체크를 둘 다 통과한 경우, 유니크 제약이 최종 방어 → 409로 변환
            throw new PostException(PostErrorCode.ALREADY_LIKED);
        }

        return PostLikeResponse.of(postId, true, postLikeRepository.countByPost_Id(postId));
    }

    // 좋아요 해제 (좋아요하지 않은 상태여도 200 — 멱등 처리)
    @Transactional
    public PostLikeResponse unsetLike(Long userId, Long postId) {
        getPost(postId);

        postLikeRepository.deleteByPost_IdAndUser_Id(postId, userId);

        return PostLikeResponse.of(postId, false, postLikeRepository.countByPost_Id(postId));
    }

    private Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.POST_NOT_FOUND));
    }
}
