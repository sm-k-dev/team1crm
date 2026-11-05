package com.team1.crm.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Development-only page navigation. Business operations remain unimplemented. */
@Controller
@Profile("dev")
public class MemberViewController {
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signup() {
        return "auth/signup";
    }

}
