package com.example.shop.member.controller;

import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.global.web.LoginSessionManager;
import com.example.shop.member.domain.Member;
import com.example.shop.member.dto.MemberResponse;
import com.example.shop.member.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 컨트롤러가 "HTTP 요청 → 응답"을 제대로 하는지 확인한다.
 * - @WebMvcTest(MemberController.class): 이 컨트롤러와 웹 관련 빈(ApiExceptionHandler 포함)만 띄운다.
 *   Service, Mapper, DB는 띄우지 않는다.
 * - @MockitoBean: 스프링 컨테이너에 "가짜 MemberService"를 빈으로 넣는다.
 *   (MemberServiceMockTest의 @Mock은 스프링 없이 쓰는 가짜, 이건 스프링 안에 넣는 가짜)
 * - 서비스 로직(중복 체크, 암호화)은 서비스 테스트에서 이미 검증했으니
 *   여기서는 상태 코드와 JSON 모양만 본다.
 */
@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MemberService memberService;

    @Test
    void 회원가입_성공하면_201과_회원정보를_응답한다() throws Exception {
        // given: 가짜 서비스가 가입 결과를 돌려주도록 대본 작성
        MemberResponse response = MemberResponse.from(Member.create("user@test.com", "encoded", "홍길동"));
        given(memberService.signup(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "user@test.com", "password": "password123", "name": "홍길동"}
                                """))
                .andExpect(status().isCreated())                         // 201
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.password").doesNotExist());     // 비밀번호는 응답에 없어야 한다
    }

    @Test
    void 입력값이_잘못되면_400과_필드별_오류를_응답한다() throws Exception {
        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-email", "password": "password123", "name": "홍길동"}
                                """))
                .andExpect(status().isBadRequest())                      // 400
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
        // @Valid에서 막혔으니 서비스는 아예 호출되면 안 된다
    }

    @Test
    void 이미_가입된_이메일이면_409를_응답한다() throws Exception {
        // given: 가짜 서비스가 중복 예외를 던지도록 대본 작성
        given(memberService.signup(any())).willThrow(new BusinessException(ErrorCode.DUPLICATE_EMAIL));

        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "user@test.com", "password": "password123", "name": "홍길동"}
                                """))
                .andExpect(status().isConflict())                        // 409
                .andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다."));
    }

    /**
     * 세션에 넣은 회원 id(1L)가 @Login을 거쳐 서비스까지 전달되는지 확인한다.
     * (@WebMvcTest가 WebMvcConfig도 띄우므로 인터셉터와 ArgumentResolver가 실제와 똑같이 동작한다)
     */
    @Test
    void 로그인했으면_내_정보를_응답한다() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginSessionManager.LOGIN_MEMBER_ID, 1L);
        MemberResponse response = MemberResponse.from(Member.create("user@test.com", "encoded", "홍길동"));
        given(memberService.getMyInfo(1L)).willReturn(response);

        mockMvc.perform(get("/api/members/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        verify(memberService).getMyInfo(1L);   // 세션의 id가 그대로 넘어갔는지
    }

    @Test
    void 로그인_안_했으면_내_정보_조회는_401() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));
    }
}
