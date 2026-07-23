package com.umc.bscene.domain.user.dto.request;

import com.umc.bscene.domain.session.enums.Part;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 밴드가 수락한 세션 지원 건에 대한 지원자의 최종 확정 요청
// 수락(isAccepted=true) 시 활동명·파트가 필수이며, 조건부 필수 검증은 서비스에서 수행
public record SessionApplyConfirmRequest(
        @NotNull(message = "최종 수락, 거절 상태값은 비어있을 수 없습니다.")
        Boolean isAccepted,

        @Size(max = 30, message = "활동명은 30자 이하여야 합니다.")
        String nickname,

        Part part
) {
}
