package com.community.reporting.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import com.community.reporting.dto.analytics.DailyIncidentCountResponse;
import com.community.reporting.dto.incident.CreateIncidentRequest;
import com.community.reporting.dto.incident.IncidentResponse;
import com.community.reporting.dto.incident.IncidentUpdateResponse;
import com.community.reporting.dto.incident.UpdateIncidentStatusRequest;
import com.community.reporting.services.AnalyticsService;
import com.community.reporting.services.IncidentService;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;

import com.community.reporting.enums.IncidentCategory;
import com.community.reporting.enums.IncidentStatus;
import org.springframework.security.core.Authentication;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;
    private final AnalyticsService analyticsService;

    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(
            @Valid @RequestBody CreateIncidentRequest request,
            Authentication authentication) {

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(
                incidentService.createIncident(
                    request,
                    authentication.getName()
                )
            );
    }
    
    @GetMapping
    public ResponseEntity<Page<IncidentResponse>> getIncidents(

            @RequestParam(required = false)
            IncidentStatus status,

            @RequestParam(required = false)
            IncidentCategory category,

            @PageableDefault(
                size = 20,
                sort = "createdAt",
                direction = Sort.Direction.DESC
            )
            Pageable pageable) {

        return ResponseEntity.ok(
            incidentService.getIncidents(
                status,
                category,
                pageable
            )
        );
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getIncident(
            @PathVariable Long id) {

        return ResponseEntity.ok(
            incidentService.getIncidentById(id)
        );
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<IncidentResponse>> getUserIncidents(
            @PathVariable Long userId,
            @PageableDefault(
                size = 20,
                sort = "createdAt",
                direction = Sort.Direction.DESC
            )
            Pageable pageable) {

        return ResponseEntity.ok(
            incidentService.getIncidentsByUser(
                userId,
                pageable
            )
        );
    }
    
    @PreAuthorize("hasAnyRole('WORKER', 'ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<IncidentResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIncidentStatusRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
            incidentService.updateStatus(
                id,
                request,
                authentication.getName()
            )
        );
    }
    
    @GetMapping("/{id}/history")
    public ResponseEntity<List<IncidentUpdateResponse>> getIncidentHistory(
            @PathVariable Long id) {

        return ResponseEntity.ok(
            incidentService.getIncidentHistory(id)
        );
    }
    
    
}