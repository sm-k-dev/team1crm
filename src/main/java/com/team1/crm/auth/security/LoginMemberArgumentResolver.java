package com.team1.crm.auth.security;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 로그인 회원 정보 자동 주입 Resolver
 * 역할: 컨트롤러 메서드에 @LoginMember가 붙은 파라미터가 있으면 현재 로그인된 회원 정보를 찾아 바인딩
 * 작성자: 황현우
 * 일시: 2026-11-09
 */
@Component
@RequiredArgsConstructor
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private final MemberRepository memberRepository;

    /**
     * @LoginMember 어노테이션이 붙어 있고, 파라미터 타입이 Member 클래스인 경우에만 이 Resolver가 동작하도록 설정
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        boolean hasLoginMemberAnnotation = parameter.hasParameterAnnotation(LoginMember.class);
        boolean hasMemberType = Member.class.isAssignableFrom(parameter.getParameterType());
        return hasLoginMemberAnnotation && hasMemberType;
    }

    /**
     * 실제로 파라미터에 넣어줄 객체를 찾아 반환하는 로직
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return null;
        }

        // Spring Security의 인증 객체에서 이메일 정보 추출 (UserDetails 구현체 또는 OAuth2User 기반)
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

        // DB에서 회원을 조회하여 반환
        return memberRepository.findByEmail(email).orElse(null);
    }
}