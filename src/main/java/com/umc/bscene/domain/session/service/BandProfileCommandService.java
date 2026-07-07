package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.session.dto.profile.request.MyBandProfileUpdateRequest;
import com.umc.bscene.domain.session.dto.profile.response.MyBandProfileResponse;

public interface BandProfileCommandService {

    MyBandProfileResponse saveMySessionProfile(
            Long userId,
            MyBandProfileUpdateRequest request
    );


}
