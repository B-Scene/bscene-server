package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.session.dto.application.request.MySessionApplicationUpdateRequest;
import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationResponse;

public interface SessionApplicationCommandService {

    MySessionApplicationResponse createSessionApplication(
            Long userId,
            MySessionApplicationUpdateRequest request
    );

    MySessionApplicationResponse updateSessionApplication(
            Long userId,
            Long sessionApplicationId,
            MySessionApplicationUpdateRequest request
    );
}
