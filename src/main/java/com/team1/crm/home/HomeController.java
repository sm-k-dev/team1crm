package com.team1.crm.home;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Development-only page navigation. Business operations remain unimplemented. */
@Controller
@Profile("dev")
public class HomeController {
    @GetMapping({"/", "/index.html"})
    public String home() {
        return "home/index";
    }

}
