package com.team1.crm.auth;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * + 비밀번호 찾기 및 재설정 MVC 컨트롤러
 * 역할: 비로그인 사용자의 비밀번호 찾기 화면 요청,
 *       재설정 메일 요청, 토큰 유효성 조회,
 *       새 비밀번호 등록 요청 및 로그인 화면 이동을 처리합니다.
 *       실제 토큰 발급과 변경은 PasswordResetService에 위임합니다.
 * 작성자: 황현우
 * 일시: 2026-11-16
 */
@Controller
@RequiredArgsConstructor
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    /** GET /forgot-password : 이메일 입력 화면 표시 */
    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "auth/forgot-password";
    }

    /** POST /forgot-password : 계정 존재 여부를 노출하지 않고 발송 요청 */
    @PostMapping("/forgot-password")
    public String requestReset(@RequestParam String email, RedirectAttributes redirectAttributes) {
        passwordResetService.requestReset(email);
        redirectAttributes.addFlashAttribute("message",
                "비밀번호 재설정 안내 메일이 발송되었습니다. 확인 부탁드립니다.");
        return "redirect:/forgot-password";
    }

    /** GET /reset-password : 링크 유효성 검사 후 새 비밀번호 화면 표시 */
    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(required = false) String token,
                                    Model model, HttpServletResponse response) {
        // URL의 재설정 토큰이 브라우저 캐시나 외부 사이트의 Referer에 남지 않도록 한다.
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");
        model.addAttribute("token", token);
        model.addAttribute("valid", passwordResetService.isValidToken(token));
        return "auth/reset-password";
    }

    /** POST /reset-password : 비밀번호 변경 후 로그인 화면으로 이동 */
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                Model model) {
        try {
            passwordResetService.resetPassword(token, newPassword, confirmPassword);
            return "redirect:/login?resetSuccess";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("token", token);
            model.addAttribute("valid", passwordResetService.isValidToken(token));
            model.addAttribute("error", ex.getMessage());
            return "auth/reset-password";
        }
    }
}
