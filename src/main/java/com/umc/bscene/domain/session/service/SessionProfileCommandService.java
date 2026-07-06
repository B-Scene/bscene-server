package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.session.dto.profile.request.MySessionProfileUpdateRequest;
import com.umc.bscene.domain.session.dto.profile.response.MySessionProfileResponse;

public interface SessionProfileCommandService {

    MySessionProfileResponse saveMySessionProfile(
            Long userId,
            MySessionProfileUpdateRequest request
    );
}