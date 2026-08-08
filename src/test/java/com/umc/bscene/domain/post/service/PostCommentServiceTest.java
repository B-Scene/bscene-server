package com.umc.bscene.domain.post.service;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.band.entity.Band;
import com.umc.bscene.domain.band.entity.BandMemberProfile;
import com.umc.bscene.domain.band.repository.BandMemberProfileRepository;
import com.umc.bscene.domain.post.dto.request.PostCommentRequest;
import com.umc.bscene.domain.post.dto.response.PostCommentListResponse;
import com.umc.bscene.domain.post.dto.response.PostCommentListResponse.FanProfileInfo;
import com.umc.bscene.domain.post.dto.response.PostCommentResponse;
import com.umc.bscene.domain.post.entity.Post;
import com.umc.bscene.domain.post.entity.PostComment;
import com.umc.bscene.domain.post.enums.PostType;
import com.umc.bscene.domain.post.exception.PostException;
import com.umc.bscene.domain.post.port.BandPort;
import com.umc.bscene.domain.post.port.UserPort;
import com.umc.bscene.domain.post.repository.PostCommentRepository;
import com.umc.bscene.domain.post.repository.PostRepository;
import com.umc.bscene.domain.post.response.code.PostErrorCode;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 게시물 댓글 목록(내 댓글 분리·차단 필터·커서)/작성/수정/삭제 단위테스트.
@ExtendWith(MockitoExtension.class)
class PostCommentServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private PostCommentRepository postCommentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserPort userPort;
    @Mock
    private BandPort bandPort;
    @Mock
    private BandMemberProfileRepository bandMemberProfileRepository;

    private PostCommentService service;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long BLOCKED_USER_ID = 99L;
    private static final Long BAND_ID = 10L;
    private static final Long POST_ID = 100L;
    private static final Long COMMENT_ID = 1000L;
    private static final Long PROFILE_ID = 77L;

    @BeforeEach
    void setUp() {
        service = new PostCommentService(
                postRepository, postCommentRepository, userRepository, userPort, bandPort, bandMemberProfileRepository);
    }

    private User user(Long id) {
        return User.builder().id(id).build();
    }

    private User user(Long id, UserMode currentMode) {
        return User.builder().id(id).currentMode(currentMode).build();
    }

    private Post post() {
        Band band = Band.builder().id(BAND_ID).name("밴드").genre(Genre.HARD_ROCK).region(Region.SEOUL)
                .profileImageUrl("밴드이미지").build();
        return Post.builder().id(POST_ID).band(band).type(PostType.TEXT).title("제목").description("설명").build();
    }

    private PostComment comment(Long id, Long writerId, String content) {
        return PostComment.builder().id(id).post(post()).user(user(writerId)).content(content).build();
    }

    private BandMemberProfile memberProfile(Long id) {
        return BandMemberProfile.builder().id(id).nickname("밴드닉").build();
    }

    private PostComment bandComment(Long id, Long writerId, String content) {
        return PostComment.builder().id(id).post(post()).user(user(writerId))
                .bandMemberProfile(memberProfile(PROFILE_ID)).content(content).build();
    }

    private void mockPostExists() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.of(post()));
    }

    // ---------- getComments ----------

    @Test
    void getComments_게시물이_없으면_예외() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        PostException exception =
                assertThrows(PostException.class, () -> service.getComments(USER_ID, POST_ID, null, null));

        assertEquals(PostErrorCode.POST_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void getComments_첫_페이지에는_내_댓글을_분리해_전부_내려준다() {
        mockPostExists();
        when(userPort.findBlockedUserIds(USER_ID)).thenReturn(Set.of());
        when(postCommentRepository.findMyComments(POST_ID, USER_ID))
                .thenReturn(List.of(comment(1L, USER_ID, "내 댓글")));
        when(postCommentRepository.findComments(eq(POST_ID), isNull(), eq(USER_ID), any(), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(comment(2L, OTHER_USER_ID, "남의 댓글")),
                        PageRequest.of(0, 10), false));
        when(userPort.findFanProfiles(any()))
                .thenReturn(Map.of(USER_ID, new FanProfileInfo("나", null),
                        OTHER_USER_ID, new FanProfileInfo("남", null)));

        PostCommentListResponse response = service.getComments(USER_ID, POST_ID, null, null);

        assertEquals(1, response.myComments().size());
        assertEquals("내 댓글", response.myComments().get(0).content());
        assertEquals(1, response.items().size());
        assertEquals("남의 댓글", response.items().get(0).content());
    }

    @Test
    void getComments_다음_페이지부터는_내_댓글을_다시_내려주지_않는다() {
        mockPostExists();
        when(userPort.findBlockedUserIds(USER_ID)).thenReturn(Set.of());
        when(postCommentRepository.findComments(eq(POST_ID), eq(5L), eq(USER_ID), any(), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 10), false));

        PostCommentListResponse response = service.getComments(USER_ID, POST_ID, 5L, null);

        assertTrue(response.myComments().isEmpty());
        verify(postCommentRepository, never()).findMyComments(anyLong(), anyLong());
    }

    @Test
    void getComments_차단한_유저와_본인을_items에서_제외한다() {
        mockPostExists();
        when(userPort.findBlockedUserIds(USER_ID)).thenReturn(Set.of(BLOCKED_USER_ID));
        when(postCommentRepository.findComments(eq(POST_ID), eq(5L), eq(USER_ID), any(), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 10), false));

        service.getComments(USER_ID, POST_ID, 5L, null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<Long>> excludedCaptor = ArgumentCaptor.captor();
        verify(postCommentRepository).findComments(
                eq(POST_ID), eq(5L), eq(USER_ID), excludedCaptor.capture(), any(Pageable.class));
        assertTrue(excludedCaptor.getValue().contains(BLOCKED_USER_ID));
        assertTrue(excludedCaptor.getValue().contains(USER_ID));
    }

    @Test
    void getComments_다음_페이지가_있으면_마지막_댓글_id를_커서로_반환한다() {
        mockPostExists();
        when(userPort.findBlockedUserIds(USER_ID)).thenReturn(Set.of());
        when(postCommentRepository.findComments(eq(POST_ID), eq(5L), eq(USER_ID), any(), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(
                        List.of(comment(6L, OTHER_USER_ID, "댓글6"), comment(7L, OTHER_USER_ID, "댓글7")),
                        PageRequest.of(0, 2), true));
        when(userPort.findFanProfiles(any())).thenReturn(Map.of());

        PostCommentListResponse response = service.getComments(USER_ID, POST_ID, 5L, 2);

        assertTrue(response.hasNext());
        assertEquals(7L, response.nextCursor());
    }

    @Test
    void getComments_다음_페이지가_없으면_커서는_null이다() {
        mockPostExists();
        when(userPort.findBlockedUserIds(USER_ID)).thenReturn(Set.of());
        when(postCommentRepository.findComments(eq(POST_ID), eq(5L), eq(USER_ID), any(), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(comment(6L, OTHER_USER_ID, "댓글6")),
                        PageRequest.of(0, 10), false));
        when(userPort.findFanProfiles(any())).thenReturn(Map.of());

        PostCommentListResponse response = service.getComments(USER_ID, POST_ID, 5L, null);

        assertFalse(response.hasNext());
        assertNull(response.nextCursor());
    }

    @Test
    void getComments_사이즈가_없으면_10_상한을_넘으면_30으로_보정한다() {
        mockPostExists();
        when(userPort.findBlockedUserIds(USER_ID)).thenReturn(Set.of());
        when(postCommentRepository.findComments(eq(POST_ID), eq(5L), eq(USER_ID), any(), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 10), false));

        service.getComments(USER_ID, POST_ID, 5L, null);
        service.getComments(USER_ID, POST_ID, 5L, 100);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.captor();
        verify(postCommentRepository, org.mockito.Mockito.times(2))
                .findComments(eq(POST_ID), eq(5L), eq(USER_ID), any(), pageableCaptor.capture());
        assertEquals(10, pageableCaptor.getAllValues().get(0).getPageSize());
        assertEquals(30, pageableCaptor.getAllValues().get(1).getPageSize());
    }

    @Test
    void getComments_밴드_댓글은_멤버_프로필_닉네임과_밴드_이미지로_표시된다() {
        mockPostExists();
        when(userPort.findBlockedUserIds(USER_ID)).thenReturn(Set.of());
        when(postCommentRepository.findComments(eq(POST_ID), eq(5L), eq(USER_ID), any(), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(bandComment(6L, OTHER_USER_ID, "밴드 댓글")),
                        PageRequest.of(0, 10), false));

        PostCommentListResponse response = service.getComments(USER_ID, POST_ID, 5L, null);

        assertEquals("BAND", response.items().get(0).writerMode());
        assertEquals("밴드닉", response.items().get(0).nickname());
        assertEquals("밴드이미지", response.items().get(0).profileImageUrl());
        // 밴드 댓글만 있으면 팬 프로필 조회 자체를 하지 않는다
        verify(userPort, never()).findFanProfiles(any());
    }

    @Test
    void getComments_팬프로필이_없는_작성자는_닉네임이_null이다() {
        mockPostExists();
        when(userPort.findBlockedUserIds(USER_ID)).thenReturn(Set.of());
        when(postCommentRepository.findComments(eq(POST_ID), eq(5L), eq(USER_ID), any(), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(comment(6L, OTHER_USER_ID, "댓글")),
                        PageRequest.of(0, 10), false));
        // 팬 프로필 없는 작성자 → 맵에 항목 없음
        when(userPort.findFanProfiles(any())).thenReturn(Map.of());

        PostCommentListResponse response = service.getComments(USER_ID, POST_ID, 5L, null);

        assertNull(response.items().get(0).nickname());
        assertEquals("댓글", response.items().get(0).content());
    }

    // ---------- createComment ----------

    @Test
    void createComment_게시물이_없으면_예외() {
        when(postRepository.findById(POST_ID)).thenReturn(Optional.empty());

        PostException exception = assertThrows(PostException.class,
                () -> service.createComment(USER_ID, POST_ID, new PostCommentRequest("댓글")));

        assertEquals(PostErrorCode.POST_NOT_FOUND, exception.getBaseResponseCode());
        verify(postCommentRepository, never()).save(any());
    }

    @Test
    void createComment_팬모드면_팬_명의로_저장하고_반환한다() {
        mockPostExists();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user(USER_ID, UserMode.FAN));
        when(bandPort.isAcceptedMember(BAND_ID, USER_ID)).thenReturn(false);
        when(postCommentRepository.save(any(PostComment.class)))
                .thenReturn(comment(COMMENT_ID, USER_ID, "새 댓글"));

        PostCommentResponse response = service.createComment(USER_ID, POST_ID, new PostCommentRequest("새 댓글"));

        assertEquals(COMMENT_ID, response.commentId());
        assertEquals("새 댓글", response.content());

        ArgumentCaptor<PostComment> captor = ArgumentCaptor.captor();
        verify(postCommentRepository).save(captor.capture());
        assertEquals("새 댓글", captor.getValue().getContent());
        assertNull(captor.getValue().getBandMemberProfile());
    }

    @Test
    void createComment_밴드모드면_게시물_밴드의_멤버_프로필_명의로_저장한다() {
        mockPostExists();
        BandMemberProfile profile = memberProfile(PROFILE_ID);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user(USER_ID, UserMode.BAND));
        when(bandPort.isAcceptedMember(BAND_ID, USER_ID)).thenReturn(true);
        when(bandPort.findMemberProfileId(BAND_ID, USER_ID)).thenReturn(Optional.of(PROFILE_ID));
        when(bandMemberProfileRepository.getReferenceById(PROFILE_ID)).thenReturn(profile);
        when(postCommentRepository.save(any(PostComment.class)))
                .thenReturn(bandComment(COMMENT_ID, USER_ID, "밴드 댓글"));

        service.createComment(USER_ID, POST_ID, new PostCommentRequest("밴드 댓글"));

        ArgumentCaptor<PostComment> captor = ArgumentCaptor.captor();
        verify(postCommentRepository).save(captor.capture());
        assertEquals(profile, captor.getValue().getBandMemberProfile());
    }

    @Test
    void createComment_밴드모드인데_소속_밴드의_게시물이_아니면_예외() {
        mockPostExists();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user(USER_ID, UserMode.BAND));
        when(bandPort.isAcceptedMember(BAND_ID, USER_ID)).thenReturn(false);

        PostException exception = assertThrows(PostException.class,
                () -> service.createComment(USER_ID, POST_ID, new PostCommentRequest("댓글")));

        assertEquals(PostErrorCode.BAND_COMMENT_NOT_MEMBER, exception.getBaseResponseCode());
        verify(postCommentRepository, never()).save(any());
    }

    @Test
    void createComment_밴드모드인데_멤버_프로필이_없으면_예외() {
        mockPostExists();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user(USER_ID, UserMode.BAND));
        when(bandPort.isAcceptedMember(BAND_ID, USER_ID)).thenReturn(true);
        when(bandPort.findMemberProfileId(BAND_ID, USER_ID)).thenReturn(Optional.empty());

        PostException exception = assertThrows(PostException.class,
                () -> service.createComment(USER_ID, POST_ID, new PostCommentRequest("댓글")));

        assertEquals(PostErrorCode.BAND_MEMBER_PROFILE_NOT_FOUND, exception.getBaseResponseCode());
        verify(postCommentRepository, never()).save(any());
    }

    @Test
    void createComment_팬모드인데_소속_밴드의_게시물이면_예외() {
        mockPostExists();
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user(USER_ID, UserMode.FAN));
        when(bandPort.isAcceptedMember(BAND_ID, USER_ID)).thenReturn(true);

        PostException exception = assertThrows(PostException.class,
                () -> service.createComment(USER_ID, POST_ID, new PostCommentRequest("댓글")));

        assertEquals(PostErrorCode.OWN_BAND_FAN_COMMENT_NOT_ALLOWED, exception.getBaseResponseCode());
        verify(postCommentRepository, never()).save(any());
    }

    // ---------- updateComment ----------

    @Test
    void updateComment_댓글이_없으면_예외() {
        when(postCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());

        PostException exception = assertThrows(PostException.class,
                () -> service.updateComment(USER_ID, POST_ID, COMMENT_ID, new PostCommentRequest("수정")));

        assertEquals(PostErrorCode.COMMENT_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void updateComment_다른_게시물의_댓글이면_예외() {
        when(postCommentRepository.findById(COMMENT_ID))
                .thenReturn(Optional.of(comment(COMMENT_ID, USER_ID, "댓글")));

        // 요청 경로의 postId(999)와 댓글이 달린 게시물(100)이 다름
        PostException exception = assertThrows(PostException.class,
                () -> service.updateComment(USER_ID, 999L, COMMENT_ID, new PostCommentRequest("수정")));

        assertEquals(PostErrorCode.COMMENT_NOT_FOUND, exception.getBaseResponseCode());
    }

    @Test
    void updateComment_남의_댓글이면_예외() {
        when(postCommentRepository.findById(COMMENT_ID))
                .thenReturn(Optional.of(comment(COMMENT_ID, OTHER_USER_ID, "남의 댓글")));

        PostException exception = assertThrows(PostException.class,
                () -> service.updateComment(USER_ID, POST_ID, COMMENT_ID, new PostCommentRequest("수정")));

        assertEquals(PostErrorCode.NOT_COMMENT_OWNER, exception.getBaseResponseCode());
    }

    @Test
    void updateComment_성공시_내용이_수정된다() {
        PostComment comment = comment(COMMENT_ID, USER_ID, "원래 내용");
        when(postCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

        PostCommentResponse response =
                service.updateComment(USER_ID, POST_ID, COMMENT_ID, new PostCommentRequest("수정된 내용"));

        assertEquals("수정된 내용", comment.getContent());
        assertEquals("수정된 내용", response.content());
    }

    // ---------- deleteComment ----------

    @Test
    void deleteComment_남의_댓글이면_예외() {
        when(postCommentRepository.findById(COMMENT_ID))
                .thenReturn(Optional.of(comment(COMMENT_ID, OTHER_USER_ID, "남의 댓글")));

        PostException exception = assertThrows(PostException.class,
                () -> service.deleteComment(USER_ID, POST_ID, COMMENT_ID));

        assertEquals(PostErrorCode.NOT_COMMENT_OWNER, exception.getBaseResponseCode());
        verify(postCommentRepository, never()).delete(any());
    }

    @Test
    void deleteComment_성공시_삭제하고_commentId만_반환한다() {
        PostComment comment = comment(COMMENT_ID, USER_ID, "댓글");
        when(postCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));

        PostCommentResponse response = service.deleteComment(USER_ID, POST_ID, COMMENT_ID);

        verify(postCommentRepository).delete(comment);
        assertEquals(COMMENT_ID, response.commentId());
        assertNull(response.content());
        assertNull(response.createdAt());
    }
}
