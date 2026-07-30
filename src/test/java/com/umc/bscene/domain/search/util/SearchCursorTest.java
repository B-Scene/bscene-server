package com.umc.bscene.domain.search.util;

import com.umc.bscene.domain.search.enums.SearchSortType;
import com.umc.bscene.domain.search.exception.SearchException;
import com.umc.bscene.domain.search.response.code.SearchErrorCode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// 탐색 통합검색 search_after 커서 인코딩/디코딩 단위테스트.
// 순수 유틸이라 목 없이 입력 → 출력만 검증한다.
class SearchCursorTest {

    @Test
    void encode_후_decode하면_정확도순_정렬값이_타입_그대로_복원된다() {
        // 정확도순 : [_score(Double), 날짜 millis(Long), docId(Long)]
        List<Object> sortValues = List.of(8.58, 1752537600000L, 42L);

        String cursor = SearchCursor.encode(sortValues, SearchSortType.ACCURACY);
        List<Object> decoded = SearchCursor.decode(cursor, SearchSortType.ACCURACY);

        assertEquals(sortValues, decoded);
    }

    @Test
    void encode_후_decode하면_인기순_정렬값이_타입_그대로_복원된다() {
        // 인기순 : [popularity(Long), _score(Double), docId(Long)]
        List<Object> sortValues = List.of(10L, 8.58, 42L);

        String cursor = SearchCursor.encode(sortValues, SearchSortType.POPULAR);
        List<Object> decoded = SearchCursor.decode(cursor, SearchSortType.POPULAR);

        assertEquals(sortValues, decoded);
    }

    @Test
    void decode_커서가_null이나_공백이면_첫_페이지로_null을_반환한다() {
        assertNull(SearchCursor.decode(null, SearchSortType.ACCURACY));
        assertNull(SearchCursor.decode("  ", SearchSortType.ACCURACY));
    }

    @Test
    void decode_base64가_아닌_문자열이면_예외() {
        SearchException exception = assertThrows(SearchException.class,
                () -> SearchCursor.decode("%%%잘못된커서%%%", SearchSortType.ACCURACY));

        assertEquals(SearchErrorCode.INVALID_CURSOR, exception.getBaseResponseCode());
    }

    @Test
    void decode_구성요소_개수가_다르면_예외() {
        // "표식|값|값|값" 4개여야 하는데 3개뿐인 커서
        String broken = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("A|8.58|42".getBytes(StandardCharsets.UTF_8));

        SearchException exception = assertThrows(SearchException.class,
                () -> SearchCursor.decode(broken, SearchSortType.ACCURACY));

        assertEquals(SearchErrorCode.INVALID_CURSOR, exception.getBaseResponseCode());
    }

    @Test
    void decode_숫자가_아닌_정렬값이면_예외() {
        String broken = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("A|점수아님|1752537600000|42".getBytes(StandardCharsets.UTF_8));

        SearchException exception = assertThrows(SearchException.class,
                () -> SearchCursor.decode(broken, SearchSortType.ACCURACY));

        assertEquals(SearchErrorCode.INVALID_CURSOR, exception.getBaseResponseCode());
    }

    @Test
    void decode_다른_정렬_모드에서_만든_커서면_예외() {
        // 정확도순으로 만든 커서를 인기순 요청에 재사용 → 정렬값 의미가 달라지므로 400
        String accuracyCursor = SearchCursor.encode(List.of(8.58, 1752537600000L, 42L), SearchSortType.ACCURACY);

        SearchException exception = assertThrows(SearchException.class,
                () -> SearchCursor.decode(accuracyCursor, SearchSortType.POPULAR));

        assertEquals(SearchErrorCode.INVALID_CURSOR, exception.getBaseResponseCode());
    }
}
