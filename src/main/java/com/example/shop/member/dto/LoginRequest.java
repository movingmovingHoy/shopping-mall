package com.example.shop.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인 요청.
 * 가입 때와 달리 @Email, @Size 같은 형식 검사는 하지 않는다.
 * 형식이 틀린 값은 어차피 DB에 없으니 "이메일 또는 비밀번호가 올바르지 않습니다"로 똑같이 응답하면 된다.
 * 여기서는 빈 값만 막는다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;
}
