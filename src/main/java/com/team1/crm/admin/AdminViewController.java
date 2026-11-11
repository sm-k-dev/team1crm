package com.team1.crm.admin;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import com.team1.crm.auth.Member;
import com.team1.crm.auth.Role;
import com.team1.crm.auth.security.LoginMember;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 관리자 화면과 전체 매장 목록을 제공한다.
 * 작성자: 김순민 / 작성일: 2026-11-11
 */
@Controller
@Profile("dev")
@RequiredArgsConstructor
public class AdminViewController {
    private final AdminStoreService adminStoreService;

    /** 관리자 첫 화면을 반환한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @GetMapping("/admin")
    public String index() {
        return "admin/index";
    }

    /** 운영 현황 화면을 반환한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @GetMapping("/admin/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }

    /** 사장님 관리 화면을 반환한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @GetMapping("/admin/owners")
    public String owners() {
        return "admin/owners";
    }

    /**
     * 관리자에게 전체 매장과 연결된 사장님 수를 표시한다.
     * 작성자: 김순민 / 작성일: 2026-11-11
     */
    @GetMapping("/admin/stores")
    public String stores(@LoginMember Member member, Model model) {
        if (!checkAdmin(member)) return "redirect:/login";
        model.addAttribute("storeForm", new AdminStoreForm());
        populate(model);
        return "admin/stores";
    }

    /** 선택한 매장의 기존 값으로 수정 폼을 연다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @GetMapping("/admin/stores/{storeId}/edit")
    public String edit(@LoginMember Member member, @PathVariable("storeId") Long storeId, Model model) {
        if (!checkAdmin(member)) return "redirect:/login";
        model.addAttribute("storeForm", adminStoreService.findForm(storeId));
        model.addAttribute("editingId", storeId);
        populate(model);
        return "admin/stores";
    }

    /** 등록 오류는 입력값과 함께 표시하고 성공하면 목록으로 이동한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @PostMapping("/admin/stores")
    public String create(@LoginMember Member member,
                         @Valid @ModelAttribute("storeForm") AdminStoreForm form, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        if (!checkAdmin(member)) return "redirect:/login";
        if (!errors.hasErrors()) {
            try { adminStoreService.create(form); }
            catch (IllegalArgumentException exception) { errors.rejectValue("ownerId", "invalid", exception.getMessage()); }
        }
        if (errors.hasErrors()) { populate(model); return "admin/stores"; }
        redirect.addFlashAttribute("successMessage", "매장을 등록했습니다.");
        return "redirect:/admin/stores";
    }

    /** 수정 실패 시 입력값을 보존하고 성공하면 PRG로 중복 제출을 줄인다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @PostMapping("/admin/stores/{storeId}")
    public String update(@LoginMember Member member, @PathVariable("storeId") Long storeId,
                         @Valid @ModelAttribute("storeForm") AdminStoreForm form, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        if (!checkAdmin(member)) return "redirect:/login";
        adminStoreService.findForm(storeId);
        if (!errors.hasErrors()) {
            try { adminStoreService.update(storeId, form); }
            catch (IllegalArgumentException exception) { errors.rejectValue("ownerId", "invalid", exception.getMessage()); }
        }
        if (errors.hasErrors()) {
            model.addAttribute("editingId", storeId);
            populate(model);
            return "admin/stores";
        }
        redirect.addFlashAttribute("successMessage", "매장 정보를 수정했습니다.");
        return "redirect:/admin/stores";
    }

    /** 공통 화면 데이터와 사장님 선택지를 채운다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private void populate(Model model) {
        var stores = adminStoreService.findStores();
        model.addAttribute("stores", stores);
        model.addAttribute("storeCount", stores.size());
        model.addAttribute("linkedStoreCount", stores.stream().filter(s -> s.ownerId() != null).count());
        model.addAttribute("linkedOwnerCount", stores.stream().map(AdminStoreRow::ownerId)
                .filter(java.util.Objects::nonNull).distinct().count());
        model.addAttribute("owners", adminStoreService.findOwners());
    }

    /** 미로그인과 권한 부족을 구분한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private boolean checkAdmin(Member member) {
        if (member == null) return false;
        if (member.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 접근할 수 있습니다.");
        }
        return true;
    }

}
