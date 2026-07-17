package com.umc.bscene.domain.search.util;

import com.umc.bscene.domain.search.exception.SearchException;
import com.umc.bscene.domain.search.response.code.SearchErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * search_after 커서 인코딩/디코딩.
 * 커서 = 마지막 문서의 정렬값 [_score, 날짜(epoch millis), docId]를
 * "점수|날짜|docId" 문자열로 이어 base64url로 감싼 불투명 토큰.
 * 클라이언트는 내용을 해석하지 않고 응답의 nextCursor를 다음 요청에 그대로 전달한다.
 * (base64 포장 : 프론트가 내부 구조에 의존하지 못하게 해 정렬 키 변경의 자유를 확보)
 */
public final class SearchCursor {

    private static final String DELIMITER = "|";

    private SearchCursor() {
    }

    public static String encode(List<Object> sortValues) {
        String joined = sortValues.get(0) + DELIMITER + sortValues.get(1) + DELIMITER + sortValues.get(2);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(joined.getBytes(StandardCharsets.UTF_8));
    }

    // null/공백이면 첫 페이지(null 반환), 형식이 깨졌으면 400
    public static List<Object> decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String joined = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = joined.split("\\" + DELIMITER);
            if (parts.length != 3) {
                throw new IllegalArgumentException("invalid cursor parts");
            }
            // 정렬 스펙 [_score(Double), 날짜 millis(Long), docId(Long)] 타입 그대로 복원
            return List.of(
                    Double.parseDouble(parts[0]),
                    Long.parseLong(parts[1]),
                    Long.parseLong(parts[2])
            );
        } catch (IllegalArgumentException e) {
            throw new SearchException(SearchErrorCode.INVALID_CURSOR);
        }
    }
}
