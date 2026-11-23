package com.team1.crm.auth.security;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 전역 모델 어드바이스
 * 역할: 모든 컨트롤러에서 공통으로 사용할 수 있도록 현재 로그인한 회원 정보를 Model에 자동 주입
 * 작성자: 황현우
 * 일시: 2026-11-09
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final MemberRepository memberRepository;

    /**
     * 모든 요청 시 실행되어, 뷰(JSP 등)에서 ${loginMember}로 현재 로그인한 회원 정보를 바로 사용 가능하게 함
     */
    @ModelAttribute("loginMember")
    public Member loginMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        String email = null;

        if (principal instanceof CustomUserDetails) {
            email = ((CustomUserDetails) principal).getUsername();
        } else if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
            email = (String) ((org.springframework.security.oauth2.core.user.OAuth2User) principal).getAttributes().get("email");
        }

        if (email == null) {
            return null;
        }

        return memberRepository.findByEmail(email).orElse(null);
    }
}