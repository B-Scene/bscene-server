package com.umc.bscene.domain.auth.dto.auth.request;

import jakarta.validation.constraints.NotNull;

public record TermAgreementRequest(
        @NotNull
        Long termId,

        @NotNull
        Boolean agreed
) {
}