package com.umc.bscene.domain.user.dto.response;

public record MyProfileResponse(
        MyBandProfile bandProfiles,
        MyFanProfile fanProfile
){

    public record MyFanProfile(
            Long fanProfileId,
            String profileImageUrl,
            String nickname,
            String email,
            Boolean isActive
    ){ }
}
