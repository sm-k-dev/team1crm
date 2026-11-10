
package com.team1.crm.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class StoreSettingsViewController {

    private final StoreService storeService;

    // 내 매장 정보 페이지
    @GetMapping("/store-settings")
    public String storeSettings(
            Authentication authentication,
            Model model
    ) {

        // 1. 로그인 여부 확인
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {

            return "redirect:/login";
        }

        // 2. 로그인 회원의 이메일 확인
        String email;

        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {

            // 구글 로그인
            email = oauth2User.getAttribute("email");

        } else {

            // 일반 로그인
            email = authentication.getName();
        }

        // 3. 이메일 확인
        if (email == null || email.isBlank()) {
            return "redirect:/login";
        }

        // 4. 로그인 회원의 매장 목록 조회
        model.addAttribute(
                "stores",
                storeService.getMyStores(email)
        );

        // 5. 내 매장 정보 페이지 출력
        return "auth/store-settings";
    }
}
