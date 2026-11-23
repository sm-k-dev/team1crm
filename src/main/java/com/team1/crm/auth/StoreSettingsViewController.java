
package com.team1.crm.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * + 내 매장 정보 화면 컨트롤러
 * 역할: 로그인한 회원의 인증 정보를 확인하고,
 *      해당 회원이 소유한 매장 목록을 조회하여
 *      내 매장 정보 페이지에 전달합니다.
 *      일반 로그인과 Google OAuth2 로그인을 구분하여 처리합니다.
 * 작성자: 황현우
 * 일시: 2026-11-09
 */

@Controller // HTML 화면을 반환하는 Spring MVC 컨트롤러
@RequiredArgsConstructor  // final 필드를 이용한 생성자 자동 생성
public class StoreSettingsViewController {

    // 회원이 소유한 매장 목록을 조회하는 서비스
    private final StoreService storeService;

    /**
     * + 내 매장 정보 페이지 이동
     * 역할: 로그인 여부를 확인하고 회원의 이메일을 가져온 뒤,
     *      해당 회원이 소유한 매장 목록을 조회하여 화면에 전달합니다.
     *      로그인하지 않은 사용자는 로그인 페이지로 이동시킵니다.
     *
     * @param authentication 현재 로그인한 회원의 인증 정보
     * @param model 조회한 매장 목록을 HTML 화면에 전달하는 객체
     * @return 내 매장 정보 페이지 또는 로그인 페이지 이동 경로
     */
    @GetMapping("/store-settings")  // GET /store-settings
    public String storeSettings(
            Authentication authentication, // 현재 사용자 인증 정보
            Model model // 컨트롤러에서 화면으로 데이터를 전달하는 객체
    ) {
        // 1. 로그인 여부 확인
        // 인증 정보가 없거나 인증되지 않은 사용자 또는 익명 사용자인지 검사
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {

            // 로그인하지 않은 경우 로그인 페이지로 이동
            return "redirect:/login";
        }

        // 2. 로그인 회원의 이메일 확인
        String email;
        // Google OAuth2 로그인 회원인지 확인
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {

            // 구글 로그인 회원의 이메일 정보 가져오기
            email = oauth2User.getAttribute("email");

        } else {

            // 일반 로그인 회원의 인증 정보에서 사용자 이름 가져오기
            // 현재 인증 구조에서는 이메일을 반환한다는 전제로 사용
            email = authentication.getName();
        }

        // 3. 이메일 정보가 존재하는지 확인
        if (email == null || email.isBlank()) {
            // 이메일을 확인할 수 없으면 로그인 페이지로 이동
            return "redirect:/login";
        }

        // 4. 로그인 회원의 매장 목록 조회
        // StoreService를 통해 조회한 매장 목록을
        // "stores"라는 이름으로 Model에 저장
        model.addAttribute(
                "stores",
                // 영업 중 우선, 각 상태 안에서는 매장 ID 오름차순. 변경: 김순민 / 2026-11-12
                storeService.getMyStores(email).stream()
                        .sorted(java.util.Comparator.comparing(Store::isClosed).thenComparing(Store::getId))
                        .toList()
        );

        // 5. 내 매장 정보 페이지 출력
        return "auth/store-settings";
    }
}
