package com.umc.bscene.domain.user.dto.response.profile;

public record MyFanProfile(
        Long fanProfileId,
        String profileImageUrl,
        String nickname,
        String email,
        Boolean isActive
) implements MyProfile {
}
