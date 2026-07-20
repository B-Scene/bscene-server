package com.umc.bscene.domain.user.port;

import com.umc.bscene.domain.user.dto.response.BandMemberResponse;

public interface BandPort {

    BandMemberResponse getActiveBandMemberProfile(Long userId);

}
