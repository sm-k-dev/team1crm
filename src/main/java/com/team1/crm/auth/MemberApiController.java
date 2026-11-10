
package com.team1.crm.auth;

import com.team1.crm.auth.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 회원 관련 API 컨트롤러
 * 역할: 회원가입 및 로그인 회원 정보 조회
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    /**
     * 회원가입 API
     * POST /api/members/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<String> signup(
            @RequestBody MemberSignupForm form
    ) {
        memberService.signup(form);

        return ResponseEntity.ok(
                "회원가입이 성공적으로 완료되었습니다."
        );
    }

    /**
     * 현재 로그인한 회원 정보 조회
     * GET /api/members/me
     *
     * 비밀번호 등 민감한 정보는 반환하지 않음
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentMember(
            @LoginMember Member member
    ) {
        if (member == null) {
            return ResponseEntity.status(401).build();
        }
// 어떤 회원 정보가 null인지 확인
/*        System.out.println("===== 회원 정보 확인 =====");
        System.out.println("회원 ID: " + member.getId());
        System.out.println("회원 이메일: " + member.getEmail());*/

        // null 값이 있으면 500 오류 대신 명확하게 처리
        if (member.getId() == null || member.getEmail() == null) {
            return ResponseEntity.internalServerError()
                    .body("로그인 회원 정보가 올바르지 않습니다.");
        }

        return ResponseEntity.ok(Map.of(
                "id", member.getId(),
                "email", member.getEmail()
        ));
    }
}