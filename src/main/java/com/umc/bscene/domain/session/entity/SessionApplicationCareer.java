package com.umc.bscene.domain.session.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "session_application_careers")
public class SessionApplicationCareer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_application_career_id")
    private Long sessionApplicationCareerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_application_id", nullable = false)
    private SessionApplication sessionApplication;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "period", nullable = false, length = 100)
    private String period;

    @Column(name = "description", length = 500)
    private String description;

    @Builder
    private SessionApplicationCareer(SessionApplication sessionApplication, String name,
                                     String period, String description) {
        this.sessionApplication = sessionApplication;
        this.name = name;
        this.period = period;
        this.description = description;
    }
}
