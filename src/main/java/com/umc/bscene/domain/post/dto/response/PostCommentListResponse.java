package com.umc.bscene.domain.post.dto.response;

import com.umc.bscene.domain.post.entity.PostComment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// 게시물 댓글 목록 조회 응답 (등록순, 커서 기반 무한스크롤)
// myComments는 첫 페이지(cursor 없음)에만 전체가 담기고, 이후 페이지에서는 빈 배열
// items에는 내 댓글과 차단한 사용자의 댓글이 제외됨
public record PostCommentListResponse(
        List<CommentItem> myComments,       // 내가 쓴 댓글 (프론트가 목록 맨 위에 고정 표시)
        List<CommentItem> items,            // 다른 사용자들의 댓글
        boolean hasNext,
        Long nextCursor                     // 다음 페이지 요청 시 cursor로 전달 (마지막 댓글 id, 다음 없으면 null)
) {

    // 댓글 아이템 (작성자 표시는 팬 프로필 닉네임/이미지 — 팬 프로필이 없는 작성자는 null)
    public record CommentItem(
            Long commentId,
            String nickname,
            String profileImageUrl,
            String content,
            LocalDateTime createdAt
    ) {

        public static CommentItem of(PostComment comment, Map<Long, FanProfileInfo> profileByUserId) {
            FanProfileInfo profile = profileByUserId.get(comment.getUser().getId());

            return new CommentItem(
                    comment.getId(),
                    profile == null ? null : profile.nickname(),
                    profile == null ? null : profile.profileImageUrl(),
                    comment.getContent(),
                    comment.getCreatedAt()
            );
        }
    }

    // 작성자 팬 프로필 표시 정보 (서비스에서 일괄 조회해 매핑)
    public record FanProfileInfo(
            String nickname,
            String profileImageUrl
    ) {
    }
}
