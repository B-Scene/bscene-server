package com.umc.bscene.domain.post.service;

import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.enums.BandMemberStatus;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.post.dto.PostPushMessage;
import com.umc.bscene.domain.post.dto.request.PostCreateRequest;
import com.umc.bscene.domain.post.dto.request.PostUpdateRequest;
import com.umc.bscene.domain.post.dto.response.PostCreateResponse;
import com.umc.bscene.domain.post.dto.response.PostListResponse;
import com.umc.bscene.domain.post.dto.response.PostResponse;
import com.umc.bscene.domain.post.dto.response.PostSummaryResponse;
import com.umc.bscene.domain.post.dto.response.PostUpdateResponse;
import com.umc.bscene.domain.post.entity.Post;
import com.umc.bscene.domain.post.enums.PostType;
import com.umc.bscene.domain.post.event.PostVideoThumbnailRequestedEvent;
import com.umc.bscene.domain.post.exception.PostException;
import com.umc.bscene.domain.post.port.FollowPort;
import com.umc.bscene.domain.post.port.NotifyPort;
import com.umc.bscene.domain.post.repository.PostRepository;
import com.umc.bscene.domain.post.response.code.PostErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private static final int MAX_TAG_COUNT = 6;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final PostRepository postRepository;
    private final BandRepository bandRepository;
    private final BandMemberRepository bandMemberRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final FollowPort followPort;
    private final NotifyPort notifyPort;

    // 콘텐츠 등록 (밴드 멤버만 가능) 등록시 팔로워에게 알림
    @Transactional
    public PostCreateResponse createPost(Long userId, Long bandId, PostCreateRequest request) {
        Band band = getBand(bandId);
        validateBandMember(band, userId);
        validateTagCount(request.tags());
        validateMediaUrls(request.type(), request.mediaUrls());

        Post post = Post.builder()
                .band(band)
                .type(request.type())
                .title(request.title())
                .description(request.description())
                .thumbnailUrl(request.thumbnailUrl())
                .build();

        addMediaList(post, request.mediaUrls());
        addTagList(post, request.tags());

        Post savedPost = postRepository.save(post);

        // 영상인데 썸네일이 없으면, 커밋 이후 첫 프레임을 추출해 자동으로 채움
        if (request.type() == PostType.VIDEO && (request.thumbnailUrl() == null || request.thumbnailUrl().isBlank())) {
            eventPublisher.publishEvent(new PostVideoThumbnailRequestedEvent(savedPost.getId(), request.mediaUrls().get(0)));
        }

        notifyFollowersAfterCommit(savedPost);

        return PostCreateResponse.from(savedPost);
    }

    // 영상 첫 프레임 자동 추출 결과를 반영 (직접 지정한 썸네일이 없을 때만)
    @Transactional
    public void applyGeneratedThumbnail(Long postId, String thumbnailUrl) {
        postRepository.findById(postId).ifPresent(post -> {
            if (post.getThumbnailUrl() == null) {
                post.update(null, null, thumbnailUrl);
            }
        });
    }

    // 콘텐츠 목록 조회 (커서 페이지네이션, type 필터링 가능)
    public PostListResponse getPosts(Long bandId, PostType type, Long cursor, Integer size) {
        getBand(bandId);

        int pageSize = (size != null) ? size : DEFAULT_PAGE_SIZE;
        long cursorValue = (cursor != null) ? cursor : Long.MAX_VALUE;
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        List<Post> posts = (type != null)
                ? postRepository.findByBand_IdAndTypeAndIdLessThanOrderByIdDesc(bandId, type, cursorValue, pageable)
                : postRepository.findByBand_IdAndIdLessThanOrderByIdDesc(bandId, cursorValue, pageable);

        boolean hasNext = posts.size() > pageSize;
        List<Post> content = hasNext ? posts.subList(0, pageSize) : posts;

        List<PostSummaryResponse> summaries = content.stream()
                .map(PostSummaryResponse::from)
                .toList();

        Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;

        return PostListResponse.of(summaries, hasNext, nextCursor);
    }

    // 콘텐츠 상세 조회 (수정 화면 prefill 겸용)
    public PostResponse getPostDetail(Long postId) {
        return PostResponse.from(getPost(postId));
    }

    // 콘텐츠 수정 (밴드 멤버만 가능, type은 수정 불가, 미디어/태그는 전달 시 전체 교체)
    @Transactional
    public PostUpdateResponse updatePost(Long userId, Long postId, PostUpdateRequest request) {
        Post post = getPost(postId);
        validateBandMember(post.getBand(), userId);
        validateTagCount(request.tags());

        post.update(request.title(), request.description(), request.thumbnailUrl());

        if (request.mediaUrls() != null) {
            validateMediaUrls(post.getType(), request.mediaUrls());
            post.clearMedia();
            addMediaList(post, request.mediaUrls());
        }

        if (request.tags() != null) {
            post.clearTags();
            addTagList(post, request.tags());
        }

        return PostUpdateResponse.from(post);
    }

    // 콘텐츠 삭제 (밴드 멤버만 가능)
    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = getPost(postId);
        validateBandMember(post.getBand(), userId);

        postRepository.delete(post);
    }

    private void addMediaList(Post post, List<String> mediaUrls) {
        if (mediaUrls == null) return;
        for (int i = 0; i < mediaUrls.size(); i++) {
            post.addMedia(mediaUrls.get(i), i);
        }
    }

    private void addTagList(Post post, List<String> tags) {
        if (tags == null) return;
        for (String tag : tags) {
            post.addTag(tag);
        }
    }

    private Band getBand(Long bandId) {
        return bandRepository.findById(bandId)
                .orElseThrow(() -> new BandException(BandErrorCode.BAND_NOT_FOUND));
    }

    private Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.POST_NOT_FOUND));
    }

    private void validateBandMember(Band band, Long userId) {
        if (!bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(band.getId(), userId, BandMemberStatus.ACCEPTED)) {
            throw new PostException(PostErrorCode.NOT_POST_BAND_MEMBER);
        }
    }

    private void validateTagCount(List<String> tags) {
        if (tags != null && tags.size() > MAX_TAG_COUNT) {
            throw new PostException(PostErrorCode.TAG_LIMIT_EXCEEDED);
        }
    }

    private void validateMediaUrls(PostType type, List<String> mediaUrls) {
        switch (type) {
            case PHOTO -> {
                if (mediaUrls == null || mediaUrls.isEmpty()) {
                    throw new PostException(PostErrorCode.PHOTO_MEDIA_REQUIRED);
                }
            }
            case VIDEO -> {
                if (mediaUrls == null || mediaUrls.size() != 1) {
                    throw new PostException(PostErrorCode.INVALID_VIDEO_MEDIA_COUNT);
                }
            }
            case TEXT -> {
                if (mediaUrls != null && !mediaUrls.isEmpty()) {
                    throw new PostException(PostErrorCode.TEXT_MEDIA_NOT_ALLOWED);
                }
            }
        }
    }

    // 게시물 등록 트랜잭션이 완료된 후 밴드 팔로워에게 알림을 발송
    private void notifyFollowersAfterCommit(Post post) {
        List<Long> receiverIds = followPort.getFollowerUserIdsByBandId(
                post.getBand().getId()
        );

        if (receiverIds.isEmpty()) {
            return;
        }

        PostPushMessage message = PostPushMessage.created(
                post.getBand().getName(),
                post.getTitle(),
                post.getId()
        );

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notifyPort.notify(receiverIds, message);
                    }
                }
        );
    }
}
