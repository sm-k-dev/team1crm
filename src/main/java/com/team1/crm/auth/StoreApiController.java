
package com.team1.crm.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * + 매장 등록 및 조회 REST API 컨트롤러
 * 역할: 로그인한 회원의 신규 매장 등록과 매장 목록 조회를 처리합니다.
 *      일반 로그인 및 Google OAuth2 로그인 사용자의 인증 정보를 확인하고,
 *      StoreService를 호출하여 매장 관련 기능을 수행합니다.
 * 작성자: 황현우
 * 일시: 2026-11-09
 */
@RestController // HTTP 요청을 처리하고 결과를 JSON으로 반환
@RequiredArgsConstructor  // final 필드의 생성자를 자동 생성
@RequestMapping("/api/stores") // 매장 관련 API의 공통 주소
public class StoreApiController {

    // 매장 등록 및 조회 기능을 처리하는 서비스
    private final StoreService storeService;

    /**
     * + 신규 매장 등록
     * 역할: 로그인 여부를 확인하고 회원의 이메일과 매장 정보를 전달하여
     *      새로운 매장을 등록한 뒤 등록 결과를 반환합니다.
     */
    @PostMapping // POST /api/stores
    public ResponseEntity<?> registerStore(
            @RequestBody StoreForm form, // JSON으로 전달된 매장 등록 정보
            Authentication authentication  // 현재 로그인한 회원의 인증 정보
    ) {

        // 1. 로그인 여부 확인
        if (!isLoggedIn(authentication)) {
            // 로그인하지 않은 경우 401 Unauthorized 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        try {
            // 2. 현재 로그인한 회원의 이메일 가져오기
            String email = getLoginEmail(authentication);

            // 3. 회원 이메일과 매장 정보를 전달하여 DB에 매장 등록
            Store store = storeService.registerStore(
                    email,
                    form.getName(),
                    form.getCategory(),
                    form.getAddress(),
                    form.getPointRate()
            );

            // 4. 등록 성공 시 201 Created와 매장 ID 반환
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "매장이 등록되었습니다.",
                            "storeId", store.getId()
                    ));

        } catch (IllegalArgumentException e) {
            // 매장 등록 과정에서 잘못된 입력값 등이 발생한 경우
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }
    /**
     * + 내 매장 목록 조회
     * 역할: 로그인한 회원의 이메일을 기준으로 등록된 매장 목록을 조회하고,
     *      필요한 매장 정보를 JSON 형식으로 반환합니다.
     */
    @GetMapping // GET /api/stores
    public ResponseEntity<?> getMyStores(
            Authentication authentication
    ) {

        // 1. 로그인 여부 확인
        if (!isLoggedIn(authentication)) {
            // 로그인하지 않은 경우 401 Unauthorized 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        try {
            // 2. 현재 로그인한 회원의 이메일 가져오기
            String email = getLoginEmail(authentication);
            // 3. 해당 회원이 등록한 매장 목록 조회
            List<Store> stores = storeService.getMyStores(email);
            // 4. 매장 목록에서 필요한 정보만 추출하여 JSON 응답 데이터 구성
            List<Map<String, Object>> result = stores.stream()
                    .map(store -> {
                        Map<String, Object> item = new HashMap<>();

                        item.put("id", store.getId()); // 매장 고유 번호
                        item.put("name", store.getName()); // 매장 이름
                        item.put("category", store.getCategory()); // 업종
                        item.put("address", store.getAddress()); // 매장 주소
                        item.put("status", store.getStatus().name());
                        item.put("closedAt", store.getClosedAt());
                        item.put("pointRate", store.getPointRate()); // 포인트 적립률

                        return item;
                    })
                    .toList();

            // 5. 조회 성공 시 200 OK와 매장 목록 반환
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            // 매장 조회 과정에서 잘못된 요청 등이 발생한 경우
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * + 회원 로그인 여부 확인
     * 역할: 인증 정보가 존재하고 인증된 사용자이며,
     *      익명 사용자가 아닌지 확인합니다.
     */
    private boolean isLoggedIn(Authentication authentication) {
        // 인증 객체가 존재하고 인증 상태이며 익명 사용자가 아닌 경우 true
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(
                authentication.getPrincipal()
        );
    }

    /**
     * + 로그인 회원 이메일 조회
     * 역할: Google OAuth2 로그인과 일반 로그인을 구분하여
     *      현재 로그인한 회원의 이메일을 반환합니다.
     */
    private String getLoginEmail(Authentication authentication) {
        // 현재 로그인한 회원의 사용자 정보 가져오기
        Object principal = authentication.getPrincipal();

        // 1. Google OAuth2 로그인 회원인 경우
        if (principal instanceof OAuth2User oauth2User) {

            // Google 계정에서 이메일 정보 추출
            String email = oauth2User.getAttribute("email");
            // 이메일 정보가 없으면 예외 발생
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException(
                        "구글 계정의 이메일을 확인할 수 없습니다."
                );
            }
            // Google 계정 이메일 반환
            return email;
        }

        // 2. 일반 로그인 회원인 경우 인증 객체의 사용자 이름 반환
        return authentication.getName();
    }
}
