package com.umc.bscene.domain.stream.service;

import com.umc.bscene.domain.stream.dto.response.ReplayResponse;
import com.umc.bscene.domain.stream.dto.response.StreamReplayResponse;
import com.umc.bscene.domain.stream.enums.ReplaySort;
import com.umc.bscene.global.response.CursorPage;

public interface StreamReplayService {

    void requestReplayUpload(Long userId, Long liveId);

    StreamReplayResponse watchReplay(Long liveId);

    String buildReplayPlaylist(Long liveId);

    CursorPage<ReplayResponse> getAllReplays(Long cursor, int size, ReplaySort sort);

    CursorPage<ReplayResponse> getFollowingReplays(Long userId, Long cursor, int size, ReplaySort sort);


}
