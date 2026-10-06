package com.community.reporting.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.community.reporting.dto.analytics.AverageResolutionTimeResponse;
import com.community.reporting.dto.analytics.CategoryCountResponse;
import com.community.reporting.dto.analytics.DailyIncidentCountResponse;
import com.community.reporting.dto.analytics.DashboardSummaryResponse;
import com.community.reporting.dto.analytics.StatusCountResponse;
import com.community.reporting.dto.incident.IncidentResponse;
import com.community.reporting.enums.IncidentStatus;
import com.community.reporting.repository.IncidentRepository;
import com.community.reporting.repository.IncidentUpdateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final IncidentRepository incidentRepository;
    private final IncidentUpdateRepository incidentUpdateRepository;

    @Transactional(readOnly = true)
    public List<CategoryCountResponse> getIncidentsByCategory() {
        return incidentRepository.countIncidentsByCategory();
    }

    @Transactional(readOnly = true)
    public List<StatusCountResponse> getIncidentsByStatus() {
        return incidentRepository.countIncidentsByStatus();
    }
    
    @Transactional(readOnly = true)
    public List<DailyIncidentCountResponse> getIncidentsPerDay() {

        return incidentRepository
            .countIncidentsPerDayRaw()
            .stream()
            .map(row -> new DailyIncidentCountResponse(
                ((java.sql.Date) row[0]).toLocalDate(),
                ((Number) row[1]).longValue()
            ))
            .toList();
    }
    @Transactional(readOnly = true)
    public List<IncidentResponse> getRecentIncidents() {

        return incidentRepository
            .findTop10ByOrderByCreatedAtDesc()
            .stream()
            .map(incident -> new IncidentResponse(
                incident.getId(),
                incident.getCategory(),
                incident.getDescription(),
                incident.getLatitude(),
                incident.getLongitude(),
                incident.getStatus(),
                incident.getReportedBy().getId(),
                incident.getReportedBy().getName(),
                incident.getCreatedAt()
            ))
            .toList();
    }
    
    @Transactional(readOnly = true)
    public AverageResolutionTimeResponse getAverageResolutionTime() {

        Double average =
            incidentUpdateRepository.calculateAverageResolutionHours();

        if (average == null) {
            average = 0.0;
        }

        return new AverageResolutionTimeResponse(average);
    }
    
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {

        long total = incidentRepository.count();

        long resolved =
            incidentRepository.countByStatus(
                IncidentStatus.RESOLVED
            );

        long open = total - resolved;

        Double averageResolution =
            incidentUpdateRepository
                .calculateAverageResolutionHours();

        if (averageResolution == null) {
            averageResolution = 0.0;
        }

        return new DashboardSummaryResponse(
            total,
            resolved,
            open,
            averageResolution,
            incidentRepository.countIncidentsByCategory(),
            incidentRepository.countIncidentsByStatus()
        );
    }
}