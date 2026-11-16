package com.umc.bscene.domain.user.entity;

import com.umc.bscene.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.regex.Pattern;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "FanProfile")
public class FanProfile extends BaseEntity {

    // 닉네임 형식: 한글(완성형·자음·모음), 영문, 공백만 허용하고 공백 포함 1~8자.
    // 앞뒤 공백을 잘라낸 값에 적용한다. 온보딩과 내 정보 수정이 같은 규칙을 쓴다.
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z ]{1,8}$");

    public static boolean isValidNickname(String nickname) {
        return nickname != null && NICKNAME_PATTERN.matcher(nickname).matches();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 팬 프로필을 가진 사용자
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false, unique = true)
    private User user;

    // 팬모드에서 사용할 닉네임
    @Column(nullable = false, unique = true, length = 20)
    private String nickname;

    @Column(name = "profile_image_url", length = 2048)
    private String profileImageUrl;

    // 내 정보 수정에서 닉네임 변경
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    // 내 정보 수정에서 프로필 이미지 변경 (null: 기존 값 유지, 빈 문자열: 이미지 삭제)
    public void updateProfileImage(String profileImageUrl) {
        if (profileImageUrl == null) {
            return;
        }
        this.profileImageUrl = profileImageUrl.isBlank() ? null : profileImageUrl.trim();
    }
}
