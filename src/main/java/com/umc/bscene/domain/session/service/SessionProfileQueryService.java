package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.session.dto.profile.response.MySessionProfileResponse;

public interface SessionProfileQueryService {

    MySessionProfileResponse getMySessionProfile(Long userId);
}