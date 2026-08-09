package com.umc.bscene.domain.post.config;

import com.umc.bscene.domain.post.adapter.BandAdapter;
import com.umc.bscene.domain.post.adapter.FanHomeAdapter;
import com.umc.bscene.domain.post.adapter.SearchAdapter;
import com.umc.bscene.domain.post.repository.PostCommentRepository;
import com.umc.bscene.domain.post.repository.PostRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PostConfig {

    // 팬홈 PostPort 구현 어댑터 (팔로우 밴드 최근 소식)
    @Bean
    public FanHomeAdapter fanHomePostAdapter(PostRepository postRepository) {
        return new FanHomeAdapter(postRepository);
    }

    // 검색 색인 PostPort 구현 어댑터 (VIDEO 게시물)
    @Bean
    public SearchAdapter searchPostAdapter(PostRepository postRepository) {
        return new SearchAdapter(postRepository);
    }

    // 밴드 도메인 PostCommentPort 구현 어댑터 (밴드 명의 소멸 시 밴드 댓글 삭제)
    @Bean
    public BandAdapter bandPostCommentAdapter(PostCommentRepository postCommentRepository) {
        return new BandAdapter(postCommentRepository);
    }
}
