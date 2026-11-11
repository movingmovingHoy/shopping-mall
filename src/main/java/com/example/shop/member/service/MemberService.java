package com.example.shop.member.service;

import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.member.domain.Member;
import com.example.shop.member.dto.LoginRequest;
import com.example.shop.member.dto.MemberResponse;
import com.example.shop.member.dto.MemberSignupRequest;
import com.example.shop.member.mapper.MemberMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class MemberService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 타이밍 공격 방지용 가짜 해시. 없는 이메일로 로그인해도 이 해시로 matches()를 돌려서
     * 있는 이메일일 때와 응답 시간을 비슷하게 맞춘다.
     * 문자열을 하드코딩하지 않고 같은 encoder로 만드는 이유: cost(반복 횟수)가 실제 해시와 같아야 걸리는 시간도 같다.
     */
    private final String dummyHash;

    /**
     * dummyHash를 만들려면 생성자 안에서 passwordEncoder가 필요해서
     * @RequiredArgsConstructor 대신 생성자를 직접 작성했다. (생성자가 하나라 @Autowired는 생략 가능)
     */
    public MemberService(MemberMapper memberMapper, PasswordEncoder passwordEncoder) {
        this.memberMapper = memberMapper;
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing-attack");
    }

    @Transactional
    public MemberResponse signup(MemberSignupRequest request) {
        // 1. 중복 이메일 확인 → 이미 있으면 409
        if (memberMapper.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        // 2. 비밀번호는 평문이 아니라 BCrypt 해시로 저장
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. 저장 (save 후 member.id가 채워짐)
        Member member = Member.create(request.getEmail(), encodedPassword, request.getName());

        try {
            memberMapper.save(member);
        } catch (DuplicateKeyException e) {
            log.warn("동시에 같은 이메일로 회원가입 시도: {}", request.getEmail(), e);
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        return MemberResponse.from(member);
    }

    /**
     * 로그인: 이메일로 회원을 찾고 비밀번호를 비교한다.
     * 세션 처리는 웹 계층(LoginSessionManager)의 일이라 여기서는 하지 않는다.
     * 서비스는 HTTP를 몰라야 테스트하기도 쉽고, 나중에 JWT 같은 다른 방식으로 바꿔도 그대로 쓸 수 있다.
     *
     * 타이밍 공격 방지: 이메일이 없을 때 바로 예외를 던지면 matches()(BCrypt, 약 0.1초)를 건너뛰어 응답이 빨라진다.
     * 그러면 응답 시간만 보고 가입 여부를 알 수 있으므로, 이메일이 없어도 가짜 해시로 matches()를 항상 실행한다.
     */
    @Transactional(readOnly = true)
    public MemberResponse login(LoginRequest request) {
        Member member = memberMapper.findByEmail(request.getEmail()).orElse(null);

        // 회원이 없으면 가짜 해시와 비교 → 결과는 항상 false지만 걸리는 시간은 같다
        String hash = (member != null) ? member.getPassword() : dummyHash;
        boolean matched = passwordEncoder.matches(request.getPassword(), hash);

        if (member == null || !matched) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        return MemberResponse.from(member);
    }

    /** 내 정보 조회. memberId는 컨트롤러가 @Login으로 세션에서 받아 온 값이다 */
    @Transactional(readOnly = true)
    public MemberResponse getMyInfo(Long memberId) {
        Member member = memberMapper.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        return MemberResponse.from(member);
    }
}
