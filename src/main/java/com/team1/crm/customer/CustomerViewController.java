package com.team1.crm.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 개발 환경에서 고객 목록을 조회하고 고객 관리 화면에 전달하는 Controller이다.
 * 인증 연동 전까지 개발용 고정 매장의 고객을 표시한다.
 *
 * 작성자: 김순민
 * 작성일: 2026-11-06
 */
@Controller
@Profile("dev")
@RequiredArgsConstructor
public class CustomerViewController {

    // 인증 연동 전 화면 연결 확인에 사용하는 개발용 매장 ID
    private static final Long PREVIEW_STORE_ID = 900101L;

    private final CustomerService customerService;

    /**
     * 개발용 매장의 고객 목록과 빈 고객 등록 Form을 화면에 전달한다.
     * 현재 로그인 사용자에 대한 매장 소유권 검증은 수행하지 않는다.
     *
     * @param model 화면에 전달할 데이터를 담는 객체
     * @return 고객 목록 Thymeleaf 템플릿 이름
     *
     * 작성자: 김순민
     * 작성일: 2026-11-06
     * 변경노트 2026-11-09 김순민: 고객 등록용 Form 전달 추가
     */
    @GetMapping("/customers")
    public String list(Model model) {
        model.addAttribute(
                "customers",
                customerService.findCustomers(PREVIEW_STORE_ID)
        );

        // 고객 등록 입력칸과 연결할 빈 Form 객체를 화면에 전달한다.
        model.addAttribute("customerForm", new CustomerForm());

        return "customer/list";
    }
}