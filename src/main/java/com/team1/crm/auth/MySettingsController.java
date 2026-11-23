package com.team1.crm.auth;

import com.team1.crm.auth.security.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 로그인 회원 자신의 정보와 매장 수정 화면이다. 작성자: 김순민 / 작성일: 2026-11-12 */
@Controller @RequiredArgsConstructor
public class MySettingsController {
    private final MySettingsService service;

    /** 본인 이름과 이메일을 표시한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    @GetMapping("/my-profile")
    public String profile(@LoginMember Member member, Model model) {
        if (member == null) return "redirect:/login";
        var info = service.profile(member.getId()); var form = new MyProfileForm(); form.setNickname(info.nickname());
        model.addAttribute("profile", info); model.addAttribute("profileForm", form);
        return "auth/my-profile";
    }

    /** 본인 이름만 저장하고 중복은 폼에 표시한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    @PostMapping("/my-profile")
    public String updateProfile(@LoginMember Member member, @Valid @ModelAttribute("profileForm") MyProfileForm form,
                                BindingResult errors, Model model, RedirectAttributes redirect) {
        if (member == null) return "redirect:/login";
        if (!errors.hasErrors()) {
            try { service.updateProfile(member.getId(), form); }
            catch (IllegalArgumentException e) { errors.rejectValue("nickname", "duplicate", e.getMessage()); }
            catch (DataIntegrityViolationException e) {
                if (!service.nicknameTaken(member.getId(), form.getNickname())) throw e;
                errors.rejectValue("nickname", "duplicate", "이미 사용 중인 이름입니다.");
            }
        }
        if (errors.hasErrors()) { model.addAttribute("profile", service.profile(member.getId())); return "auth/my-profile"; }
        redirect.addFlashAttribute("successMessage", "내 정보를 수정했습니다."); return "redirect:/my-profile";
    }

    /** 사장님 본인의 영업 매장 수정 폼을 표시한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    @GetMapping("/stores/{storeId}/edit")
    public String editStore(@LoginMember Member member, @PathVariable("storeId") Long id, Model model) {
        if (member == null) return "redirect:/login"; owner(member);
        Store store = service.ownedStore(member.getId(), id); var form = new MyStoreForm();
        form.setName(store.getName()); form.setCategory(store.getCategory()); form.setAddress(store.getAddress()); form.setPointRate(store.getPointRate());
        model.addAttribute("store", store); model.addAttribute("storeForm", form); return "auth/my-store-edit";
    }

    /** 검증 오류와 입력값을 보존하며 소유자 변경은 받지 않는다. 작성자: 김순민 / 작성일: 2026-11-12 */
    @PostMapping("/stores/{storeId}/edit")
    public String updateStore(@LoginMember Member member, @PathVariable("storeId") Long id,
                              @Valid @ModelAttribute("storeForm") MyStoreForm form, BindingResult errors,
                              Model model, RedirectAttributes redirect) {
        if (member == null) return "redirect:/login"; owner(member);
        Store store = service.ownedStore(member.getId(), id);
        if (!errors.hasErrors()) {
            try { service.updateStore(member.getId(), id, form); }
            catch (IllegalArgumentException e) { errors.rejectValue("name", "duplicate", e.getMessage()); }
        }
        if (errors.hasErrors()) { model.addAttribute("store", store); return "auth/my-store-edit"; }
        redirect.addFlashAttribute("successMessage", "매장 정보를 수정했습니다."); return "redirect:/store-settings";
    }
    /** 매장 수정은 사장님 역할로 한정한다. 작성자: 김순민 / 작성일: 2026-11-12 */
    private void owner(Member member) {
        if (member.getRole() != Role.USER) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
}
