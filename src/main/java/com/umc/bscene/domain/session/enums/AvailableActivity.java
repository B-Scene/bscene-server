package com.umc.bscene.domain.session.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AvailableActivity {
    REGULAR_REHEARSAL("정기 합주"),
    LIVE_PERFORMANCE("라이브 공연"),
    RECORDING("앨범 및 음원 작업"),
    COMPETITION("경연"),
    MEMBER_TRANSITION("멤버 전환");

    private final String description;

    AvailableActivity(String description) {
        this.description = description;
    }

    @JsonCreator
    public static AvailableActivity fromValue(String value) {
        for (AvailableActivity activity : values()) {
            if (activity.name().equalsIgnoreCase(value) || activity.description.equals(value)) {
                return activity;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 가능한 활동입니다: " + value);
    }

    @JsonValue
    public String getDescription() {
        return description;
    }
}
