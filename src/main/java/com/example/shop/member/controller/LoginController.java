package com.example.shop.member.controller;

import com.example.shop.global.web.LoginSessionManager;
import com.example.shop.member.dto.LoginRequest;
import com.example.shop.member.dto.MemberResponse;
import com.example.shop.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인/로그아웃. URL이 /api/members 아래가 아니라 /api/login, /api/logout이라 컨트롤러를 따로 뒀다.
 * 흐름: 서비스가 회원 확인(비즈니스 로직) → 세션 매니저가 세션 처리(웹 계층의 일)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class LoginController {

    private final MemberService memberService;
    private final LoginSessionManager loginSessionManager;

    /** 로그인 성공: 200 + 회원 정보. 세션 쿠키(JSESSIONID)는 응답 헤더 Set-Cookie로 나간다 */
    @PostMapping("/login")
    public ResponseEntity<MemberResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest httpRequest) {
        MemberResponse response = memberService.login(request);
        loginSessionManager.login(httpRequest, response.getId(), response.getRole());
        return ResponseEntity.ok(response);
    }

    /** 로그아웃: 돌려줄 내용이 없으므로 204 No Content */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        loginSessionManager.logout(httpRequest);
        return ResponseEntity.noContent().build();
    }
}
