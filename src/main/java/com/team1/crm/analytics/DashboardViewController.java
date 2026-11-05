package com.team1.crm.analytics;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Development-only page navigation. Business operations remain unimplemented. */
@Controller
@Profile("dev")
public class DashboardViewController {
    @GetMapping("/analytics/dashboard")
    public String dashboard() {
        return "analytics/dashboard";
    }

}
