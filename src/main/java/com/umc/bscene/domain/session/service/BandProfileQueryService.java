package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.session.dto.profile.response.MyBandProfileResponse;

public interface BandProfileQueryService {

    MyBandProfileResponse getMySessionProfile(Long userId);
}