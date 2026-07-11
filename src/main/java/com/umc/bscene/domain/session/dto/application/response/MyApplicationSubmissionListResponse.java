package com.umc.bscene.domain.session.dto.application.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({"content", "size", "nextCursor", "hasNext"})
public record MyApplicationSubmissionListResponse(
        List<MyApplicationSubmissionItemResponse> content,
        int size,
        Long nextCursor,
        boolean hasNext
) {
}
