package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.band.dto.request.MyBandProfileUpdateRequest;
import com.umc.bscene.domain.band.dto.response.MyBandProfileResponse;

public interface BandProfileCommandService {

    MyBandProfileResponse saveMySessionProfile(
            Long userId,
            MyBandProfileUpdateRequest request
    );


}
