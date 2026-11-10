package com.example.shop.member.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 회원 도메인.
 * - setter를 열지 않는다. 새 회원은 create()로만 만든다.
 * - 기본 생성자는 MyBatis가 조회 결과를 담을 때 쓴다.
 *   MyBatis는 리플렉션으로 private 생성자와 private 필드에도 접근할 수 있어서
 *   외부에서는 new Member()를 못 쓰게 private으로 막아 둔다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Member {

    private Long id;
    private String email;
    private String password;   // BCrypt로 암호화된 값
    private String name;
    private Role role;
    private LocalDateTime createdAt;

    /** 회원가입용. 권한은 항상 USER로 시작한다. */
    public static Member create(String email, String encodedPassword, String name) {
        Member member = new Member();
        member.email = email;
        member.password = encodedPassword;
        member.name = name;
        member.role = Role.USER;
        return member;
    }
}
