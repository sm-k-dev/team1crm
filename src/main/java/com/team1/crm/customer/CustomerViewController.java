package com.team1.crm.customer;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.Store;
import com.team1.crm.auth.StoreService;
import com.team1.crm.auth.security.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 로그인한 회원의 소유 매장과 고객 목록을 화면에 전달하는 Controller이다.
 * 승인·정지 상태 정책 연동 전까지 개발 환경에서 사용한다.
 *
 * 작성자: 김순민
 * 작성일: 2026-11-06
 * 변경노트 2026-11-10 김순민: 고정 매장 조회를 로그인 회원의 소유 매장 조회로 변경
 */
@Controller
@Profile("dev")
@RequiredArgsConstructor
public class CustomerViewController {

    private final CustomerService customerService;
    private final StoreService storeService;

    /**
     * 로그인 회원의 매장 소유권을 확인한 후 고객을 소프트 삭제한다.
     *
     * @param member 로그인한 회원
     * @param customerId 삭제할 고객 ID
     * @param storeId 고객이 속한 매장 ID
     * @return 선택 매장의 고객 목록 주소
     * 작성자: 김순민
     * 작성일: 2026-11-11
     */
    @PostMapping("/customers/{customerId}/delete")
    public String delete(
            @LoginMember Member member,
            @PathVariable("customerId") Long customerId,
            @RequestParam("storeId") Long storeId
    ) {
        if (member == null) {
            return "redirect:/login";
        }
        Store store;
        try {
            store = storeService.findOwnedStore(member.getId(), storeId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "접근 가능한 매장이 아닙니다.");
        }
        try {
            customerService.deleteCustomer(store.getId(), customerId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 매장의 고객을 찾을 수 없습니다.");
        }
        return "redirect:/customers?storeId=" + store.getId();
    }

    /**
     * 로그인한 회원이 선택한 매장의 고객 목록과 등록 Form을 표시한다.
     * 매장을 지정하지 않으면 소유 매장 목록의 첫 번째 매장으로 이동한다.
     *
     * @param member 로그인한 회원
     * @param storeId 선택한 매장의 ID. 생략 가능
     * @param model 화면에 전달할 데이터
     * @param keyword 이름 또는 전화번호 뒤 4자리 검색어. 생략 가능
     * @param redirectAttributes 첫 매장으로 이동할 때 유지할 주소 매개변수
     * @param page 조회할 페이지 번호. 첫 페이지는 0
     * @return 고객 목록 화면 또는 첫 소유 매장의 고객 목록 주소
     *
     * 작성자: 김순민
     * 작성일: 2026-11-06
     * 변경노트 2026-11-09 김순민: 고객 등록용 Form 전달 추가
     * 변경노트 2026-11-10 김순민: 로그인 회원과 매장 선택·소유권 확인 연결
     * 변경노트 2026-11-10 김순민: 매장별 고객 검색 연결
     * 변경노트 2026-11-10 김순민: 매장별 이름·전화번호 뒤 4자리 검색 연결
     * 변경노트 2026-11-10 김순민: 고객 검색 결과의 페이지 조회 연결
     */
    @GetMapping("/customers")
    public String list(
            @LoginMember Member member,
            @RequestParam(name = "storeId", required = false) Long storeId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (member == null) {
            return "redirect:/login";
        }

        List<Store> allStores = storeService.getMyStores(member.getEmail());
        List<Store> stores = allStores.stream().filter(store -> !store.isClosed()).toList();

        if (stores.isEmpty()) {
            model.addAttribute("closedOnly", !allStores.isEmpty());
            model.addAttribute("closedStoreId", allStores.isEmpty() ? null : allStores.getFirst().getId());
            return "customer/no-store";
        }

        // 폐업 매장은 고객 업무 대신 과거 통계로 안내한다. 작성자: 김순민 / 작성일: 2026-11-11
        var closedStore = allStores.stream().filter(store -> store.isClosed() && store.getId().equals(storeId)).findFirst();
        if (closedStore.isPresent()) {
            model.addAttribute("closedOnly", true);
            model.addAttribute("closedStoreId", closedStore.get().getId());
            return "customer/no-store";
        }

        if (storeId == null) {
            redirectAttributes.addAttribute(
                    "storeId",
                    stores.get(0).getId()
            );

            if (keyword != null && !keyword.isBlank()) {
                redirectAttributes.addAttribute("keyword", keyword.strip());
            }

            redirectAttributes.addAttribute("page", Math.max(page, 0));

            return "redirect:/customers";
        }

        Store selectedStore;

        try {
            selectedStore = storeService.findOwnedStore(
                    member.getId(),
                    storeId
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "접근 가능한 매장이 아닙니다."
            );
        }

        model.addAttribute("stores", stores);
        model.addAttribute("selectedStore", selectedStore);
        Page<Customer> customerPage = customerService.findCustomerPage(
                selectedStore.getId(),
                keyword,
                page
        );

        // 실제 마지막 페이지를 넘어선 요청은 마지막 페이지로 이동한다.
        if (customerPage.getTotalPages() > 0
                && customerPage.getNumber() >= customerPage.getTotalPages()) {
            redirectAttributes.addAttribute("storeId", selectedStore.getId());
            redirectAttributes.addAttribute(
                    "keyword",
                    keyword == null ? "" : keyword.strip()
            );
            redirectAttributes.addAttribute(
                    "page",
                    customerPage.getTotalPages() - 1
            );

            return "redirect:/customers";
        }

        model.addAttribute("customerPage", customerPage);
        model.addAttribute("customers", customerPage.getContent());
        model.addAttribute(
                "keyword",
                keyword == null ? "" : keyword.strip()
        );
        model.addAttribute("customerForm", new CustomerForm());

        return "customer/list";
    }

    /**
     * 로그인한 회원의 소유 매장에 고객을 등록한다.
     * 입력 오류가 있으면 입력값과 오류를 유지하여 목록 화면을 다시 표시한다.
     *
     * @param member 로그인한 회원
     * @param storeId 고객을 등록할 매장의 ID
     * @param form 고객 등록 입력값
     * @param bindingResult 입력값 변환 및 검증 결과
     * @param model 화면에 전달할 데이터
     * @return 오류 시 고객 목록 화면, 성공 시 선택 매장의 고객 목록 주소
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @PostMapping("/customers")
    public String register(
            @LoginMember Member member,
            @RequestParam(name = "storeId") Long storeId,
            @Valid @ModelAttribute("customerForm") CustomerForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (member == null) {
            return "redirect:/login";
        }

        Store selectedStore;

        try {
            selectedStore = storeService.findOwnedStore(
                    member.getId(),
                    storeId
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "접근 가능한 매장이 아닙니다."
            );
        }

        if (!bindingResult.hasErrors()) {
            try {
                customerService.registerCustomer(selectedStore, form);

                return "redirect:/customers?storeId="
                        + selectedStore.getId();
            } catch (IllegalArgumentException exception) {
                bindingResult.rejectValue(
                        "phone",
                        "customer.phone.invalid",
                        exception.getMessage()
                );
            }
        }

        model.addAttribute(
                "stores",
                storeService.getMyStores(member.getEmail())
        );
        model.addAttribute("selectedStore", selectedStore);
        Page<Customer> customerPage = customerService.findCustomerPage(
                selectedStore.getId(),
                "",
                0
        );

        model.addAttribute("customerPage", customerPage);
        model.addAttribute("customers", customerPage.getContent());
        model.addAttribute("keyword", "");
        model.addAttribute("showRegistration", true);

        return "customer/list";
    }

    /**
     * 로그인한 회원의 소유 매장에 속한 고객 상세 정보를 표시한다.
     *
     * @param member 로그인한 회원
     * @param customerId 조회할 고객의 ID
     * @param storeId 고객이 속한 매장의 ID
     * @param model 화면에 전달할 데이터
     * @return 고객 상세 화면
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @GetMapping("/customers/{customerId}")
    public String detail(
            @LoginMember Member member,
            @PathVariable(name = "customerId") Long customerId,
            @RequestParam(name = "storeId") Long storeId,
            Model model
    ) {
        if (member == null) {
            return "redirect:/login";
        }

        Store selectedStore;

        try {
            selectedStore = storeService.findOwnedStore(
                    member.getId(),
                    storeId
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "접근 가능한 매장이 아닙니다."
            );
        }

        Customer customer;

        try {
            customer = customerService.findCustomer(
                    selectedStore.getId(),
                    customerId
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "해당 매장의 고객을 찾을 수 없습니다."
            );
        }

        model.addAttribute("selectedStore", selectedStore);
        model.addAttribute("customer", customer);

        return "customer/detail";
    }

    /**
     * 로그인한 회원의 소유 매장에 속한 고객 정보를 수정 Form에 채워 표시한다.
     *
     * @param member 로그인한 회원
     * @param customerId 수정할 고객의 ID
     * @param storeId 고객이 속한 매장의 ID
     * @param model 화면에 전달할 데이터
     * @return 고객 수정 화면
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @GetMapping("/customers/{customerId}/edit")
    public String editForm(
            @LoginMember Member member,
            @PathVariable(name = "customerId") Long customerId,
            @RequestParam(name = "storeId") Long storeId,
            Model model
    ) {
        if (member == null) {
            return "redirect:/login";
        }

        Store selectedStore;

        try {
            selectedStore = storeService.findOwnedStore(
                    member.getId(),
                    storeId
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "접근 가능한 매장이 아닙니다."
            );
        }

        Customer customer;

        try {
            customer = customerService.findCustomer(
                    selectedStore.getId(),
                    customerId
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "해당 매장의 고객을 찾을 수 없습니다."
            );
        }

        CustomerForm form = new CustomerForm();
        form.setName(customer.getName());
        form.setPhone(customer.getPhone());
        form.setBirthMonth(customer.getBirthMonth());
        form.setMemo(customer.getMemo());
        form.setAgreeMarketing(customer.isAgreeMarketing());

        model.addAttribute("selectedStore", selectedStore);
        model.addAttribute("customerId", customer.getId());
        model.addAttribute("customerForm", form);

        return "customer/edit";
    }

    /**
     * 로그인한 회원의 소유 매장에 속한 고객 정보를 수정한다.
     * 오류가 있으면 입력값과 오류 메시지를 유지하여 수정 화면을 다시 표시한다.
     *
     * @param member 로그인한 회원
     * @param customerId 수정할 고객의 ID
     * @param storeId 고객이 속한 매장의 ID
     * @param form 고객 수정 입력값
     * @param bindingResult 입력값 변환 및 검증 결과
     * @param model 화면에 전달할 데이터
     * @return 오류 시 수정 화면, 성공 시 고객 상세 주소
     *
     * 작성자: 김순민
     * 작성일: 2026-11-10
     */
    @PostMapping("/customers/{customerId}/edit")
    public String update(
            @LoginMember Member member,
            @PathVariable(name = "customerId") Long customerId,
            @RequestParam(name = "storeId") Long storeId,
            @Valid @ModelAttribute("customerForm") CustomerForm form,
            BindingResult bindingResult,
            Model model
    ) {
        if (member == null) {
            return "redirect:/login";
        }

        Store selectedStore;

        try {
            selectedStore = storeService.findOwnedStore(
                    member.getId(),
                    storeId
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "접근 가능한 매장이 아닙니다."
            );
        }

        try {
            customerService.findCustomer(
                    selectedStore.getId(),
                    customerId
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "해당 매장의 고객을 찾을 수 없습니다."
            );
        }

        if (!bindingResult.hasErrors()) {
            try {
                customerService.updateCustomer(
                        selectedStore.getId(),
                        customerId,
                        form
                );

                return "redirect:/customers/"
                        + customerId
                        + "?storeId="
                        + selectedStore.getId();
            } catch (IllegalArgumentException exception) {
                bindingResult.rejectValue(
                        "phone",
                        "customer.phone.invalid",
                        exception.getMessage()
                );
            }
        }

        model.addAttribute("selectedStore", selectedStore);
        model.addAttribute("customerId", customerId);

        return "customer/edit";
    }
}
