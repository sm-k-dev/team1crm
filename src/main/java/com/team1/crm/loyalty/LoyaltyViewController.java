package com.team1.crm.loyalty;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.Store;
import com.team1.crm.auth.StoreService;
import com.team1.crm.auth.security.LoginMember;
import com.team1.crm.loyalty.coupon.CouponForm;
import com.team1.crm.loyalty.coupon.CouponService;
import com.team1.crm.loyalty.visit.VisitForm;
import com.team1.crm.loyalty.visit.VisitResult;
import com.team1.crm.loyalty.visit.VisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 고객 상세 화면의 방문 등록·쿠폰 발급 폼을 받는 화면용 컨트롤러.
 * 쿠폰 사용과 포인트 사용은 별도 경로 없이 방문 등록 폼 안에서 함께 처리한다.
 * (JSON API는 VisitApiController, CouponApiController가 따로 담당한다.)
 *
 * 처리 후에는 항상 고객 상세 화면으로 redirect 하고, 결과 메시지는 flash 값으로 전달한다.
 * - 성공: visitMessage / couponMessage
 * - 실패: visitError / couponError
 *
 * 작성자: 이재준
 */
@Controller
@Profile("dev") // 고객 화면(CustomerViewController)과 같은 조건에서만 켠다
@RequiredArgsConstructor
public class LoyaltyViewController {

    private final StoreService storeService;
    private final VisitService visitService;
    private final CouponService couponService;

    @PostMapping("/customers/{customerId}/visits")
    public String registerVisit(
            @LoginMember Member member,
            @PathVariable("customerId") Long customerId,
            @RequestParam("storeId") Long storeId,
            @Valid @ModelAttribute VisitForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (member == null) {
            return "redirect:/login";
        }
        Store store = findOwnedStoreOrForbidden(member, storeId);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("visitError", "입력값을 확인해 주세요. 결제 금액은 0원 이상 1억 원 이하, 사용 포인트는 0 이상, 메모는 200자 이하입니다.");
            return redirectToDetail(customerId, store.getId(), redirectAttributes);
        }

        try {
            VisitResult result = visitService.register(store.getId(), customerId, form);
            StringBuilder message = new StringBuilder("방문이 등록되었습니다. 결제 금액 ")
                    .append(String.format("%,d", result.paidAmount())).append("원");
            if (result.discountAmount() > 0) {
                message.append(" (쿠폰 할인 ").append(String.format("%,d", result.discountAmount())).append("원");
                message.append(result.usedPoints() > 0 ? ", " : ")");
            }
            if (result.usedPoints() > 0) {
                message.append(result.discountAmount() > 0 ? "" : " (")
                        .append("포인트 ").append(String.format("%,d", result.usedPoints())).append("P 사용)");
            }
            message.append(", 적립 포인트 ").append(result.earnedPoints()).append("P");
            redirectAttributes.addFlashAttribute("visitMessage", message.toString());
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("visitError", exception.getMessage());
        } catch (ResponseStatusException exception) {
            // 폐업 매장(409) 등: 오류 페이지 대신 화면 안내로 보여 준다
            redirectAttributes.addFlashAttribute("visitError", exception.getReason());
        }
        return redirectToDetail(customerId, store.getId(), redirectAttributes);
    }

    @PostMapping("/customers/{customerId}/coupons")
    public String issueCoupon(
            @LoginMember Member member,
            @PathVariable("customerId") Long customerId,
            @RequestParam("storeId") Long storeId,
            @Valid @ModelAttribute CouponForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (member == null) {
            return "redirect:/login";
        }
        Store store = findOwnedStoreOrForbidden(member, storeId);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("couponError", "입력값을 확인해 주세요. 쿠폰 이름은 50자 이하, 할인 금액은 1원 이상, 만료일은 필수입니다.");
            return redirectToDetail(customerId, store.getId(), redirectAttributes);
        }

        try {
            couponService.issue(store.getId(), customerId, form);
            redirectAttributes.addFlashAttribute("couponMessage", "쿠폰이 발급되었습니다.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("couponError", exception.getMessage());
        } catch (ResponseStatusException exception) {
            redirectAttributes.addFlashAttribute("couponError", exception.getReason());
        }
        return redirectToDetail(customerId, store.getId(), redirectAttributes);
    }

    /** 로그인 회원 소유 매장이 아니면 403. (customer 화면 컨트롤러와 같은 처리) */
    private Store findOwnedStoreOrForbidden(Member member, Long storeId) {
        try {
            return storeService.findOwnedStore(member.getId(), storeId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 가능한 매장이 아닙니다.");
        }
    }

    private String redirectToDetail(Long customerId, Long storeId, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("storeId", storeId);
        return "redirect:/customers/" + customerId;
    }
}
