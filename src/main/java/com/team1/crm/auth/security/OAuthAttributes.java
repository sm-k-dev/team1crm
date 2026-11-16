package com.team1.crm.auth.security;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.EmailAddress;
import com.team1.crm.auth.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 소셜 로그인(OAuth2) 사용자 정보 공통 DTO 클래스
 * 역할: 소셜 로그인 제공자(Google 등)로부터 받은 응답 데이터를 파싱하여 Member 엔티티로 변환
 * 작성자: 황현우
 * 일시: 2026-11-09
 */
@Getter
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private String picture;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String name, String email, String picture) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.picture = picture;
    }

    // 소셜 로그인 API 종류에 따라 분기 처리 (현재는 구글 중심)
    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .picture((String) attributes.get("picture"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    /**
     * 처음 가입할 때 회원 엔티티 생성
     */
    public Member toEntity() {
        return Member.builder()
                .email(EmailAddress.normalize(email))
                .nickname(name)
                .password("") // 소셜 로그인 사용자는 비밀번호가 없으므로 빈 값 또는 임의 처리
                .role(Role.USER) // 기본 권한 설정 (프로젝트 정책에 맞게 조절 가능)
                .build();
    }
}
