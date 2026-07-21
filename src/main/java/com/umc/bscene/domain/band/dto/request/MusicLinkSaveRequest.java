package com.umc.bscene.domain.band.dto.request;

import com.umc.bscene.domain.band.enums.EtcMusicPlatform;

public record MusicLinkSaveRequest(
        String spotifyUrl,
        String youtubeUrl,
        String soundcloudUrl,
        EtcMusicPlatform etcPlatform,
        String etcUrl,
        String otherUrl
) {
}
