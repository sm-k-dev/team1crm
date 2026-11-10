
package com.team1.crm.home;

import com.team1.crm.auth.Store;
import com.team1.crm.auth.StoreService;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@Profile("dev")
@RequiredArgsConstructor
public class HomeController {

    private final StoreService storeService;

    @GetMapping({"/", "/index.html"})
    public String home(
            Authentication authentication,
            Model model
    ) {

        // 1. 로그인하지 않은 회원
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {

            return "home/index";
        }

        // 2. 로그인한 회원의 이메일 가져오기
        String email;

        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {

            // 구글 로그인
            email = oauth2User.getAttribute("email");

        } else {

            // 일반 로그인
            email = authentication.getName();
        }

        // 3. 이메일이 없는 경우
        if (email == null || email.isBlank()) {
            return "home/index";
        }

        // 4. 로그인한 회원의 매장 목록 조회
        List<Store> stores = storeService.getMyStores(email);

        // 5. Thymeleaf로 매장 목록 전달
        model.addAttribute("stores", stores);

        // 6. 첫 번째 매장을 기본 선택
        if (!stores.isEmpty()) {
            model.addAttribute("selectedStore", stores.get(0));
        }

        // 7. 홈 화면 출력
        return "home/index";
    }
}
