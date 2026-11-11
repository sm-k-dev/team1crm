
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

/**
 * + CRM 홈페이지 화면 컨트롤러
 * 역할: 사용자의 로그인 상태를 확인하고,
 *      로그인한 회원이 소유한 매장 목록을 조회하여
 *      CRM 메인 화면에 전달합니다.
 *      매장이 존재하면 첫 번째 매장을 기본 매장으로 선택합니다.
 * 작성자: 황현우
 * 일시: 2026-11-10
 */

@Controller // HTML 화면을 반환하는 Spring MVC 컨트롤러
@Profile("dev") // dev 프로필이 활성화된 환경에서만 Bean 등록
@RequiredArgsConstructor // final 필드를 이용한 생성자 자동 생성
public class HomeController {

    // 로그인한 회원의 매장 목록을 조회하는 서비스
    private final StoreService storeService;
    private final HomeSummaryService homeSummaryService;

    /**
     * + CRM 홈페이지 출력
     * 역할: 홈페이지 접속 시 로그인 상태를 확인하고,
     *      로그인한 회원의 이메일을 기준으로 매장 목록을 조회합니다.
     *      조회한 매장 목록과 기본 선택 매장을 Model에 저장한 뒤
     *      CRM 메인 화면을 반환합니다.
     *
     * @param authentication 현재 로그인한 회원의 인증 정보
     * @param model HTML 화면에 데이터를 전달하는 객체
     * @return CRM 메인 화면 경로
     */
    @GetMapping({"/", "/index.html"})
    public String home(
            Authentication authentication,
            @org.springframework.web.bind.annotation.RequestParam(name = "storeId", required = false) Long storeId,
            Model model
    ) {

        // 1. 로그인하지 않은 회원
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {

            return "home/index";
        }

        // 관리자 이동 및 선택 매장 집계 연결. 작성자: 김순민 / 작성일: 2026-11-11
        if (authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            return "redirect:/admin/dashboard";
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

        // 요청한 매장은 소유 목록에서만 허용한다. 작성자: 김순민 / 작성일: 2026-11-11
        if (storeId != null && stores.stream().noneMatch(store -> store.getId().equals(storeId))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "접근 가능한 매장이 아닙니다.");
        }
        // 6. 지정한 소유 매장을 선택하고 생략하면 첫 매장을 선택한다.
        if (!stores.isEmpty()) {
            Store selected = storeId == null ? stores.get(0) : stores.stream()
                    .filter(store -> store.getId().equals(storeId)).findFirst().orElseThrow();
            model.addAttribute("selectedStore", selected);
            model.addAttribute("summary", homeSummaryService.summarize(selected.getId(),
                    java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"))));
        }

        // 7. 홈 화면 출력
        return "home/index";
    }
}
