package com.umc.bscene.domain.post.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.post.dto.response.PostDetailResponse;
import com.umc.bscene.domain.post.entity.Post;
import com.umc.bscene.domain.post.enums.PostType;
import com.umc.bscene.domain.post.exception.PostException;
import com.umc.bscene.domain.post.repository.PostCommentRepository;
import com.umc.bscene.domain.post.repository.PostLikeRepository;
import com.umc.bscene.domain.post.repository.PostRepository;
import com.umc.bscene.domain.post.response.code.PostErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

// 팬모드 게시물 상세페이지 조회 단위테스트.
@ExtendWith(MockitoExtension.class)
class PostDetailServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private PostLikeRepository postLikeRepository;
    @Mock
    private PostCommentRepository postCommentRepository;

    private PostDetailService service;

    private static final Long USER_ID = 1L;
    private static final Long BAND_ID = 10L;
    private static final Long POST_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new PostDetailService(postRepository, postLikeRepository, postCommentRepository);
    }

    private Post post() {
        Band band = Band.builder().id(BAND_ID).name("밴드").genre(Genre.HARD_ROCK).region(Region.SEOUL).build();
        return Post.builder().id(POST_ID).band(band).type(PostType.TEXT).title("제목").description("설명").build();
    }

    @Test
    void getPostDetailPage_게시물이_없으면_예외() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        PostException exception =
                assertThrows(PostException.class, () -> service.getPostDetailPage(USER_ID, POST_ID));

        assertEquals(PostErrorCode.POST_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void getPostDetailPage_밴드정보와_카운트와_하트_상태를_포함해_반환한다() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        // commentCount는 차단 여부와 무관한 전체 댓글 수 (countByPost_Id에 차단 조건 없음)
        when(postLikeRepository.countByPost_Id(POST_ID)).thenReturn(5L);
        when(postCommentRepository.countByPost_Id(POST_ID)).thenReturn(3L);
        when(postLikeRepository.existsByPost_IdAndUser_Id(POST_ID, USER_ID)).thenReturn(true);

        PostDetailResponse response = service.getPostDetailPage(USER_ID, POST_ID);

        assertEquals(POST_ID, response.postId());
        assertEquals(BAND_ID, response.band().bandId());
        assertEquals("밴드", response.band().name());
        assertEquals(PostType.TEXT, response.type());
        assertTrue(response.mediaUrls().isEmpty());
        assertEquals(5L, response.likeCount());
        assertEquals(3L, response.commentCount());
        assertTrue(response.isLiked());
    }
}
