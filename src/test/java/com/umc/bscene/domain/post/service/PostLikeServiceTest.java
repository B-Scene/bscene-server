package com.umc.bscene.domain.post.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.post.dto.response.PostLikeResponse;
import com.umc.bscene.domain.post.entity.Post;
import com.umc.bscene.domain.post.entity.PostLike;
import com.umc.bscene.domain.post.enums.PostType;
import com.umc.bscene.domain.post.exception.PostException;
import com.umc.bscene.domain.post.repository.PostLikeRepository;
import com.umc.bscene.domain.post.repository.PostRepository;
import com.umc.bscene.domain.post.response.code.PostErrorCode;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 게시물 좋아요 등록(409)/해제(멱등) 단위테스트.
@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private PostLikeRepository postLikeRepository;
    @Mock
    private UserRepository userRepository;

    private PostLikeService service;

    private static final Long USER_ID = 1L;
    private static final Long BAND_ID = 10L;
    private static final Long POST_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new PostLikeService(postRepository, postLikeRepository, userRepository);
    }

    private Post post() {
        Band band = Band.builder().id(BAND_ID).name("밴드").genre(Genre.HARD_ROCK).region(Region.SEOUL).build();
        return Post.builder().id(POST_ID).band(band).type(PostType.TEXT).title("제목").build();
    }

    // ---------- setLike ----------

    @Test
    void setLike_게시물이_없으면_예외() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        PostException exception =
                assertThrows(PostException.class, () -> service.setLike(USER_ID, POST_ID));

        assertEquals(PostErrorCode.POST_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void setLike_이미_좋아요한_게시물이면_예외() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        when(postLikeRepository.existsByPost_IdAndUser_Id(POST_ID, USER_ID)).thenReturn(true);

        PostException exception =
                assertThrows(PostException.class, () -> service.setLike(USER_ID, POST_ID));

        assertEquals(PostErrorCode.ALREADY_LIKED, exception.getBaseResponseCode());
        verify(postLikeRepository, never()).save(any());
    }

    @Test
    void setLike_동시_요청으로_unique_위반이_나면_409로_변환한다() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        when(postLikeRepository.existsByPost_IdAndUser_Id(POST_ID, USER_ID)).thenReturn(false);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
        when(postLikeRepository.save(any(PostLike.class)))
                .thenThrow(new DataIntegrityViolationException("unique 위반"));

        PostException exception =
                assertThrows(PostException.class, () -> service.setLike(USER_ID, POST_ID));

        assertEquals(PostErrorCode.ALREADY_LIKED, exception.getBaseResponseCode());
    }

    @Test
    void setLike_성공시_변경_후_좋아요_수를_반환한다() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        when(postLikeRepository.existsByPost_IdAndUser_Id(POST_ID, USER_ID)).thenReturn(false);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
        when(postLikeRepository.countByPost_Id(POST_ID)).thenReturn(5L);

        PostLikeResponse response = service.setLike(USER_ID, POST_ID);

        assertEquals(POST_ID, response.postId());
        assertTrue(response.isLiked());
        assertEquals(5L, response.likeCount());
        verify(postLikeRepository).save(any(PostLike.class));
    }

    // ---------- unsetLike ----------

    @Test
    void unsetLike_게시물이_없으면_예외() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        PostException exception =
                assertThrows(PostException.class, () -> service.unsetLike(USER_ID, POST_ID));

        assertEquals(PostErrorCode.POST_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void unsetLike_좋아요_여부와_관계없이_멱등하게_해제한다() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
        when(postLikeRepository.countByPost_Id(POST_ID)).thenReturn(4L);

        PostLikeResponse response = service.unsetLike(USER_ID, POST_ID);

        assertEquals(POST_ID, response.postId());
        assertFalse(response.isLiked());
        assertEquals(4L, response.likeCount());
        verify(postLikeRepository).deleteByPost_IdAndUser_Id(POST_ID, USER_ID);
    }
}
