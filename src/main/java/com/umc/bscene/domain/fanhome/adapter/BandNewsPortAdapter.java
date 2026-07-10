package com.umc.bscene.domain.fanhome.adapter;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.fanhome.dto.response.BandNewsItem;
import com.umc.bscene.domain.fanhome.port.BandNewsPort;
import com.umc.bscene.domain.post.enums.PostType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 임시 스텁 (provider = post 도메인).
 */
public class BandNewsPortAdapter implements BandNewsPort {

    @Override
    public List<BandNewsItem> findRecentNews(List<Long> bandIds, int limit) {
        return List.of(
                new BandNewsItem(
                        1L, "WAVY", "https://dummy.img/wavy.png",
                        Genre.INDIE_POP, Region.SEOUL,
                        10L, PostType.PHOTO, "https://dummy.img/post10.png",
                        "홍대 롤링홀 라이브", "다음주 홍대 롤링홀에서 라이브 공연이 예정되어있어요!",
                        List.of("홍대", "정기공연", "인디팝"), LocalDateTime.now().minusHours(2)
                ),
                new BandNewsItem(
                        2L, "DAYBREAK", "https://dummy.img/daybreak.png",
                        Genre.ROCK, Region.SEOUL,
                        11L, PostType.TEXT, null,
                        "신곡 발매 기념 라이브", "다음주 신곡 발매 기념 라이브가 예정되어있어요! 많은 기대 부탁드립니다.",
                        List.of("홍대", "라이브", "메탈"), LocalDateTime.now().minusHours(5)
                )
        );
    }
}
