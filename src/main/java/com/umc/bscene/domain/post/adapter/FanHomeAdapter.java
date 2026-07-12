package com.umc.bscene.domain.post.adapter;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse.BandNewsItem;
import com.umc.bscene.domain.fanhome.dto.response.FollowingBandNewsResponse;
import com.umc.bscene.domain.fanhome.dto.response.FollowingBandNewsResponse.NewsItem;
import com.umc.bscene.domain.fanhome.port.PostPort;
import com.umc.bscene.domain.post.entity.Post;
import com.umc.bscene.domain.post.entity.PostMedia;
import com.umc.bscene.domain.post.entity.PostTag;
import com.umc.bscene.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 팬홈의 PostPort를 post 도메인이 구현하는 어댑터.
 * 팔로우한 밴드들의 최근 소식(Post)을 밴드 정보와 조합해 최신순으로 반환한다.
 *
 * N+1을 피하기 위해 쿼리를 3방으로 고정한다.
 *   1) 포스트 목록 (밴드 fetch join)
 *   2) 미디어를 postId IN 으로 일괄 조회
 *   3) 태그를 postId IN 으로 일괄 조회
 * 이후 postId 기준으로 그룹핑해 메모리에서 조립한다.
 */
@RequiredArgsConstructor
public class FanHomeAdapter implements PostPort {

    private final PostRepository postRepository;

    @Override
    public List<BandNewsItem> findRecentNews(List<Long> bandIds, int limit) {
        if (bandIds.isEmpty()) {
            return List.of();
        }

        // 1) 밴드 소식(Post + Band) 최신순 조회 (커서 없이 상위부터 → MAX_VALUE 전달)
        List<Post> posts = postRepository.findNewsByBandIds(bandIds, Long.MAX_VALUE, PageRequest.of(0, limit));
        if (posts.isEmpty()) {
            return List.of();
        }
        List<Long> postIds = posts.stream().map(Post::getId).toList();

        // 2) 미디어 일괄 조회 후 postId 기준 그룹핑 (쿼리가 sortOrder asc 정렬 → 리스트 첫 요소가 첫 이미지)
        Map<Long, List<PostMedia>> mediaByPostId = postRepository.findMediaByPostIds(postIds).stream()
                .collect(Collectors.groupingBy(m -> m.getPost().getId()));

        // 3) 태그 일괄 조회 후 postId 기준으로 tagName만 모아 그룹핑
        Map<Long, List<String>> tagsByPostId = postRepository.findTagsByPostIds(postIds).stream()
                .collect(Collectors.groupingBy(
                        t -> t.getPost().getId(),
                        Collectors.mapping(PostTag::getTagName, Collectors.toList())
                ));

        // 4) 메모리 조립
        return posts.stream()
                .map(post -> toItem(post, mediaByPostId, tagsByPostId))
                .toList();
    }

    private BandNewsItem toItem(
            Post post,
            Map<Long, List<PostMedia>> mediaByPostId,
            Map<Long, List<String>> tagsByPostId
    ) {
        Band band = post.getBand();
        return new BandNewsItem(
                band.getId(),
                band.getName(),
                band.getProfileImageUrl(),
                band.getGenre(),
                band.getRegion(),
                post.getId(),
                post.getType(),
                previewImageUrl(post, mediaByPostId.getOrDefault(post.getId(), List.of())),
                post.getTitle(),
                post.getDescription(),
                tagsByPostId.getOrDefault(post.getId(), List.of()),
                post.getCreatedAt()
        );
    }

    // 카드 미리보기 이미지: PHOTO=첫 사진, VIDEO=썸네일, TEXT=없음
    private String previewImageUrl(Post post, List<PostMedia> media) {
        return switch (post.getType()) {
            case PHOTO -> media.isEmpty() ? null : media.get(0).getMediaUrl();
            case VIDEO -> post.getThumbnailUrl();
            case TEXT -> null;
        };
    }

    @Override
    public FollowingBandNewsResponse findFollowingBandNews(List<Long> bandIds, Long cursor, int size) {
        if (bandIds.isEmpty()) {
            return new FollowingBandNewsResponse(List.of(), null, false);
        }

        // 첫 페이지(cursor 없음)는 MAX_VALUE로 상위부터, size + 1 조회로 다음 페이지 존재 여부 판별
        Long effectiveCursor = (cursor == null) ? Long.MAX_VALUE : cursor;
        List<Post> fetched = postRepository.findNewsByBandIds(bandIds, effectiveCursor, PageRequest.of(0, size + 1));
        boolean hasNext = fetched.size() > size;
        List<Post> posts = hasNext ? fetched.subList(0, size) : fetched;

        if (posts.isEmpty()) {
            return new FollowingBandNewsResponse(List.of(), null, false);
        }
        List<Long> postIds = posts.stream().map(Post::getId).toList();

        // 미디어/태그 일괄 조회 후 postId 기준 그룹핑
        Map<Long, List<PostMedia>> mediaByPostId = postRepository.findMediaByPostIds(postIds).stream()
                .collect(Collectors.groupingBy(m -> m.getPost().getId()));
        Map<Long, List<String>> tagsByPostId = postRepository.findTagsByPostIds(postIds).stream()
                .collect(Collectors.groupingBy(
                        t -> t.getPost().getId(),
                        Collectors.mapping(PostTag::getTagName, Collectors.toList())
                ));

        List<NewsItem> items = posts.stream()
                .map(post -> toNewsItem(post, mediaByPostId, tagsByPostId))
                .toList();

        // 다음 페이지가 있을 때만 마지막 소식의 id를 커서로 반환
        Long nextCursor = hasNext ? posts.get(posts.size() - 1).getId() : null;
        return new FollowingBandNewsResponse(items, nextCursor, hasNext);
    }

    private NewsItem toNewsItem(
            Post post,
            Map<Long, List<PostMedia>> mediaByPostId,
            Map<Long, List<String>> tagsByPostId
    ) {
        Band band = post.getBand();
        return new NewsItem(
                band.getId(),
                band.getName(),
                band.getProfileImageUrl(),
                band.getGenre(),
                band.getRegion(),
                post.getId(),
                post.getType(),
                mediaUrls(post, mediaByPostId.getOrDefault(post.getId(), List.of())),
                post.getTitle(),
                post.getDescription(),
                tagsByPostId.getOrDefault(post.getId(), List.of()),
                post.getCreatedAt()
        );
    }

    // 미디어 URL 배열: PHOTO=사진 전부(sortOrder순), VIDEO=썸네일, TEXT=빈 배열
    private List<String> mediaUrls(Post post, List<PostMedia> media) {
        return switch (post.getType()) {
            case PHOTO -> media.stream().map(PostMedia::getMediaUrl).toList();
            case VIDEO -> post.getThumbnailUrl() == null ? List.of() : List.of(post.getThumbnailUrl());
            case TEXT -> List.of();
        };
    }
}
