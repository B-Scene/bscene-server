package com.umc.bscene.domain.band.dto.request;

import com.umc.bscene.domain.band.enums.BandMemberType;
import jakarta.validation.constraints.NotNull;

public record BandInviteLinkCreateRequest(
        @NotNull BandMemberType memberType
) {
}