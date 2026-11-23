
package com.team1.crm.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * + 닉네임 중복확인 API 컨트롤러
 * 역할: 회원가입 화면의 중복확인 버튼에서 호출한
 *       GET /api/members/check-nickname 요청을 처리합니다.
 *       입력 길이·앞뒤 공백을 검사하고 기존 MemberRepository로
 *       닉네임 존재 여부를 조회해 JSON 결과를 반환합니다.
 *       김순민님이 수정한 회원가입 로직은 변경하지 않습니다.
 * 작성자: 황현우
 * 일시: 2026-11-16
 */
@RestController
@RequiredArgsConstructor
public class NicknameCheckController {

    private final MemberRepository memberRepository;

    /**
     * 입력한 닉네임의 중복 여부를 반환합니다.
     * @param nickname 회원가입 화면에서 입력한 닉네임
     * @return available과 message를 포함한 JSON 응답
     */
    @GetMapping("/api/members/check-nickname")
    public ResponseEntity<?> checkNickname(
            @RequestParam String nickname) {

        if (nickname.length() < 2 ||
                nickname.length() > 10 ||
                !nickname.equals(nickname.trim())) {
            return ResponseEntity.badRequest().body(
                    Map.of("available", false,
                            "message", "닉네임은 공백 없이 2~10자로 입력해 주세요.")
            );
        }

        boolean exists =
                memberRepository.existsByNickname(nickname);

        return ResponseEntity.ok(
                Map.of(
                        "available", !exists,
                        "message", exists
                                ? "이미 사용 중인 닉네임입니다."
                                : "사용 가능한 닉네임입니다."
                )
        );
    }
}
