package com.umc.bscene.domain.post.adapter;

import com.umc.bscene.domain.post.entity.Post;
import com.umc.bscene.domain.post.repository.PostRepository;
import com.umc.bscene.domain.search.port.PostPort;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * 검색 색인의 PostPort를 post 도메인이 구현하는 어댑터.
 * 색인 대상은 게시물 전체 (탐색 화면의 "게시물" 섹션 — 영상/사진/글).
 */
@RequiredArgsConstructor
public class SearchAdapter implements PostPort {

    private final PostRepository postRepository;

    @Override
    public List<Post> findAllWithBandAndTags() {
        return postRepository.findAllWithBandAndTags();
    }

    @Override
    public Optional<Post> findByIdWithBandAndTags(Long postId) {
        return postRepository.findByIdWithBandAndTags(postId);
    }

    @Override
    public List<Post> findAllByBandIdWithBandAndTags(Long bandId) {
        return postRepository.findAllByBandIdWithBandAndTags(bandId);
    }
}
