package com.team1.crm.auth.security;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Service;

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
     * 구글 사용자 정보가 이미 DB에 있으면 유지, 없으면 새로 회원가입 처리
     */
    private Member saveOrUpdate(OAuthAttributes attributes, String registrationId) {
        Member member = memberRepository.findByEmail(attributes.getEmail())
                .map(entity -> entity)
                .orElseGet(() -> {
                    Member newMember = attributes.toEntity();
                    newMember.linkSocial(registrationId, attributes.getAttributes().get(attributes.getNameAttributeKey()).toString());
                    return memberRepository.save(newMember);
                });

        return member;
    }
}