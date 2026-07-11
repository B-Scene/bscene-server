package com.umc.bscene.domain.fanhome.adapter;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.fanhome.dto.response.FanHomeResponse.RecommendedBandItem;
import com.umc.bscene.domain.fanhome.port.BandPort;

import java.util.List;

/**
 * 임시 스텁 (provider = band 도메인).
 * band 도메인에 BandRecommendationService가 이미 있어 이를 재사용해 구현 가능
 */
public class BandAdapter implements BandPort {

    @Override
    public List<RecommendedBandItem> recommendTopBands(Long userId, int limit) {
        return List.of(
                new RecommendedBandItem(1L, "wave to earth", Genre.INDIE_POP, Region.SEOUL, "https://dummy.img/w2e.png"),
                new RecommendedBandItem(2L, "호피폴라", Genre.ACOUSTIC, Region.SEOUL, "https://dummy.img/hopipolla.png"),
                new RecommendedBandItem(3L, "SURL", Genre.ROCK, Region.GYEONGGI, "https://dummy.img/surl.png")
        );
    }
}
