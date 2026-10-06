package com.community.reporting.dto.analytics;

import com.community.reporting.enums.IncidentCategory;

public record CategoryCountResponse(
    IncidentCategory category,
    Long count
) {}