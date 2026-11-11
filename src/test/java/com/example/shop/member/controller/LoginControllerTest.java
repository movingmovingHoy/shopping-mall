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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * - MemberService는 가짜(@MockitoBean): 회원 확인 로직은 서비스 테스트에서 이미 검증했다.
 * - LoginSessionManager는 진짜(@Import): 세션이 정말 만들어지고 없어지는지 확인해야 하기 때문이다.
 *   @WebMvcTest는 @Component를 자동으로 띄우지 않아서 직접 넣어 준다.
 */
@WebMvcTest(LoginController.class)
@Import(LoginSessionManager.class)
class LoginControllerTest {

    private static final String LOGIN_JSON = """
            {"email": "user@test.com", "password": "password123"}
            """;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MemberService memberService;

    @Test
    void 로그인_성공하면_200과_세션에_회원id가_저장된다() throws Exception {
        given(memberService.login(any())).willReturn(memberResponse(1L));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(request().sessionAttribute(LoginSessionManager.LOGIN_MEMBER_ID, 1L));  // 세션에 id 저장됨
    }

    @Test
    void 로그인하면_기존_세션은_버리고_새_세션을_만든다() throws Exception {
        given(memberService.login(any())).willReturn(memberResponse(1L));
        MockHttpSession oldSession = new MockHttpSession();   // 로그인 전부터 갖고 있던 세션

        mockMvc.perform(post("/api/login")
                        .session(oldSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .andExpect(status().isOk());

        assertThat(oldSession.isInvalid()).isTrue();   // 세션 고정 공격 방지
    }

    @Test
    void 이메일이나_비밀번호가_틀리면_401() throws Exception {
        given(memberService.login(any())).willThrow(new BusinessException(ErrorCode.LOGIN_FAILED));

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"))
                .andExpect(request().sessionAttributeDoesNotExist(LoginSessionManager.LOGIN_MEMBER_ID));  // 세션 없음
    }

    @Test
    void 이메일이_비어_있으면_400() throws Exception {
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "", "password": "password123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void 로그아웃하면_204와_세션이_만료된다() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginSessionManager.LOGIN_MEMBER_ID, 1L);   // 로그인된 상태를 흉내

        mockMvc.perform(post("/api/logout").session(session))
                .andExpect(status().isNoContent());

        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void 로그인_안_한_상태로_로그아웃해도_204() throws Exception {
        mockMvc.perform(post("/api/logout"))
                .andExpect(status().isNoContent());
    }

    /**
     * Member에는 id를 넣는 방법이 없어서(setter 없음, id는 DB가 만듦)
     * 테스트에서만 ReflectionTestUtils로 private 필드에 직접 값을 넣는다.
     */
    private MemberResponse memberResponse(Long id) {
        Member member = Member.create("user@test.com", "encoded", "홍길동");
        ReflectionTestUtils.setField(member, "id", id);
        return MemberResponse.from(member);
    }
}
