package com.umc.bscene.domain.search.util;

import com.umc.bscene.domain.search.enums.SearchSortType;
import com.umc.bscene.domain.search.exception.SearchException;
import com.umc.bscene.domain.search.response.code.SearchErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * search_after 커서 인코딩/디코딩.
 * 커서 = 정렬 모드 표식 + 마지막 문서의 정렬값 3개를 "표식|값|값|값" 문자열로 이어
 * base64url로 감싼 불투명 토큰. 값 구성은 모드별 정렬 스펙을 따른다 (decode 참고).
 * 클라이언트는 내용을 해석하지 않고 응답의 nextCursor를 다음 요청에 그대로 전달한다.
 * (base64 포장 : 프론트가 내부 구조에 의존하지 못하게 해 정렬 키 변경의 자유를 확보)
 */
public final class SearchCursor {

    private static final String DELIMITER = "|";

    // 커서를 만든 정렬 모드 표식 — 모드가 다른 커서 재사용(정확도 커서로 인기순 요청 등)을 400으로 방어
    private static final String ACCURACY_MARK = "A";
    private static final String POPULAR_MARK = "P";

    private SearchCursor() {
    }

    public static String encode(List<Object> sortValues, SearchSortType sort) {
        String joined = markOf(sort) + DELIMITER
                + sortValues.get(0) + DELIMITER + sortValues.get(1) + DELIMITER + sortValues.get(2);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(joined.getBytes(StandardCharsets.UTF_8));
    }

    // null/공백이면 첫 페이지(null 반환), 형식이 깨졌거나 정렬 모드가 다르면 400
    public static List<Object> decode(String cursor, SearchSortType sort) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String joined = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = joined.split("\\" + DELIMITER);
            if (parts.length != 4) {
                throw new IllegalArgumentException("invalid cursor parts");
            }
            if (!parts[0].equals(markOf(sort))) {
                throw new IllegalArgumentException("cursor sort mode mismatch");
            }
            // 모드별 정렬값 타입 그대로 복원
            //  - 정확도 : [_score(Double), 날짜 millis(Long), docId(Long)]
            //  - 인기   : [popularity(Long), _score(Double), docId(Long)]
            if (sort == SearchSortType.POPULAR) {
                return List.of(
                        Long.parseLong(parts[1]),
                        Double.parseDouble(parts[2]),
                        Long.parseLong(parts[3])
                );
            }
            return List.of(
                    Double.parseDouble(parts[1]),
                    Long.parseLong(parts[2]),
                    Long.parseLong(parts[3])
            );
        } catch (IllegalArgumentException e) {
            throw new SearchException(SearchErrorCode.INVALID_CURSOR);
        }
    }

    private static String markOf(SearchSortType sort) {
        return (sort == SearchSortType.POPULAR) ? POPULAR_MARK : ACCURACY_MARK;
    }
}
