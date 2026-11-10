
package com.team1.crm.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MemberViewController {

    // 로그인 화면
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    // 회원가입 화면
    @GetMapping("/signup")
    public String signup() {
        return "auth/signup";
    }
}
