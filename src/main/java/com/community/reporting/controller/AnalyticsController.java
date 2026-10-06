package com.community.reporting.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.community.reporting.dto.analytics.AverageResolutionTimeResponse;
import com.community.reporting.dto.analytics.CategoryCountResponse;
import com.community.reporting.dto.analytics.DailyIncidentCountResponse;
import com.community.reporting.dto.analytics.DashboardSummaryResponse;
import com.community.reporting.dto.analytics.StatusCountResponse;
import com.community.reporting.dto.incident.IncidentResponse;
import com.community.reporting.services.AnalyticsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/incidents-by-category")
    public ResponseEntity<List<CategoryCountResponse>>
            getIncidentsByCategory() {

        return ResponseEntity.ok(
            analyticsService.getIncidentsByCategory()
        );
    }

    @GetMapping("/incidents-by-status")
    public ResponseEntity<List<StatusCountResponse>>
            getIncidentsByStatus() {

        return ResponseEntity.ok(
            analyticsService.getIncidentsByStatus()
        );
    }
    
    @GetMapping("/recent-incidents")
    public ResponseEntity<List<IncidentResponse>>
            getRecentIncidents() {

        return ResponseEntity.ok(
            analyticsService.getRecentIncidents()
        );
    }
    
    @GetMapping("/incidents-per-day")
    public ResponseEntity<List<DailyIncidentCountResponse>>
            getIncidentsPerDay() {

        return ResponseEntity.ok(
            analyticsService.getIncidentsPerDay()
        );
    }
    
    @GetMapping("/average-resolution-time")
    public ResponseEntity<AverageResolutionTimeResponse>
            getAverageResolutionTime() {

        return ResponseEntity.ok(
            analyticsService.getAverageResolutionTime()
        );
    }
    
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardSummaryResponse>
            getDashboardSummary() {

        return ResponseEntity.ok(
            analyticsService.getDashboardSummary()
        );
    }
}