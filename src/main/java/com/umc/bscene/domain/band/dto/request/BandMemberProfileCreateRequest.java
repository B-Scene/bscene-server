package com.umc.bscene.domain.band.dto.request;

import com.umc.bscene.domain.session.enums.Part;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BandMemberProfileCreateRequest(

        @NotBlank
        @Size(max = 30)
        String nickname,

        @NotNull
        Part part
) {
}
