package com.example.shop.member.dto;

import com.example.shop.member.domain.Member;
import lombok.Getter;

/**
 * 회원 정보 응답. 비밀번호는 절대 응답에 넣지 않는다.
 * (도메인 객체 Member를 그대로 반환하면 password까지 JSON으로 나가기 때문에 DTO로 변환한다)
 */
@Getter
public final class MemberResponse {

    private final Long id;
    private final String email;
    private final String name;

    private MemberResponse(Long id, String email, String name) {
        this.id = id;
        this.email = email;
        this.name = name;
    }

    /** 다른 객체 하나를 변환할 때는 관례상 from을 쓴다 */
    /** Member 도메인 객체를 MemberResponse DTO로 변환한다. -> 응답할때는 password 제외 */
    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getEmail(), member.getName());
    }
}
