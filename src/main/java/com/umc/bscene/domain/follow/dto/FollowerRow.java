package com.umc.bscene.domain.follow.dto;

// 팔로워 커서 페이징 조회용 리포지토리 프로젝션 (followId는 커서 계산에만 쓰고 응답 DTO에는 노출하지 않음)
public record FollowerRow(
        Long followId,
        Long userId,
        String fanProfileImageUrl,
        String nickname
) {
}
