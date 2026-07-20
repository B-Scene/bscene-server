package com.umc.bscene.domain.user.dto.response;

import java.util.List;

public record MyProfileResponse(
        List<MyBandProfile> bandProfiles,
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
