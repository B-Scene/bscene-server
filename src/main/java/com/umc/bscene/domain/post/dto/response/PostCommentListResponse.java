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

    // 댓글 아이템
    // FAN  : 팬 프로필 닉네임/이미지로 표시 (팬 프로필이 없는 작성자는 null)
    // BAND : 작성 시점 멤버 프로필 닉네임 + 게시물 밴드의 프로필 이미지로 표시
    public record CommentItem(
            Long commentId,
            String writerMode,                  // FAN | BAND (프론트 뱃지·렌더 분기용)
            String nickname,
            String profileImageUrl,
            String content,
            LocalDateTime createdAt
    ) {

        public static CommentItem of(
                PostComment comment,
                Map<Long, FanProfileInfo> profileByUserId,
                String bandProfileImageUrl
        ) {
            if (comment.getBandMemberProfile() != null) {
                return new CommentItem(
                        comment.getId(),
                        "BAND",
                        comment.getBandMemberProfile().getNickname(),
                        bandProfileImageUrl,
                        comment.getContent(),
                        comment.getCreatedAt()
                );
            }

            FanProfileInfo profile = profileByUserId.get(comment.getUser().getId());

            return new CommentItem(
                    comment.getId(),
                    "FAN",
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
