package com.umc.bscene.domain.session.dto.profile.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.umc.bscene.domain.session.entity.SessionBasicProfile;
import com.umc.bscene.domain.user.entity.User;
import com.umc.bscene.domain.user.enums.Gender;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@JsonPropertyOrder({
        "userId",
        "name",
        "phone",
        "email",
        "gender",
        "birthDate",
        "profileImageUrl"
})
public class SessionBasicProfileResponse {

    private Long userId;
    private String name;
    private String phone;
    private String email;
    private Gender gender;
    private LocalDate birthDate;
    private String profileImageUrl;

    public static SessionBasicProfileResponse of(
            User user,
            SessionBasicProfile profile,
            String accountEmail
    ) {
        return SessionBasicProfileResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .email(profile != null && profile.getEmail() != null
                        ? profile.getEmail() : accountEmail)
                .gender(profile != null && profile.getGender() != null
                        ? profile.getGender() : user.getGender())
                .birthDate(profile != null && profile.getBirthDate() != null
                        ? profile.getBirthDate() : user.getBirthDate())
                .profileImageUrl(profile == null ? null : profile.getProfileImageUrl())
                .build();
    }
}
