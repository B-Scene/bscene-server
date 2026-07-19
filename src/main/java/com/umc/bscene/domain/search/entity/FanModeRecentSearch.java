package com.umc.bscene.domain.search.entity;

import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 팬모드 최근 검색어. 유저당 최대 10개는 저장 로직(RecentSearchService)에서 관리.
 * (userId, keyword) 유니크 — 같은 검색어를 다시 검색하면 행을 새로 만들지 않고
 * searchedAt만 갱신해 목록 맨 위로 올린다.
 * (BaseEntity의 updatedAt은 필드 변경이 있어야 갱신되므로 검색 시각을 별도 컬럼으로 관리)
 */
@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "FanModeRecentSearch",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"userId", "keyword"})
        }
)
public class FanModeRecentSearch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fanModeRecentSearchId")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(nullable = false)
    private LocalDateTime searchedAt;

    public void updateSearchedAt(LocalDateTime searchedAt) {
        this.searchedAt = searchedAt;
    }
}
