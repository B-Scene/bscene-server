package com.umc.bscene.domain.session.dto.application.request;

import jakarta.validation.constraints.NotNull;

public record SessionApplicationStatusRequest(

        @NotNull
        Boolean isApproved
) {
}