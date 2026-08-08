package com.umc.bscene.domain.post.service;

import com.umc.bscene.domain.band.entity.BandMemberProfile;
import com.umc.bscene.domain.band.repository.BandMemberProfileRepository;
import com.umc.bscene.domain.post.dto.request.PostCommentRequest;
import com.umc.bscene.domain.post.dto.response.PostCommentListResponse;
import com.umc.bscene.domain.post.dto.response.PostCommentListResponse.CommentItem;
import com.umc.bscene.domain.post.dto.response.PostCommentListResponse.FanProfileInfo;
import com.umc.bscene.domain.post.dto.response.PostCommentResponse;
import com.umc.bscene.domain.post.entity.Post;
import com.umc.bscene.domain.post.entity.PostComment;
import com.umc.bscene.domain.post.exception.PostException;
import com.umc.bscene.domain.post.port.BandPort;
import com.umc.bscene.domain.post.port.UserPort;
import com.umc.bscene.domain.post.repository.PostCommentRepository;
import com.umc.bscene.domain.post.repository.PostRepository;
import com.umc.bscene.domain.post.response.code.PostErrorCode;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.UserMode;
import com.umc.bscene.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostCommentService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 30;

    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;
    private final UserRepository userRepository;
    private final UserPort userPort;
    private final BandPort bandPort;
    private final BandMemberProfileRepository bandMemberProfileRepository;

    // 댓글 목록 조회 (등록순, 커서 기반 무한스크롤)
    // 내 댓글은 첫 페이지(cursor 없음)에만 myComments로 분리해 전부 내려주고, items에서는 내 댓글·차단한 유저 댓글 제외
    public PostCommentListResponse getComments(Long userId, Long postId, Long cursor, Integer size) {
        Post post = getPost(postId);

        int pageSize = (size == null) ? DEFAULT_PAGE_SIZE : Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        // 첫 페이지에만 내 댓글 전체를 내려줌 (프론트가 목록 맨 위에 고정 표시, 밴드 명의 댓글 포함)
        List<CommentItem> myComments = (cursor == null)
                ? toItems(post, postCommentRepository.findMyComments(postId, userId))
                : List.of();

        // items 제외 대상 : 나 자신(전부) + 차단한 유저(팬 댓글만 — 밴드 명의 노출은 쿼리에서 유지)
        Set<Long> excludedUserIds = new HashSet<>(userPort.findBlockedUserIds(userId));
        excludedUserIds.add(userId);

        Slice<PostComment> slice = postCommentRepository.findComments(
                postId, cursor, userId, excludedUserIds, PageRequest.of(0, pageSize));

        List<CommentItem> items = toItems(post, slice.getContent());
        Long nextCursor = slice.hasNext() ? slice.getContent().getLast().getId() : null;

        return new PostCommentListResponse(myComments, items, slice.hasNext(), nextCursor);
    }

    // 댓글 작성 : 팬모드는 팬 명의, 밴드모드는 게시물 밴드의 멤버 프로필 명의로 저장
    @Transactional
    public PostCommentResponse createComment(Long userId, Long postId, PostCommentRequest request) {
        Post post = getPost(postId);
        User user = userRepository.getReferenceById(userId);

        PostComment comment = postCommentRepository.save(PostComment.builder()
                .post(post)
                .user(user)
                .content(request.content())
                .bandMemberProfile(resolveBandIdentity(user, post))
                .build());

        return PostCommentResponse.from(comment);
    }

    // 밴드모드면 게시물 밴드의 멤버 프로필(명의)을, 팬모드면 null을 반환. 모드-멤버십 정책 위반은 여기서 차단
    // - 밴드모드 : 게시물 밴드의 멤버만 작성 가능
    // - 팬모드 : 소속 밴드 게시물에는 작성 불가 (멤버가 팬 명의로 자기 밴드에 댓글 다는 것 방지 — 밴드모드로만)
    private BandMemberProfile resolveBandIdentity(User user, Post post) {
        Long bandId = post.getBand().getId();
        boolean isMember = bandPort.isAcceptedMember(bandId, user.getId());

        if (user.getCurrentMode() == UserMode.BAND) {
            if (!isMember) {
                throw new PostException(PostErrorCode.BAND_COMMENT_NOT_MEMBER);
            }

            Long profileId = bandPort.findMemberProfileId(bandId, user.getId())
                    .orElseThrow(() -> new PostException(PostErrorCode.BAND_MEMBER_PROFILE_NOT_FOUND));

            return bandMemberProfileRepository.getReferenceById(profileId);
        }

        if (isMember) {
            throw new PostException(PostErrorCode.OWN_BAND_FAN_COMMENT_NOT_ALLOWED);
        }

        return null;
    }

    // 댓글 수정 (본인 댓글만)
    @Transactional
    public PostCommentResponse updateComment(Long userId, Long postId, Long commentId, PostCommentRequest request) {
        PostComment comment = getOwnComment(userId, postId, commentId);
        comment.updateContent(request.content());

        return PostCommentResponse.from(comment);
    }

    // 댓글 삭제 (본인 댓글만)
    @Transactional
    public PostCommentResponse deleteComment(Long userId, Long postId, Long commentId) {
        PostComment comment = getOwnComment(userId, postId, commentId);
        postCommentRepository.delete(comment);

        return PostCommentResponse.deleted(commentId);
    }

    private Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostException(PostErrorCode.POST_NOT_FOUND));
    }

    // 댓글 존재 + 해당 게시물 소속 검증 후, 본인 댓글인지 확인
    private PostComment getOwnComment(Long userId, Long postId, Long commentId) {
        PostComment comment = postCommentRepository.findById(commentId)
                .filter(found -> found.getPost().getId().equals(postId))
                .orElseThrow(() -> new PostException(PostErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new PostException(PostErrorCode.NOT_COMMENT_OWNER);
        }

        return comment;
    }

    // 댓글 → 아이템 변환
    // 팬 댓글 : 작성자 팬 프로필(닉네임/이미지)을 일괄 조회해 매핑 (N+1 방지, 팬 프로필 없는 작성자는 null 표시)
    // 밴드 댓글 : 작성 시점 멤버 프로필 닉네임 + 게시물 밴드의 프로필 이미지로 표시
    private List<CommentItem> toItems(Post post, List<PostComment> comments) {
        if (comments.isEmpty()) {
            return List.of();
        }

        Set<Long> fanWriterIds = comments.stream()
                .filter(comment -> comment.getBandMemberProfile() == null)
                .map(comment -> comment.getUser().getId())
                .collect(Collectors.toSet());

        Map<Long, FanProfileInfo> profileByUserId = fanWriterIds.isEmpty()
                ? Map.of()
                : userPort.findFanProfiles(fanWriterIds);

        // 밴드 댓글 표시용 이미지는 게시물 밴드의 프로필 이미지를 공유 (밴드 댓글이 있을 때만 접근)
        String bandProfileImageUrl = comments.stream().anyMatch(comment -> comment.getBandMemberProfile() != null)
                ? post.getBand().getProfileImageUrl()
                : null;

        return comments.stream()
                .map(comment -> CommentItem.of(comment, profileByUserId, bandProfileImageUrl))
                .toList();
    }
}
