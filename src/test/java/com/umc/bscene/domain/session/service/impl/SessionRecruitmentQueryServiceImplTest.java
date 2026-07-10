package com.umc.bscene.domain.session.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SessionRecruitmentQueryServiceImplTest {

    @Test
    @DisplayName("모집 공고는 생성 후 3일이 되기 전까지만 신규 공고이다")
    void isNewRecruitmentUsesThreeDayBoundary() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 11, 10, 0);

        assertThat(SessionRecruitmentQueryServiceImpl.isNewRecruitment(
                createdAt,
                createdAt.plusDays(3).minusNanos(1)
        )).isTrue();
        assertThat(SessionRecruitmentQueryServiceImpl.isNewRecruitment(
                createdAt,
                createdAt.plusDays(3)
        )).isFalse();
        assertThat(SessionRecruitmentQueryServiceImpl.isNewRecruitment(
                createdAt,
                createdAt.plusDays(4)
        )).isFalse();
    }

    @Test
    @DisplayName("생성 시각이 없으면 신규 공고가 아니다")
    void isNewRecruitmentReturnsFalseWithoutCreatedAt() {
        assertThat(SessionRecruitmentQueryServiceImpl.isNewRecruitment(
                null,
                LocalDateTime.of(2026, 7, 11, 10, 0)
        )).isFalse();
    }
}
