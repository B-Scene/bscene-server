package com.umc.bscene.domain.post.service;

import com.umc.bscene.domain.post.dto.request.PostCommentRequest;
import com.umc.bscene.domain.post.dto.response.PostCommentListResponse;
import com.umc.bscene.domain.post.dto.response.PostCommentListResponse.CommentItem;
import com.umc.bscene.domain.post.dto.response.PostCommentListResponse.FanProfileInfo;
import com.umc.bscene.domain.post.dto.response.PostCommentResponse;
import com.umc.bscene.domain.post.entity.Post;
import com.umc.bscene.domain.post.entity.PostComment;
import com.umc.bscene.domain.post.exception.PostException;
import com.umc.bscene.domain.post.port.UserPort;
import com.umc.bscene.domain.post.repository.PostCommentRepository;
import com.umc.bscene.domain.post.repository.PostRepository;
import com.umc.bscene.domain.post.response.code.PostErrorCode;
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

    // 댓글 목록 조회 (등록순, 커서 기반 무한스크롤)
    // 내 댓글은 첫 페이지(cursor 없음)에만 myComments로 분리해 전부 내려주고, items에서는 내 댓글·차단한 유저 댓글 제외
    public PostCommentListResponse getComments(Long userId, Long postId, Long cursor, Integer size) {
        getPost(postId);

        int pageSize = (size == null) ? DEFAULT_PAGE_SIZE : Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        // 첫 페이지에만 내 댓글 전체를 내려줌 (프론트가 목록 맨 위에 고정 표시)
        List<CommentItem> myComments = (cursor == null)
                ? toItems(postCommentRepository.findMyComments(postId, userId))
                : List.of();

        // items 제외 대상 : 차단한 유저 + 나 자신 (내 댓글은 myComments로 이미 내려감)
        Set<Long> excludedUserIds = new HashSet<>(userPort.findBlockedUserIds(userId));
        excludedUserIds.add(userId);

        Slice<PostComment> slice = postCommentRepository.findComments(
                postId, cursor, excludedUserIds, PageRequest.of(0, pageSize));

        List<CommentItem> items = toItems(slice.getContent());
        Long nextCursor = slice.hasNext() ? slice.getContent().getLast().getId() : null;

        return new PostCommentListResponse(myComments, items, slice.hasNext(), nextCursor);
    }

    // 댓글 작성
    @Transactional
    public PostCommentResponse createComment(Long userId, Long postId, PostCommentRequest request) {
        Post post = getPost(postId);

        PostComment comment = postCommentRepository.save(PostComment.builder()
                .post(post)
                .user(userRepository.getReferenceById(userId))
                .content(request.content())
                .build());

        return PostCommentResponse.from(comment);
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

    // 작성자 팬 프로필(닉네임/이미지)을 일괄 조회해 매핑 (N+1 방지, 팬 프로필 없는 작성자는 null 표시)
    private List<CommentItem> toItems(List<PostComment> comments) {
        if (comments.isEmpty()) {
            return List.of();
        }

        Set<Long> writerIds = comments.stream()
                .map(comment -> comment.getUser().getId())
                .collect(Collectors.toSet());

        Map<Long, FanProfileInfo> profileByUserId = userPort.findFanProfiles(writerIds);

        return comments.stream()
                .map(comment -> CommentItem.of(comment, profileByUserId))
                .toList();
    }
}
