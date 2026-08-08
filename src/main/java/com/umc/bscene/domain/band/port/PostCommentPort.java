package com.umc.bscene.domain.band.port;

/**
 * 밴드 도메인이 게시물 댓글을 정리하기 위한 포트 (adapter는 post 도메인이 구현).
 * 밴드 명의(멤버 프로필)가 소멸할 때 그 명의로 남은 밴드 댓글을 함께 삭제한다.
 * (댓글이 멤버 프로필을 FK로 참조하므로, 선삭제하지 않으면 프로필 삭제가 FK 제약으로 실패한다)
 */
public interface PostCommentPort {

    // 멤버 제거(강퇴/자진 탈퇴) 시 : 해당 유저가 그 밴드 게시물에 밴드 명의로 쓴 댓글 삭제
    void deleteBandComments(Long bandId, Long userId);

    // 멤버 프로필 삭제 시 : 그 프로필 명의로 쓴 댓글 삭제
    void deleteCommentsByProfileId(Long profileId);
}
