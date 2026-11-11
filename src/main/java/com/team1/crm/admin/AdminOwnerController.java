package com.team1.crm.admin;

import com.team1.crm.auth.Member;
import com.team1.crm.auth.Role;
import com.team1.crm.auth.security.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 관리자 사장님 계정 등록 요청을 처리한다. 작성자: 김순민 / 작성일: 2026-11-11 */
@Controller
@Profile("dev")
@RequiredArgsConstructor
public class AdminOwnerController {
    private final AdminOwnerService service;
    private final AdminStoreService stores;

    /** 기존 이름을 수정 폼에 전달한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @GetMapping("/admin/owners/{ownerId}/edit")
    public String edit(@LoginMember Member member, @PathVariable("ownerId") Long ownerId, Model model) {
        if (!checkAdmin(member)) return "redirect:/login";
        var owner = service.findOwner(ownerId);
        var form = new AdminOwnerEditForm(); form.setNickname(owner.nickname());
        model.addAttribute("owner", owner);
        model.addAttribute("ownerEditForm", form);
        return "admin/owner-edit";
    }

    /** 이름 검증 오류는 폼에 표시하고 성공 시 목록으로 이동한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @PostMapping("/admin/owners/{ownerId}")
    public String update(@LoginMember Member member, @PathVariable("ownerId") Long ownerId,
                         @Valid @ModelAttribute("ownerEditForm") AdminOwnerEditForm form, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        if (!checkAdmin(member)) return "redirect:/login";
        var owner = service.findOwner(ownerId);
        if (!errors.hasErrors()) {
            try { service.updateNickname(ownerId, form); }
            catch (IllegalArgumentException exception) { errors.rejectValue("nickname", "duplicate", exception.getMessage()); }
            catch (DataIntegrityViolationException exception) {
                if (!service.nicknameTaken(ownerId, form.getNickname())) throw exception;
                errors.rejectValue("nickname", "duplicate", "이미 사용 중인 이름입니다.");
            }
        }
        if (errors.hasErrors()) {
            model.addAttribute("owner", owner);
            return "admin/owner-edit";
        }
        redirect.addFlashAttribute("successMessage", "사장님 이름을 수정했습니다.");
        return "redirect:/admin/owners";
    }

    /** 수정 화면과 요청에 관리자 권한을 적용한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private boolean checkAdmin(Member member) {
        if (member == null) return false;
        if (member.getRole() != Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 접근할 수 있습니다.");
        return true;
    }

    /** 검증·권한·중복을 확인하고 성공 시 목록으로 이동한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    @PostMapping("/admin/owners")
    public String create(@LoginMember Member member,
                         @Valid @ModelAttribute("ownerForm") AdminOwnerForm form, BindingResult errors,
                         Model model, RedirectAttributes redirect) {
        if (member == null) return "redirect:/login";
        if (member.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자만 접근할 수 있습니다.");
        }
        if (!errors.hasErrors()) {
            try { service.create(form); }
            catch (IllegalArgumentException exception) { rejectDuplicate(errors, exception.getMessage()); }
            catch (DataIntegrityViolationException exception) {
                // 동시 등록 중 DB 유니크 위반은 롤백이 끝난 뒤 새 조회로 확인한다.
                String field = service.duplicateField(form);
                if (field == null) throw exception;
                rejectDuplicate(errors, field);
            }
        }
        if (errors.hasErrors()) {
            form.setPassword(null);
            var owners = stores.findOwnerRows();
            model.addAttribute("owners", owners);
            model.addAttribute("ownerCount", owners.size());
            model.addAttribute("linkedOwnerCount", owners.stream().filter(owner -> !owner.stores().isEmpty()).count());
            model.addAttribute("unlinkedOwnerCount", owners.stream().filter(owner -> owner.stores().isEmpty()).count());
            return "admin/owners";
        }
        redirect.addFlashAttribute("successMessage", "사장님 계정을 등록했습니다. 매장 관리에서 매장을 연결해 주세요.");
        return "redirect:/admin/owners";
    }

    /** 중복 오류를 해당 필드에 표시한다. 작성자: 김순민 / 작성일: 2026-11-11 */
    private void rejectDuplicate(BindingResult errors, String field) {
        if (!"email".equals(field) && !"nickname".equals(field)) throw new IllegalArgumentException("알 수 없는 중복 필드");
        errors.rejectValue(field, "duplicate", "email".equals(field) ? "이미 가입된 이메일입니다." : "이미 사용 중인 이름입니다.");
    }
}
