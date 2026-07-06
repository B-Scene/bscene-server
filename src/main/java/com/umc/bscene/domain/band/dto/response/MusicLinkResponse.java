package com.umc.bscene.domain.band.dto.response;

import com.umc.bscene.domain.band.entity.MusicLink;
import com.umc.bscene.domain.band.enums.EtcMusicPlatform;

public record MusicLinkResponse(
        String spotifyUrl,
        String youtubeUrl,
        String soundcloudUrl,
        EtcMusicPlatform etcPlatform,
        String etcUrl,
        String otherUrl
) {
    private static final MusicLinkResponse EMPTY =
            new MusicLinkResponse(null, null, null, null, null, null);

    public static MusicLinkResponse from(MusicLink musicLink) {
        if (musicLink == null) {
            return EMPTY;
        }

        return new MusicLinkResponse(
                musicLink.getSpotifyUrl(),
                musicLink.getYoutubeUrl(),
                musicLink.getSoundcloudUrl(),
                musicLink.getEtcPlatform(),
                musicLink.getEtcUrl(),
                musicLink.getOtherUrl()
        );
    }
}
