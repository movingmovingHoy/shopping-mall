package com.example.shop.member.domain;

/**
 * 회원 권한. DB의 member.role(VARCHAR)에는 enum 이름("USER", "ADMIN")이 그대로 저장된다.
 */
public enum Role {
    USER,
    ADMIN
}
