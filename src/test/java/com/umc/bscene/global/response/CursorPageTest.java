package com.umc.bscene.global.response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CursorPage 팩토리 계약 및 JSON 왕복 검증.
 * <p>
 * 이 클래스는 @Cacheable을 통해 Redis에 JSON으로 저장되며, 역직렬화 경로는
 * @JsonCreator가 붙은 private 생성자 하나뿐이다. 필드명("items"/"pageInfo"/"nextCursor"/"hasNext")이나
 * 생성자 시그니처가 바뀌면 캐시가 조용히 깨지므로 왕복 테스트로 고정한다.
 */
@DisplayName("CursorPage")
class CursorPageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    record Item(Long id, String name) {
    }

    @Nested
    @DisplayName("팩토리 메서드")
    class Factories {

        @Test
        @DisplayName("of()는 전달한 items와 nextCursor/hasNext를 그대로 담는다")
        void ofKeepsAllArguments() {
            CursorPage<String> page = CursorPage.of(List.of("a", "b"), 42L, true);

            assertThat(page.getItems()).containsExactly("a", "b");
            assertThat(page.getPageInfo().nextCursor()).isEqualTo(42L);
            assertThat(page.getPageInfo().hasNext()).isTrue();
        }

        @Test
        @DisplayName("of()에 null 커서와 hasNext=false를 넘기면 마지막 페이지가 된다")
        void ofAcceptsNullCursor() {
            CursorPage<String> page = CursorPage.of(List.of("a"), null, false);

            assertThat(page.getItems()).containsExactly("a");
            assertThat(page.getPageInfo().nextCursor()).isNull();
            assertThat(page.getPageInfo().hasNext()).isFalse();
        }

        @Test
        @DisplayName("of()는 빈 목록도 그대로 담는다")
        void ofAcceptsEmptyItems() {
            CursorPage<String> page = CursorPage.of(List.of(), 7L, true);

            assertThat(page.getItems()).isEmpty();
            assertThat(page.getPageInfo().nextCursor()).isEqualTo(7L);
            assertThat(page.getPageInfo().hasNext()).isTrue();
        }

        @Test
        @DisplayName("ofHasNext()는 hasNext를 항상 true로 고정한다")
        void ofHasNextAlwaysTrue() {
            CursorPage<Item> page = CursorPage.ofHasNext(List.of(new Item(1L, "첫번째")), 100L);

            assertThat(page.getItems()).containsExactly(new Item(1L, "첫번째"));
            assertThat(page.getPageInfo().nextCursor()).isEqualTo(100L);
            assertThat(page.getPageInfo().hasNext()).isTrue();
        }

        @Test
        @DisplayName("ofLastPage()는 커서를 null로, hasNext를 false로 고정한다")
        void ofLastPageHasNoCursor() {
            CursorPage<String> page = CursorPage.ofLastPage(List.of("x", "y", "z"));

            assertThat(page.getItems()).containsExactly("x", "y", "z");
            assertThat(page.getPageInfo().nextCursor()).isNull();
            assertThat(page.getPageInfo().hasNext()).isFalse();
        }

        @Test
        @DisplayName("empty()는 빈 목록 + null 커서 + hasNext=false다")
        void emptyIsFullyEmpty() {
            CursorPage<String> page = CursorPage.empty();

            assertThat(page.getItems()).isNotNull();
            assertThat(page.getItems()).isEmpty();
            assertThat(page.getPageInfo().nextCursor()).isNull();
            assertThat(page.getPageInfo().hasNext()).isFalse();
        }

        @Test
        @DisplayName("empty()는 호출할 때마다 독립적인 인스턴스를 반환한다")
        void emptyReturnsNewInstances() {
            CursorPage<String> first = CursorPage.empty();
            CursorPage<String> second = CursorPage.empty();

            assertThat(first).isNotSameAs(second);
            assertThat(first.getPageInfo()).isEqualTo(second.getPageInfo());
        }
    }

    @Nested
    @DisplayName("Jackson 왕복 (Redis 캐시 계약)")
    class JsonRoundTrip {

        @Test
        @DisplayName("직렬화 JSON은 items/pageInfo/nextCursor/hasNext 필드명을 유지한다")
        void serializesWithStableFieldNames() throws Exception {
            String json = objectMapper.writeValueAsString(CursorPage.ofHasNext(List.of("a"), 5L));

            assertThat(json)
                    .contains("\"items\"")
                    .contains("\"pageInfo\"")
                    .contains("\"nextCursor\"")
                    .contains("\"hasNext\"");
        }

        @Test
        @DisplayName("String 페이지를 직렬화 후 역직렬화하면 items와 pageInfo가 보존된다")
        void roundTripsStringPage() throws Exception {
            CursorPage<String> original = CursorPage.ofHasNext(List.of("첫번째", "두번째"), 42L);

            String json = objectMapper.writeValueAsString(original);
            CursorPage<String> restored = objectMapper.readValue(json, new TypeReference<CursorPage<String>>() {
            });

            assertThat(restored.getItems()).containsExactly("첫번째", "두번째");
            assertThat(restored.getPageInfo().nextCursor()).isEqualTo(42L);
            assertThat(restored.getPageInfo().hasNext()).isTrue();
        }

        @Test
        @DisplayName("레코드 페이지도 필드 값까지 그대로 복원된다")
        void roundTripsRecordPage() throws Exception {
            CursorPage<Item> original = CursorPage.of(List.of(new Item(1L, "가"), new Item(2L, "나")), 2L, true);

            String json = objectMapper.writeValueAsString(original);
            CursorPage<Item> restored = objectMapper.readValue(json, new TypeReference<CursorPage<Item>>() {
            });

            assertThat(restored.getItems()).containsExactly(new Item(1L, "가"), new Item(2L, "나"));
            assertThat(restored.getPageInfo().nextCursor()).isEqualTo(2L);
            assertThat(restored.getPageInfo().hasNext()).isTrue();
        }

        @Test
        @DisplayName("마지막 페이지(null 커서)도 null이 유지된 채 복원된다")
        void roundTripsLastPageWithNullCursor() throws Exception {
            CursorPage<String> original = CursorPage.ofLastPage(List.of("only"));

            String json = objectMapper.writeValueAsString(original);
            CursorPage<String> restored = objectMapper.readValue(json, new TypeReference<CursorPage<String>>() {
            });

            assertThat(restored.getItems()).containsExactly("only");
            assertThat(restored.getPageInfo().nextCursor()).isNull();
            assertThat(restored.getPageInfo().hasNext()).isFalse();
        }

        @Test
        @DisplayName("empty()도 왕복 후 빈 페이지로 복원된다")
        void roundTripsEmptyPage() throws Exception {
            String json = objectMapper.writeValueAsString(CursorPage.empty());
            CursorPage<String> restored = objectMapper.readValue(json, new TypeReference<CursorPage<String>>() {
            });

            assertThat(restored.getItems()).isEmpty();
            assertThat(restored.getPageInfo().nextCursor()).isNull();
            assertThat(restored.getPageInfo().hasNext()).isFalse();
        }
    }
}
