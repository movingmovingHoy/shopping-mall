package com.example.shop.global.web;

import com.example.shop.member.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 API가 아직 없어서 LoginCheckInterceptorTest처럼 테스트 전용 컨트롤러로 확인한다.
 * WebMvcConfig가 함께 뜨므로 로그인 인터셉터(order 1) → 관리자 인터셉터(order 2) 순서까지 실제와 같다.
 */
@WebMvcTest(controllers = AdminCheckInterceptorTest.TestController.class)
@Import(AdminCheckInterceptorTest.TestController.class)
class AdminCheckInterceptorTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 로그인_안_했으면_403이_아니라_401() throws Exception {
        // 관리자 검사보다 로그인 검사가 먼저 실행되는지 확인
        mockMvc.perform(post("/api/admin/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));
    }

    @Test
    void 일반_회원이면_403() throws Exception {
        mockMvc.perform(post("/api/admin/products").session(loginSession(Role.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_REQUIRED"))
                .andExpect(jsonPath("$.message").value("관리자 권한이 필요합니다."));
    }

    @Test
    void 세션에_role이_없으면_403() throws Exception {
        // role을 알 수 없으면 막는 쪽이 안전하다
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginSessionManager.LOGIN_MEMBER_ID, 1L);

        mockMvc.perform(post("/api/admin/products").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ADMIN_REQUIRED"));
    }

    @Test
    void 관리자면_통과() throws Exception {
        mockMvc.perform(post("/api/admin/products").session(loginSession(Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void 관리자_경로가_아니면_일반_회원도_통과() throws Exception {
        mockMvc.perform(get("/api/members/me").session(loginSession(Role.USER)))
                .andExpect(status().isOk());
    }

    private MockHttpSession loginSession(Role role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginSessionManager.LOGIN_MEMBER_ID, 1L);
        session.setAttribute(LoginSessionManager.LOGIN_MEMBER_ROLE, role);
        return session;
    }

    @RestController
    static class TestController {

        @PostMapping("/api/admin/products")
        String adminProduct() { return "ok"; }

        @GetMapping("/api/members/me")
        String me() { return "ok"; }
    }
}
