package com.example.shop.member.service;

import com.example.shop.global.exception.BusinessException;
import com.example.shop.global.exception.ErrorCode;
import com.example.shop.member.domain.Member;
import com.example.shop.member.dto.MemberResponse;
import com.example.shop.member.dto.MemberSignupRequest;
import com.example.shop.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

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
}
