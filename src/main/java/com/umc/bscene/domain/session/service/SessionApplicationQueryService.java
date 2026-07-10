package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.session.dto.application.response.MySessionApplicationResponse;

import java.util.List;

public interface SessionApplicationQueryService {

    List<MySessionApplicationResponse> getMySessionApplications(Long userId);
}
