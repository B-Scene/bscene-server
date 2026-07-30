package com.umc.bscene.domain.search.service;

import com.umc.bscene.domain.search.dto.response.RecentSearchListResponse;
import com.umc.bscene.domain.search.entity.FanModeRecentSearch;
import com.umc.bscene.domain.search.exception.SearchException;
import com.umc.bscene.domain.search.repository.FanModeRecentSearchRepository;
import com.umc.bscene.domain.search.response.code.SearchErrorCode;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 팬모드 최근 검색어 (기록/조회/개별 삭제/전체 삭제) 단위테스트.
@ExtendWith(MockitoExtension.class)
class RecentSearchServiceTest {

    @Mock
    private FanModeRecentSearchRepository fanModeRecentSearchRepository;
    @Mock
    private UserRepository userRepository;

    private RecentSearchService service;

    private static final Long USER_ID = 1L;
    private static final Long RECENT_SEARCH_ID = 100L;

    @BeforeEach
    void setUp() {
        service = new RecentSearchService(fanModeRecentSearchRepository, userRepository);
    }

    private FanModeRecentSearch recentSearch(Long id, String keyword, LocalDateTime searchedAt) {
        return FanModeRecentSearch.builder()
                .id(id)
                .user(User.builder().id(USER_ID).build())
                .keyword(keyword)
                .searchedAt(searchedAt)
                .build();
    }

    // ---------- record ----------

    @Test
    void record_100자를_넘는_검색어는_저장하지_않는다() {
        service.record(USER_ID, "가".repeat(101));

        verify(fanModeRecentSearchRepository, never()).findByUser_IdAndKeyword(anyLong(), anyString());
        verify(fanModeRecentSearchRepository, never()).save(any());
    }

    @Test
    void record_이미_있는_검색어면_searchedAt만_갱신한다() {
        LocalDateTime before = LocalDateTime.now().minusDays(1);
        FanModeRecentSearch existing = recentSearch(RECENT_SEARCH_ID, "블루문", before);
        when(fanModeRecentSearchRepository.findByUser_IdAndKeyword(USER_ID, "블루문"))
                .thenReturn(Optional.of(existing));

        service.record(USER_ID, "블루문");

        assertEquals(true, existing.getSearchedAt().isAfter(before));
        verify(fanModeRecentSearchRepository, never()).save(any());
    }

    @Test
    void record_새_검색어면_저장하고_10개_초과분을_삭제한다() {
        when(fanModeRecentSearchRepository.findByUser_IdAndKeyword(USER_ID, "새검색어"))
                .thenReturn(Optional.empty());
        when(userRepository.getReferenceById(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
        // 저장 후 11개 → 가장 오래된 1개가 삭제 대상
        List<FanModeRecentSearch> eleven = LongStream.rangeClosed(1, 11)
                .mapToObj(i -> recentSearch(i, "검색어" + i, LocalDateTime.now().minusMinutes(i)))
                .toList();
        when(fanModeRecentSearchRepository.findAllByUser_IdOrderBySearchedAtDescIdDesc(USER_ID))
                .thenReturn(eleven);

        service.record(USER_ID, "새검색어");

        ArgumentCaptor<FanModeRecentSearch> saveCaptor = ArgumentCaptor.captor();
        verify(fanModeRecentSearchRepository).save(saveCaptor.capture());
        assertEquals("새검색어", saveCaptor.getValue().getKeyword());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FanModeRecentSearch>> deleteCaptor = ArgumentCaptor.captor();
        verify(fanModeRecentSearchRepository).deleteAll(deleteCaptor.capture());
        assertEquals(1, deleteCaptor.getValue().size());
        assertEquals(11L, deleteCaptor.getValue().get(0).getId());
    }

    @Test
    void record_새_검색어여도_10개_이하면_삭제하지_않는다() {
        when(fanModeRecentSearchRepository.findByUser_IdAndKeyword(USER_ID, "새검색어"))
                .thenReturn(Optional.empty());
        when(userRepository.getReferenceById(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
        when(fanModeRecentSearchRepository.findAllByUser_IdOrderBySearchedAtDescIdDesc(USER_ID))
                .thenReturn(List.of(recentSearch(1L, "검색어", LocalDateTime.now())));

        service.record(USER_ID, "새검색어");

        verify(fanModeRecentSearchRepository, never()).deleteAll(anyList());
    }

    // ---------- getRecentSearches ----------

    @Test
    void getRecentSearches_최신순으로_최대_10개만_반환한다() {
        // 동시 검색 경합으로 순간적으로 12개가 저장된 상황
        List<FanModeRecentSearch> twelve = LongStream.rangeClosed(1, 12)
                .mapToObj(i -> recentSearch(i, "검색어" + i, LocalDateTime.now().minusMinutes(i)))
                .toList();
        when(fanModeRecentSearchRepository.findAllByUser_IdOrderBySearchedAtDescIdDesc(USER_ID))
                .thenReturn(twelve);

        RecentSearchListResponse response = service.getRecentSearches(USER_ID);

        assertEquals(10, response.recentSearches().size());
        assertEquals("검색어1", response.recentSearches().get(0).keyword());
    }

    // ---------- delete ----------

    @Test
    void delete_본인_소유가_아니거나_없는_검색어면_예외() {
        when(fanModeRecentSearchRepository.findByIdAndUser_Id(RECENT_SEARCH_ID, USER_ID))
                .thenReturn(Optional.empty());

        SearchException exception =
                assertThrows(SearchException.class, () -> service.delete(USER_ID, RECENT_SEARCH_ID));

        assertEquals(SearchErrorCode.RECENT_SEARCH_NOT_FOUND, exception.getBaseResponseCode());
        verify(fanModeRecentSearchRepository, never()).delete(any());
    }

    @Test
    void delete_성공시_해당_검색어를_삭제한다() {
        FanModeRecentSearch recentSearch = recentSearch(RECENT_SEARCH_ID, "블루문", LocalDateTime.now());
        when(fanModeRecentSearchRepository.findByIdAndUser_Id(RECENT_SEARCH_ID, USER_ID))
                .thenReturn(Optional.of(recentSearch));

        service.delete(USER_ID, RECENT_SEARCH_ID);

        verify(fanModeRecentSearchRepository).delete(recentSearch);
    }

    // ---------- deleteAll ----------

    @Test
    void deleteAll_본인의_검색어_전체_삭제를_위임한다() {
        service.deleteAll(USER_ID);

        verify(fanModeRecentSearchRepository).deleteAllByUser_Id(USER_ID);
    }
}
