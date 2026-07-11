package com.umc.bscene.domain.session.entity;

import com.umc.bscene.domain.session.enums.ApplicationStatus;
import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "application_submissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_application_submission_recruitment_application",
                columnNames = {"session_recruitment_id", "session_application_id"}
        )
)
public class SessionApplicationSubmission extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_submission_id")
    private Long applicationSubmissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_recruitment_id", nullable = false)
    private SessionRecruitment sessionRecruitment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_application_id", nullable = false)
    private SessionApplication sessionApplication;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @Builder
    private SessionApplicationSubmission(
            SessionRecruitment sessionRecruitment,
            SessionApplication sessionApplication,
            ApplicationStatus status
    ) {
        this.sessionRecruitment = sessionRecruitment;
        this.sessionApplication = sessionApplication;
        this.status = status == null ? ApplicationStatus.PENDING : status;
    }

    public void cancel() {
        this.status = ApplicationStatus.CANCELED;
    }

    public void markChecked() {
        if (this.checkedAt == null) {
            this.checkedAt = LocalDateTime.now();
        }
    }
}
