package com.umc.bscene.domain.post.adapter;

import com.umc.bscene.domain.post.entity.Post;
import com.umc.bscene.domain.post.enums.PostType;
import com.umc.bscene.domain.post.repository.PostRepository;
import com.umc.bscene.domain.search.port.PostPort;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

/**
 * 검색 색인의 PostPort를 post 도메인이 구현하는 어댑터.
 * 색인 대상은 VIDEO 타입 게시물만 (탐색 화면의 "영상" 섹션).
 */
@RequiredArgsConstructor
public class SearchAdapter implements PostPort {

    private final PostRepository postRepository;

    @Override
    public List<Post> findAllVideosWithBandAndTags() {
        return postRepository.findAllByTypeWithBandAndTags(PostType.VIDEO);
    }

    @Override
    public Optional<Post> findVideoByIdWithBandAndTags(Long postId) {
        return postRepository.findByIdAndTypeWithBandAndTags(postId, PostType.VIDEO);
    }

    @Override
    public List<Post> findAllVideosByBandIdWithBandAndTags(Long bandId) {
        return postRepository.findAllByBandIdAndTypeWithBandAndTags(bandId, PostType.VIDEO);
    }
}
