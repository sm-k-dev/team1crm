package com.team1.crm.auth;

import com.team1.crm.auth.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 내 매장 정보에서 소유 사장님의 폐업 요청을 처리한다. 작성자: 김순민 / 작성일: 2026-11-11 */
@Controller
@RequiredArgsConstructor
public class StoreClosureController {
    private final StoreClosureService service;

    /** 사장님 본인 요청만 허용하고 처리 후 내 매장 정보로 이동한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @PostMapping("/stores/{storeId}/close")
    public String close(@LoginMember Member member, @PathVariable("storeId") Long storeId, RedirectAttributes redirect) {
        if (member == null) return "redirect:/login";
        if (member.getRole() != Role.USER) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        service.close(member.getId(), storeId);
        redirect.addFlashAttribute("successMessage", "폐업 처리했습니다. 매장명과 과거 거래·통계는 보존됩니다.");
        return "redirect:/store-settings";
    }
}
