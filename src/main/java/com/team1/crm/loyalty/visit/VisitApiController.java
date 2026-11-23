package com.team1.crm.loyalty.visit;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.Store;
import com.team1.crm.auth.StoreService;
import com.team1.crm.auth.security.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 방문 등록 API. 명세: POST /api/customers/{id}/visits
 * storeId는 클라이언트 값을 그대로 믿지 않고, 로그인 회원이 그 매장의 소유자인지 먼저 확인한다.
 *
 * 작성자: 이재준
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers/{customerId}/visits")
public class VisitApiController {

    private final VisitService visitService;
    private final StoreService storeService;

    @PostMapping
    public ResponseEntity<VisitResult> register(
            @LoginMember Member member,
            @PathVariable("customerId") Long customerId,
            @RequestParam("storeId") Long storeId,
            @Valid @RequestBody VisitForm form) {

        // 로그인 회원이 이 매장의 소유자인지 확인 (아니면 예외)
        Store store = storeService.findOwnedStore(member.getId(), storeId);

        VisitResult result = visitService.register(store.getId(), customerId, form);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
