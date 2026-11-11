package com.example.shop.member.service;

import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.member.dto.LoginRequest;
import com.example.shop.member.dto.MemberSignupRequest;
import com.example.shop.member.mapper.MemberMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

/**
 * 진짜 DB로는 만들기 어려운 상황(동시 가입)을 Mock으로 흉내 내는 테스트.
 * - 스프링을 띄우지 않는다. Mockito만 쓰기 때문에 DB 없이 아주 빠르게 돈다.
 * - @Mock: 가짜 객체. 우리가 given(...)으로 정해 준 대로만 동작한다.
 * - @InjectMocks: 진짜 MemberService를 만들고, 위의 가짜들을 생성자에 넣어 준다.
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceMockTest {

    @Mock
    MemberMapper memberMapper;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    MemberService memberService;

    @Test
    void 중복체크는_통과했지만_저장할때_UK에_걸리면_DUPLICATE_EMAIL() {
        // given: 다른 사람이 먼저 가입한 직후 상황을 흉내 낸다
        //   - existsByEmail은 false (아직 상대방이 커밋 전이라 안 보임)
        //   - save에서는 DB의 UNIQUE KEY에 걸려 DuplicateKeyException 발생
        MemberSignupRequest request = new MemberSignupRequest("user@test.com", "password123", "홍길동");
        given(memberMapper.existsByEmail(any())).willReturn(false);
        willThrow(new DuplicateKeyException("Duplicate entry")).given(memberMapper).save(any());

        // when & then: 500이 아니라 409용 BusinessException으로 바뀌어 나와야 한다
        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_EMAIL);
    }

    @Test
    void 중복이_아닌_DB_오류는_바꾸지_않고_그대로_던진다() {
        // given: DB 연결이 끊긴 상황 (중복과 상관없는 에러)
        MemberSignupRequest request = new MemberSignupRequest("user@test.com", "password123", "홍길동");
        given(memberMapper.existsByEmail(any())).willReturn(false);
        willThrow(new DataAccessResourceFailureException("DB 연결 실패")).given(memberMapper).save(any());

        // when & then: catch에 안 걸리고 원래 예외가 그대로 나와야 한다 → 핸들러에서 500 처리
        //   catch (Exception e)로 넓게 잡았다면 이 테스트가 실패한다
        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }

    /**
     * 타이밍 공격 방지 확인.
     * 응답 시간을 직접 재면 컴퓨터 상태에 따라 결과가 들쭉날쭉해서 테스트가 불안정하다.
     * 대신 "시간이 같아지는 원인"인 matches() 호출이 실제로 일어났는지를 verify로 확인한다.
     */
    @Test
    void 없는_이메일이어도_비밀번호_비교는_실행한다() {
        given(memberMapper.findByEmail(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.login(new LoginRequest("none@test.com", "password123")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.LOGIN_FAILED);

        // 회원이 없어도 matches()가 1번 호출돼야 한다 (바로 예외를 던졌다면 0번)
        verify(passwordEncoder).matches(eq("password123"), any());
    }
}
