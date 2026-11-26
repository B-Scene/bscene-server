package com.umc.bscene.domain.session.repository;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.session.entity.SessionApplication;
import com.umc.bscene.domain.session.enums.Part;
import com.umc.bscene.domain.session.enums.SkillLevel;
import com.umc.bscene.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class SessionApplicationSearchRepositoryTest {

    private static final String DEFAULT_PURPOSE = "기본";
    private static final Long VIEWER_ID = 1L;

    @Autowired
    private SessionApplicationRepository repository;

    @Test
    void pagesThroughMatchGroupsEvenWhenLaterGroupsHaveHigherIds() {
        SessionApplication olderFullMatch = save(2L, Region.SEOUL, Genre.HARD_ROCK);
        SessionApplication newerFullMatch = save(3L, Region.SEOUL, Genre.HARD_ROCK);
        SessionApplication regionMatch = save(4L, Region.SEOUL, Genre.JAZZ);
        SessionApplication genreMatch = save(5L, Region.GYEONGGI, Genre.HARD_ROCK);
        SessionApplication noMatch = save(6L, Region.GYEONGGI, Genre.JAZZ);
        save(VIEWER_ID, Region.SEOUL, Genre.HARD_ROCK);
        save(7L, Region.SEOUL, Genre.HARD_ROCK).updateVisibility(false);
        save(8L, Region.SEOUL, Genre.HARD_ROCK).delete();

        assertThat(repository.searchByPreferredRegionAndGenre(
                VIEWER_ID, DEFAULT_PURPOSE, Region.SEOUL, Genre.HARD_ROCK, null, PageRequest.of(0, 1)))
                .containsExactly(newerFullMatch);
        assertThat(repository.searchByPreferredRegionAndGenre(
                VIEWER_ID, DEFAULT_PURPOSE, Region.SEOUL, Genre.HARD_ROCK,
                newerFullMatch.getSessionApplicationId(), PageRequest.of(0, 2)))
                .containsExactly(olderFullMatch, genreMatch);
        assertThat(repository.searchByPreferredRegionAndGenre(
                VIEWER_ID, DEFAULT_PURPOSE, Region.SEOUL, Genre.HARD_ROCK,
                genreMatch.getSessionApplicationId(), PageRequest.of(0, 3)))
                .containsExactly(regionMatch, noMatch);
    }

    private SessionApplication save(Long userId, Region region, Genre genre) {
        return repository.save(SessionApplication.builder()
                .userId(userId)
                .nickname("뮤지션")
                .title("기본 지원서")
                .purpose(DEFAULT_PURPOSE)
                .part(Part.GUITAR)
                .skillLevel(SkillLevel.INTERMEDIATE)
                .genre(genre)
                .region(region)
                .build());
    }
}
