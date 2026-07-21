package com.umc.bscene.domain.session.dto.profile.request;

import com.umc.bscene.domain.user.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Getter
@NoArgsConstructor
public class SessionBasicProfileUpdateRequest {

    @Email(message = "올바른 이메일 형식이어야 합니다.")
    @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
    private String email;

    private Gender gender;

    @URL(message = "프로필 이미지는 올바른 URL 형식이어야 합니다.")
    @Size(max = 500, message = "프로필 이미지 URL은 500자 이하여야 합니다.")
    private String profileImageUrl;
}
