package com.umc.bscene.domain.band.dto.request;

import com.umc.bscene.domain.session.enums.Part;
import jakarta.validation.constraints.Size;

public record BandMemberProfileUpdateRequest(

        @Size(max = 30)
        String nickname,

        Part part
) {
}
