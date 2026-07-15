package com.umc.bscene.domain.search.port;

import com.umc.bscene.domain.post.entity.Post;

import java.util.List;
import java.util.Optional;

/**
 * 검색 색인이 영상 게시물 데이터를 조회하기 위한 포트 (adapter는 post 도메인이 구현).
 * 모든 조회는 band·tagList를 fetch join한 상태로 반환해야 한다.
 */
public interface PostPort {

    // 전체 색인용 : VIDEO 타입 게시물 전체 (band·tagList fetch join)
    List<Post> findAllVideosWithBandAndTags();

    // 단건 색인용 : VIDEO 타입 게시물 조회 (없으면 empty → 문서 삭제 처리)
    Optional<Post> findVideoByIdWithBandAndTags(Long postId);

    // 연쇄 재색인용 : 특정 밴드의 VIDEO 게시물 전체 (band·tagList fetch join)
    List<Post> findAllVideosByBandIdWithBandAndTags(Long bandId);
}
