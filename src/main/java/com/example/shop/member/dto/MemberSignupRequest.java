package com.example.shop.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원가입 요청.
 * - @NoArgsConstructor: Jackson이 JSON을 객체로 만들 때 사용
 * - @AllArgsConstructor: 테스트 코드에서 요청 객체를 만들 때 사용
 * 최대 길이는 schema.sql의 컬럼 길이에 맞췄다. (email 100, name 50)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberSignupRequest {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 100, message = "이메일은 100자 이하여야 합니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
    private String name;
}
