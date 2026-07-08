package com.umc.bscene.domain.follow.dto.response;

public record FollowResponse(
        Long bandId,
        boolean isFollowing
) {
    public static FollowResponse of(Long bandId, boolean isFollowing) {
        return new FollowResponse(bandId, isFollowing);
    }
}
