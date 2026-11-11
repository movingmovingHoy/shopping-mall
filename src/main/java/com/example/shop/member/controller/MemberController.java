package com.example.shop.member.controller;

import com.example.shop.global.web.Login;
import com.example.shop.member.dto.MemberResponse;
import com.example.shop.member.dto.MemberSignupRequest;
import com.example.shop.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 컨트롤러는 요청 검증(@Valid)과 응답 변환만 한다.
 * 검증 실패/중복 이메일 같은 오류는 ApiExceptionHandler가 처리하므로 여기엔 try-catch가 없다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    /** 회원가입: 새 리소스를 만들었으므로 200이 아니라 201 Created로 응답 */
    @PostMapping
    public ResponseEntity<MemberResponse> signup(@Valid @RequestBody MemberSignupRequest request) {
        MemberResponse response = memberService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 내 정보 조회. 로그인 안 했으면 LoginCheckInterceptor가 먼저 401로 막는다.
     * @Login 덕분에 HttpSession을 직접 꺼내지 않고 회원 id만 받는다.
     */
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> me(@Login Long memberId) {
        return ResponseEntity.ok(memberService.getMyInfo(memberId));
    }
}
