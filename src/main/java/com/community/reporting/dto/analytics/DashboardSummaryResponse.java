package com.community.reporting.dto.analytics;

import java.util.List;

public record DashboardSummaryResponse(
    long totalIncidents,
    long resolvedIncidents,
    long openIncidents,
    Double averageResolutionHours,
    List<CategoryCountResponse> incidentsByCategory,
    List<StatusCountResponse> incidentsByStatus
) {}