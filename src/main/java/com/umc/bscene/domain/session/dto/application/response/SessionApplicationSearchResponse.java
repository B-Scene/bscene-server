package com.umc.bscene.domain.session.dto.application.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonPropertyOrder({"content", "size", "nextCursor", "hasNext"})
public class SessionApplicationSearchResponse {

    private List<SessionApplicationSearchItemResponse> content;
    private Integer size;
    private Long nextCursor;
    private Boolean hasNext;
}
