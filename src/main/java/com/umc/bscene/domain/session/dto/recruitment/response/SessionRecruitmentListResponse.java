package com.umc.bscene.domain.session.dto.recruitment.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
@Getter
@Builder
@JsonPropertyOrder({
        "content",
        "size",
        "nextCursor",
        "hasNext"
})
public class SessionRecruitmentListResponse {

    private List<SessionRecruitmentListItemResponse> content;
    private Integer size;
    private Long nextCursor;
    private Boolean hasNext;
}