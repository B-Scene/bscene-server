package com.umc.bscene.domain.session.service;

import com.umc.bscene.domain.session.entity.SessionRecruitmentSearchKeyword;
import com.umc.bscene.domain.session.repository.SessionRecruitmentSearchKeywordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class SessionRecruitmentSearchKeywordServiceTest {

    @Mock
    private SessionRecruitmentSearchKeywordRepository repository;

    private SessionRecruitmentSearchKeywordService service;

    @BeforeEach
    void setUp() {
        service = new SessionRecruitmentSearchKeywordService(repository);
    }

    @Test
    void keepsOnlyTenRecentSearchesAfterRecordingNewKeyword() {
        Long userId = 1L;
        List<SessionRecruitmentSearchKeyword> searches = searches(userId, 11);
        when(repository.findByUserIdAndKeyword(userId, "new-keyword"))
                .thenReturn(Optional.empty());
        when(repository.findAllByUserIdOrderBySearchedAtDesc(userId))
                .thenReturn(searches);

        service.record(userId, "new-keyword");

        verify(repository).deleteAll(searches.subList(10, 11));
    }

    @Test
    void returnsLatestTenSearchesInRepositoryOrder() {
        Long userId = 1L;
        when(repository.findAllByUserIdOrderBySearchedAtDesc(userId))
                .thenReturn(searches(userId, 11));

        var result = service.getAll(userId);

        assertThat(result).hasSize(10);
        assertThat(result)
                .extracting(response -> response.keyword())
                .containsExactly(
                        "keyword-1", "keyword-2", "keyword-3", "keyword-4", "keyword-5",
                        "keyword-6", "keyword-7", "keyword-8", "keyword-9", "keyword-10"
                );
    }

    @Test
    void refreshesDuplicateKeywordWithoutCreatingNewRow() {
        Long userId = 1L;
        SessionRecruitmentSearchKeyword existing = search(userId, "drum");
        when(repository.findByUserIdAndKeyword(userId, "drum"))
                .thenReturn(Optional.of(existing));

        service.record(userId, "drum");

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ignoresNullAndBlankKeyword() {
        service.record(1L, null);
        service.record(1L, "   ");

        verify(repository, never()).findByUserIdAndKeyword(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void trimsWhitespaceBeforeSavingKeyword() {
        Long userId = 1L;
        when(repository.findByUserIdAndKeyword(userId, "드럼"))
                .thenReturn(Optional.empty());
        when(repository.findAllByUserIdOrderBySearchedAtDesc(userId))
                .thenReturn(List.of());

        service.record(userId, "  드럼  ");

        ArgumentCaptor<SessionRecruitmentSearchKeyword> captor =
                ArgumentCaptor.forClass(SessionRecruitmentSearchKeyword.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getKeyword()).isEqualTo("드럼");
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
    }

    @Test
    void truncatesKeywordLongerThanOneHundredCharacters() {
        Long userId = 1L;
        String longKeyword = "가".repeat(101);
        String expected = "가".repeat(100);
        when(repository.findByUserIdAndKeyword(userId, expected))
                .thenReturn(Optional.empty());
        when(repository.findAllByUserIdOrderBySearchedAtDesc(userId))
                .thenReturn(List.of());

        service.record(userId, longKeyword);

        ArgumentCaptor<SessionRecruitmentSearchKeyword> captor =
                ArgumentCaptor.forClass(SessionRecruitmentSearchKeyword.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getKeyword()).hasSize(100);
    }

    @Test
    void deletesOnlyKeywordOwnedByUser() {
        service.delete(1L, 20L);

        verify(repository)
                .deleteBySessionRecruitmentSearchKeywordIdAndUserId(20L, 1L);
    }

    private List<SessionRecruitmentSearchKeyword> searches(Long userId, int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> search(userId, "keyword-" + index))
                .toList();
    }

    private SessionRecruitmentSearchKeyword search(Long userId, String keyword) {
        return SessionRecruitmentSearchKeyword.builder()
                .userId(userId)
                .keyword(keyword)
                .searchedAt(LocalDateTime.now())
                .build();
    }
}
