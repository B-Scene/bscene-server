package com.umc.bscene.domain.term.entity;

import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "UserTerms",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"userId", "termId"})
        }
)
public class UserTerms extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    /* 추후 Terms 엔티티 생성시 변경
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "termId", nullable = false)
    private Terms terms;
     */
    @Column(name = "termId", nullable = false)
    private Long termId;

    @Column(name = "isAgreed", nullable = false)
    private Boolean isAgreed;

    @Column(name = "agreedAt", nullable = false)
    private LocalDateTime agreedAt;
}