
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
     * + 이메일 중복확인 API
     * 역할: 회원가입 전에 이메일 사용 가능 여부 확인
     * GET /api/members/check-email?email=example@test.com
     * 작성자: 황현우
     * 일시: 2026-11-11
     */
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(
            @RequestParam String email
    ) {
        // 1. 이메일 입력값 검사
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "이메일을 입력해주세요.")
            );
        }

        // 2. 이메일 중복 여부 확인
        boolean exists = memberService.isEmailExists(email);

        // 3. 중복된 이메일인 경우
        if (exists) {
            return ResponseEntity.ok(
                    Map.of(
                            "available", false,
                            "message", "이미 가입된 이메일입니다."
                    )
            );
        }

        // 4. 사용 가능한 이메일인 경우
        return ResponseEntity.ok(
                Map.of(
                        "available", true,
                        "message", "사용 가능한 이메일입니다."
                )
        );
    }


    /**
     * + 회원가입 API
     * 역할: 신규 회원가입 및 이메일·닉네임 중복 오류 처리
     * POST /api/members/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @jakarta.validation.Valid @RequestBody MemberSignupForm form
    ) {
        try {
            // 1. 회원가입 서비스 실행
            memberService.signup(form);

            // 2. 회원가입 성공 시 HTTP 200 반환
            return ResponseEntity.ok(
                    Map.of("message", "회원가입이 성공적으로 완료되었습니다.")
            );

        } catch (IllegalArgumentException e) {

            // 3. 이메일 또는 닉네임 중복 시 HTTP 409 반환
            return ResponseEntity.status(409).body(
                    Map.of("message", e.getMessage())
            );
        }
    }

    /** 가입 중복은 성공 응답 대신 화면에서 읽을 수 있는 JSON으로 반환한다. 작성자: 김순민 / 작성일: 2026-11-13 */
    @ExceptionHandler(DuplicateMemberEmailException.class)
    public ResponseEntity<?> duplicateEmail(DuplicateMemberEmailException exception) {
        return ResponseEntity.status(409).body(Map.of("message", exception.getMessage()));
    }

    /** 동시 가입의 DB 유니크 충돌도 성공으로 표시하지 않는다. 작성자: 김순민 / 작성일: 2026-11-13 */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<?> signupConflict(org.springframework.dao.DataIntegrityViolationException exception) {
        return ResponseEntity.status(409).body(Map.of("message", "이미 사용 중인 가입 정보입니다. 이메일과 이름을 확인해 주세요."));
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
