package com.umc.bscene.domain.user.port;

import com.umc.bscene.domain.user.dto.response.BandMemberResponse;
import com.umc.bscene.domain.user.dto.response.MyProfileResponse;

import java.util.List;

public interface BandPort {

    BandMemberResponse getActiveBandMemberProfile(Long userId);
    List<MyProfileResponse.MyBandProfile> getAssociatedBandProfiles(Long userId);
}
