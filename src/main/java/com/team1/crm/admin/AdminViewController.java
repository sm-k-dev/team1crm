package com.team1.crm.admin;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Development-only page navigation. Business operations remain unimplemented. */
@Controller
@Profile("dev")
public class AdminViewController {
    @GetMapping("/admin")
    public String index() {
        return "admin/index";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/owners")
    public String owners() {
        return "admin/owners";
    }

    @GetMapping("/admin/stores")
    public String stores() {
        return "admin/stores";
    }

}
