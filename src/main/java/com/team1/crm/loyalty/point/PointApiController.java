package com.team1.crm.loyalty.point;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.Store;
import com.team1.crm.auth.StoreService;
import com.team1.crm.auth.security.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 포인트 사용 API. 명세: POST /api/customers/{id}/points/use
 *
 * 작성자: 이재준
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customers/{customerId}/points")
public class PointApiController {

    private final PointService pointService;
    private final StoreService storeService;

    @PostMapping("/use")
    public PointUseResult use(
            @LoginMember Member member,
            @PathVariable("customerId") Long customerId,
            @RequestParam("storeId") Long storeId,
            @Valid @RequestBody PointUseForm form) {

        Store store = storeService.findOwnedStore(member.getId(), storeId);
        return pointService.use(store.getId(), customerId, form.getPoints());
    }
}
