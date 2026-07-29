package com.umc.bscene.domain.post.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.exception.BandException;
import com.umc.bscene.domain.band.repository.BandMemberRepository;
import com.umc.bscene.domain.band.repository.BandRepository;
import com.umc.bscene.domain.band.response.code.BandErrorCode;
import com.umc.bscene.domain.post.dto.request.PostCreateRequest;
import com.umc.bscene.domain.post.dto.request.PostUpdateRequest;
import com.umc.bscene.domain.post.dto.response.PostCreateResponse;
import com.umc.bscene.domain.post.dto.response.PostListResponse;
import com.umc.bscene.domain.post.dto.response.PostResponse;
import com.umc.bscene.domain.post.dto.response.PostUpdateResponse;
import com.umc.bscene.domain.post.entity.Post;
import com.umc.bscene.domain.post.enums.PostType;
import com.umc.bscene.domain.post.event.PostVideoThumbnailRequestedEvent;
import com.umc.bscene.domain.post.exception.PostException;
import com.umc.bscene.domain.post.port.FollowPort;
import com.umc.bscene.domain.post.port.NotifyPort;
import com.umc.bscene.domain.post.repository.PostRepository;
import com.umc.bscene.domain.post.response.code.PostErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private BandRepository bandRepository;
    @Mock
    private BandMemberRepository bandMemberRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private FollowPort followPort;
    @Mock
    private NotifyPort notifyPort;

    private PostService service;

    private static final Long USER_ID = 1L;
    private static final Long BAND_ID = 10L;
    private static final Long POST_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new PostService(postRepository, bandRepository, bandMemberRepository, eventPublisher, followPort, notifyPort);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private Band band() {
        return Band.builder().id(BAND_ID).name("밴드").genre(Genre.HARD_ROCK).region(Region.SEOUL).build();
    }

    private Post post(PostType type) {
        return Post.builder().id(POST_ID).band(band()).type(type).title("제목").description("설명").build();
    }

    private void mockBandMember(boolean isMember) {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band()));
        when(bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(eq(BAND_ID), eq(USER_ID), any()))
                .thenReturn(isMember);
    }

    // ---------- createPost ----------

    @Test
    void createPost_밴드가_없으면_예외() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.empty());
        PostCreateRequest request = new PostCreateRequest(PostType.TEXT, "제목", null, List.of(), null, List.of());

        BandException exception = assertThrows(BandException.class,
                () -> service.createPost(USER_ID, BAND_ID, request));

        assertEquals(BandErrorCode.BAND_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void createPost_밴드멤버가_아니면_예외() {
        mockBandMember(false);
        PostCreateRequest request = new PostCreateRequest(PostType.TEXT, "제목", null, List.of(), null, List.of());

        PostException exception = assertThrows(PostException.class,
                () -> service.createPost(USER_ID, BAND_ID, request));

        assertEquals(PostErrorCode.NOT_POST_BAND_MEMBER, exception.getBaseResponseCode());
    }

    @Test
    void createPost_태그가_8개초과면_예외() {
        mockBandMember(true);
        List<String> tooManyTags = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9");
        PostCreateRequest request = new PostCreateRequest(PostType.TEXT, "제목", null, List.of(), null, tooManyTags);

        PostException exception = assertThrows(PostException.class,
                () -> service.createPost(USER_ID, BAND_ID, request));

        assertEquals(PostErrorCode.TAG_LIMIT_EXCEEDED, exception.getBaseResponseCode());
    }

    @Test
    void createPost_사진타입인데_미디어가_없으면_예외() {
        mockBandMember(true);
        PostCreateRequest request = new PostCreateRequest(PostType.PHOTO, "제목", null, List.of(), null, List.of());

        PostException exception = assertThrows(PostException.class,
                () -> service.createPost(USER_ID, BAND_ID, request));

        assertEquals(PostErrorCode.PHOTO_MEDIA_REQUIRED, exception.getBaseResponseCode());
    }

    @Test
    void createPost_영상타입인데_미디어가_2개면_예외() {
        mockBandMember(true);
        PostCreateRequest request = new PostCreateRequest(
                PostType.VIDEO, "제목", null, List.of("a.mp4", "b.mp4"), null, List.of()
        );

        PostException exception = assertThrows(PostException.class,
                () -> service.createPost(USER_ID, BAND_ID, request));

        assertEquals(PostErrorCode.INVALID_VIDEO_MEDIA_COUNT, exception.getBaseResponseCode());
    }

    @Test
    void createPost_글타입인데_미디어가_있으면_예외() {
        mockBandMember(true);
        PostCreateRequest request = new PostCreateRequest(PostType.TEXT, "제목", null, List.of("a.jpg"), null, List.of());

        PostException exception = assertThrows(PostException.class,
                () -> service.createPost(USER_ID, BAND_ID, request));

        assertEquals(PostErrorCode.TEXT_MEDIA_NOT_ALLOWED, exception.getBaseResponseCode());
    }

    @Test
    void createPost_영상이고_썸네일이_없으면_자동추출_이벤트가_발행된다() {
        mockBandMember(true);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(followPort.getFollowerUserIdsByBandId(BAND_ID)).thenReturn(List.of());
        PostCreateRequest request = new PostCreateRequest(
                PostType.VIDEO, "제목", null, List.of("video.mp4"), null, List.of()
        );

        service.createPost(USER_ID, BAND_ID, request);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());
        assertEquals(1, captor.getAllValues().stream()
                .filter(event -> event instanceof PostVideoThumbnailRequestedEvent)
                .count());
    }

    @Test
    void createPost_성공시_등록한_콘텐츠를_반환한다() {
        mockBandMember(true);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(followPort.getFollowerUserIdsByBandId(BAND_ID)).thenReturn(List.of());
        PostCreateRequest request = new PostCreateRequest(PostType.TEXT, "제목", "설명", List.of(), null, List.of("태그"));

        PostCreateResponse response = service.createPost(USER_ID, BAND_ID, request);

        assertEquals("제목", response.title());
        assertEquals(List.of("태그"), response.tags());
    }

    // ---------- getPosts ----------

    @Test
    void getPosts_타입_필터가_있으면_필터된_레포지토리를_호출한다() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band()));
        when(postRepository.findByBand_IdAndTypeAndIdLessThanOrderByIdDesc(any(), any(), any(), any()))
                .thenReturn(List.of());

        PostListResponse response = service.getPosts(BAND_ID, PostType.PHOTO, null, null);

        assertEquals(0, response.posts().size());
        verify(postRepository, never()).findByBand_IdAndIdLessThanOrderByIdDesc(any(), any(), any());
    }

    @Test
    void getPosts_결과가_페이지크기보다_많으면_hasNext가_true다() {
        when(bandRepository.findById(BAND_ID)).thenReturn(Optional.of(band()));
        List<Post> threeExtra = List.of(post(PostType.TEXT), post(PostType.TEXT), post(PostType.TEXT));
        when(postRepository.findByBand_IdAndIdLessThanOrderByIdDesc(any(), any(), any())).thenReturn(threeExtra);

        PostListResponse response = service.getPosts(BAND_ID, null, null, 2);

        assertEquals(2, response.posts().size());
        assertEquals(true, response.hasNext());
    }

    // ---------- getPostDetail ----------

    @Test
    void getPostDetail_존재하지_않으면_예외() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        PostException exception = assertThrows(PostException.class, () -> service.getPostDetail(POST_ID));

        assertEquals(PostErrorCode.POST_NOT_FOUND, exception.getBaseResponseCode());
    }

    // ---------- updatePost ----------

    @Test
    void updatePost_밴드멤버가_아니면_예외() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post(PostType.TEXT)));
        when(bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(eq(BAND_ID), eq(USER_ID), any())).thenReturn(false);
        PostUpdateRequest request = new PostUpdateRequest("새제목", null, null, null, null);

        PostException exception = assertThrows(PostException.class,
                () -> service.updatePost(USER_ID, POST_ID, request));

        assertEquals(PostErrorCode.NOT_POST_BAND_MEMBER, exception.getBaseResponseCode());
    }

    @Test
    void updatePost_성공시_제목이_수정된다() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post(PostType.TEXT)));
        when(bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(eq(BAND_ID), eq(USER_ID), any())).thenReturn(true);
        PostUpdateRequest request = new PostUpdateRequest("새제목", null, null, null, null);

        PostUpdateResponse response = service.updatePost(USER_ID, POST_ID, request);

        assertEquals("새제목", response.title());
    }

    @Test
    void updatePost_미디어가_전달되면_기존_사진타입_검증을_통과해야한다() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post(PostType.PHOTO)));
        when(bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(eq(BAND_ID), eq(USER_ID), any())).thenReturn(true);
        PostUpdateRequest request = new PostUpdateRequest(null, null, List.of(), null, null);

        PostException exception = assertThrows(PostException.class,
                () -> service.updatePost(USER_ID, POST_ID, request));

        assertEquals(PostErrorCode.PHOTO_MEDIA_REQUIRED, exception.getBaseResponseCode());
    }

    // ---------- deletePost ----------

    @Test
    void deletePost_밴드멤버가_아니면_예외() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post(PostType.TEXT)));
        when(bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(eq(BAND_ID), eq(USER_ID), any())).thenReturn(false);

        PostException exception = assertThrows(PostException.class, () -> service.deletePost(USER_ID, POST_ID));

        assertEquals(PostErrorCode.NOT_POST_BAND_MEMBER, exception.getBaseResponseCode());
        verify(postRepository, never()).delete(any());
    }

    @Test
    void deletePost_성공시_삭제된다() {
        Post post = post(PostType.TEXT);
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));
        when(bandMemberRepository.existsByBand_IdAndUser_IdAndStatus(eq(BAND_ID), eq(USER_ID), any())).thenReturn(true);

        service.deletePost(USER_ID, POST_ID);

        verify(postRepository).delete(post);
    }

    // ---------- applyGeneratedThumbnail ----------

    @Test
    void applyGeneratedThumbnail_게시물이_없으면_아무일도_하지않는다() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        service.applyGeneratedThumbnail(POST_ID, "thumb.jpg");

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void applyGeneratedThumbnail_이미_썸네일이_있으면_덮어쓰지않는다() {
        Post post = Post.builder().id(POST_ID).band(band()).type(PostType.VIDEO).title("제목")
                .thumbnailUrl("existing.jpg").build();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        service.applyGeneratedThumbnail(POST_ID, "generated.jpg");

        assertEquals("existing.jpg", post.getThumbnailUrl());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void applyGeneratedThumbnail_썸네일이_없으면_자동생성값으로_채우고_이벤트를_발행한다() {
        Post post = Post.builder().id(POST_ID).band(band()).type(PostType.VIDEO).title("제목").build();
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post));

        service.applyGeneratedThumbnail(POST_ID, "generated.jpg");

        assertEquals("generated.jpg", post.getThumbnailUrl());
        verify(eventPublisher).publishEvent(any(Object.class));
    }
}
