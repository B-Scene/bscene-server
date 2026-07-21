package com.umc.bscene.domain.session.entity;

import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "session_recruitment_views",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_session_recruitment_view",
                columnNames = {"session_recruitment_id", "user_id"}
        )
)
public class SessionRecruitmentView extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sessionRecruitmentViewId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_recruitment_id", nullable = false)
    private SessionRecruitment sessionRecruitment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    private SessionRecruitmentView(SessionRecruitment sessionRecruitment, User user) {
        this.sessionRecruitment = sessionRecruitment;
        this.user = user;
    }
}
