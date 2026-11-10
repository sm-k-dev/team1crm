
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stores")
public class StoreApiController {

    private final StoreService storeService;

    // 1. 새로운 매장 등록
    @PostMapping
    public ResponseEntity<?> registerStore(
            @RequestBody StoreForm form,
            Authentication authentication
    ) {

        // 로그인 여부 확인
        if (!isLoggedIn(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        try {
            // 로그인 회원의 이메일 가져오기
            String email = getLoginEmail(authentication);

            // 매장 저장
            Store store = storeService.registerStore(
                    email,
                    form.getName(),
                    form.getCategory(),
                    form.getAddress(),
                    form.getPointRate()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "매장이 등록되었습니다.",
                            "storeId", store.getId()
                    ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // 2. 내 매장 목록 조회
    @GetMapping
    public ResponseEntity<?> getMyStores(
            Authentication authentication
    ) {

        // 로그인 여부 확인
        if (!isLoggedIn(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다."));
        }

        try {
            String email = getLoginEmail(authentication);

            List<Store> stores = storeService.getMyStores(email);

            List<Map<String, Object>> result = stores.stream()
                    .map(store -> {
                        Map<String, Object> item = new HashMap<>();

                        item.put("id", store.getId());
                        item.put("name", store.getName());
                        item.put("category", store.getCategory());
                        item.put("address", store.getAddress());
                        item.put("pointRate", store.getPointRate());

                        return item;
                    })
                    .toList();

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // 3. 로그인 여부 확인
    private boolean isLoggedIn(Authentication authentication) {

        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(
                authentication.getPrincipal()
        );
    }

    // 4. 로그인 회원의 이메일 가져오기
    private String getLoginEmail(Authentication authentication) {

        Object principal = authentication.getPrincipal();

        // 구글 로그인 회원
        if (principal instanceof OAuth2User oauth2User) {

            String email = oauth2User.getAttribute("email");

            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException(
                        "구글 계정의 이메일을 확인할 수 없습니다."
                );
            }

            return email;
        }

        // 일반 로그인 회원
        return authentication.getName();
    }
}
