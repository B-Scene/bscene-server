package com.umc.bscene.domain.session.dto.recruitment.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonPropertyOrder({
        "bandId", "bandName", "bandProfileImageUrl",
        "content", "size", "nextCursor", "hasNext"
})
public class ManagedRecruitmentListResponse {
    private Long bandId;
    private String bandName;
    private String bandProfileImageUrl;
    private List<ManagedRecruitmentItemResponse> content;
    private Integer size;
    private Long nextCursor;
    private Boolean hasNext;
}
