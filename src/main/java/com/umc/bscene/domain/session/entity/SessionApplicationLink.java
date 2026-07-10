package com.umc.bscene.domain.session.entity;

import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "session_application_links")
public class SessionApplicationLink extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_application_link_id")
    private Long sessionApplicationLinkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_application_id", nullable = false)
    private SessionApplication sessionApplication;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    private SessionApplicationLink(
            SessionApplication sessionApplication,
            String url
    ) {
        this.sessionApplication = sessionApplication;
        this.url = url;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
