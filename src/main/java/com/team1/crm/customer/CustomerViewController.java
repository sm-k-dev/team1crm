package com.team1.crm.customer;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Development-only page navigation. Business operations remain unimplemented. */
@Controller
@Profile("dev")
public class CustomerViewController {
    @GetMapping("/customers")
    public String list() {
        return "customer/list";
    }

}
