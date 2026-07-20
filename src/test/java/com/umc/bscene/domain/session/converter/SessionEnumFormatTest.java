package com.umc.bscene.domain.session.converter;

import com.umc.bscene.domain.auth.enums.onboarding.Genre;
import com.umc.bscene.domain.auth.enums.onboarding.Region;
import com.umc.bscene.domain.session.dto.application.request.MySessionApplicationCreateRequest;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionEnumFormatTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void commonGenreContainsRequestedValues() {
        assertThat(List.of(Genre.values()))
                .containsExactly(
                        Genre.METAL,
                        Genre.BLUES,
                        Genre.PSYCHEDELIC_ROCK,
                        Genre.ALTERNATIVE_ROCK,
                        Genre.INDIE,
                        Genre.ELECTRONIC_ROCK,
                        Genre.JAZZ,
                        Genre.POP,
                        Genre.POP_ROCK,
                        Genre.PUNK_ROCK,
                        Genre.FOLK_ROCK,
                        Genre.HARD_ROCK,
                        Genre.ETC
                );
    }

    @Test
    void commonEnumsKeepEnglishJsonFormat() throws Exception {
        assertThat(objectMapper.writeValueAsString(Genre.METAL)).isEqualTo("\"METAL\"");
        assertThat(objectMapper.writeValueAsString(Region.SEOUL)).isEqualTo("\"SEOUL\"");
        assertThat(objectMapper.readValue("\"METAL\"", Genre.class)).isEqualTo(Genre.METAL);
        assertThat(objectMapper.readValue("\"SEOUL\"", Region.class)).isEqualTo(Region.SEOUL);
    }

    @Test
    void sessionFormatUsesKoreanForRequestAndResponse() throws Exception {
        SessionPayload payload = objectMapper.readValue(
                """
                        {"genre":"메탈","region":"서울"}
                        """,
                SessionPayload.class
        );

        assertThat(payload.genre()).isEqualTo(Genre.METAL);
        assertThat(payload.region()).isEqualTo(Region.SEOUL);
        assertThat(objectMapper.writeValueAsString(payload))
                .isEqualTo("{\"genre\":\"메탈\",\"region\":\"서울\"}");
    }

    @Test
    void applicationCreateRequestUsesSessionKoreanFormat() throws Exception {
        MySessionApplicationCreateRequest request = objectMapper.readValue(
                """
                        {"genre":"인디","region":"서울"}
                        """,
                MySessionApplicationCreateRequest.class
        );

        assertThat(request.getGenre()).isEqualTo(Genre.INDIE);
        assertThat(request.getRegion()).isEqualTo(Region.SEOUL);
    }

    @Test
    void sessionFormatRejectsEnglishCodes() {
        assertThatThrownBy(() -> readSessionPayload(
                """
                        {"genre":"METAL","region":"SEOUL"}
                        """
        )).isInstanceOf(JacksonException.class);
    }

    private SessionPayload readSessionPayload(String json) throws JacksonException {
        return objectMapper.readValue(json, SessionPayload.class);
    }

    private record SessionPayload(
            @SessionGenreFormat Genre genre,
            @SessionRegionFormat Region region
    ) {
    }
}
