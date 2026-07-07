package com.umc.bscene.domain.stream.dto.response;

public record BandInfoForGetLiveResponse(
        Long broadcasterId,
        BandInfo bandInfo
) {
    record BandInfo(
            String bandName,
            String bandProfileImageUrl
    ) {}

    public BandInfoForGetLiveResponse(Long broadcasterId, BandInfo bandInfo) {
        this.broadcasterId = broadcasterId;
        this.bandInfo = bandInfo;
    }

    public BandInfoForGetLiveResponse(Long broadcasterId, String bandName, String bandProfileImageUrl) {
        this(broadcasterId, new BandInfo(bandName, bandProfileImageUrl));
    }
}
