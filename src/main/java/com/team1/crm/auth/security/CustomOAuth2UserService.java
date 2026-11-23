package com.team1.crm.auth.security;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.MemberRepository;
import com.team1.crm.auth.EmailAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Collections;

/**
 * 소셜 로그인(Google) 사용자 정보 처리 서비스
 * 역할: 소셜 로그인 성공 시 사용자 정보를 가져와 DB에 저장하거나 업데이트
 * 작성자: 황현우
 * 일시: 2026-11-09
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 현재 로그인 진행 중인 서비스(구글 등)를 구분하는 아이디 (예: google)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // OAuth2 로그인 진행 시 키가 되는 필드값 (PK와 같은 의미, 구글은 기본적으로 "sub")
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // OAuth2Attributes를 통해 소셜 정보를 담는 DTO 생성
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        // 회원 정보 저장 또는 업데이트
        Member member = saveOrUpdate(attributes, registrationId);

        // Spring Security에서 사용할 세션 정보 또는 인증 객체 생성 반환
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + member.getRole().name())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey()
        );
    }

    /**
     * 제공자·고유 ID로 기존 회원을 찾고 이메일만 같은 계정은 자동 연결하지 않는다.
     * 작성자: 김순민 / 작성일: 2026-11-13
     */
    // 작성자: 김순민 / 작성일: 2026-11-13
    Member saveOrUpdate(OAuthAttributes attributes, String registrationId) {
        Object subject = attributes.getAttributes().get(attributes.getNameAttributeKey());
        if (subject == null || subject.toString().isBlank()
                || attributes.getEmail() == null || attributes.getEmail().isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"),
                    "Google 계정 식별 정보를 확인할 수 없습니다.");
        }
        String providerId = subject.toString();
        Member linked = memberRepository.findByProviderAndProviderId(registrationId, providerId).orElse(null);
        if (linked != null) {
            // 현재 화면 인증 객체는 Google 이메일을 사용하므로 불일치는 자동 갱신하지 않는다.
            if (!linked.getEmail().equalsIgnoreCase(attributes.getEmail())) {
                throw new OAuth2AuthenticationException(new OAuth2Error("social_email_changed"),
                        "Google 계정 이메일이 변경되었습니다. 계정 연결 정보를 확인해 주세요.");
            }
            return linked;
        }
        if (memberRepository.existsByNormalizedEmail(EmailAddress.normalize(attributes.getEmail()))) {
            throw new OAuth2AuthenticationException(new OAuth2Error("account_link_required"),
                    "이미 가입된 이메일입니다. 기존 가입 방식으로 로그인해 주세요.");
        }
        Member member = attributes.toEntity();
        member.linkSocial(registrationId, providerId);
        return memberRepository.save(member);
    }
}
