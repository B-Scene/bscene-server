package com.umc.bscene.domain.user.dto.response;

public record MyProfileResponse(
        MyBandProfile bandProfiles,
        MyFanProfile fanProfile
){

    public record MyBandProfile(
            Long bandId,
            String profileImageUrl,
            String bandName,
            String genre,       // Genre enum에서 String 변환 필요
            String region,      // Region enum에서 String 변환 필요
            Boolean isActive
    ){ }

    public record MyFanProfile(
            Long fanProfileId,
            String profileImageUrl,
            String nickname,
            String email,
            Boolean isActive
    ){ }
}
