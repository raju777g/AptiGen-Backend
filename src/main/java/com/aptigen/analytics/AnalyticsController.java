package com.aptigen.analytics;

import com.aptigen.common.AuthUtil;
import com.aptigen.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final SkillRadarService skillRadarService;
    private final UserService userService;
    private final AuthUtil authUtil;
    private final DashboardStatsService dashboardStatsService;

    @GetMapping("/skill-radar")
    public ResponseEntity<?> skillRadar(Authentication auth) {
        Long userId = userService.findByEmail(authUtil.extractEmail(auth)).getId();
        return ResponseEntity.ok(skillRadarService.getSkillRadar(userId));
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<?> dashboardStats(Authentication auth) {
        Long userId = userService.findByEmail(authUtil.extractEmail(auth)).getId();
        return ResponseEntity.ok(dashboardStatsService.getStats(userId));
    }

    @GetMapping("/skill-radar/average")
    public ResponseEntity<?> skillRadarAverage() {
        return ResponseEntity.ok(skillRadarService.getPlatformAverage());
    }

}
