package com.community.reporting.dto.analytics;

import com.community.reporting.enums.IncidentStatus;

public record StatusCountResponse(
    IncidentStatus status,
    Long count
) {}