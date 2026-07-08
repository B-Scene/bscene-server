package com.umc.bscene.domain.band.dto.request;

import jakarta.validation.constraints.NotNull;

public record BandMemberAcceptRequest(

        @NotNull
        Long sessionProfileId
) {
}
