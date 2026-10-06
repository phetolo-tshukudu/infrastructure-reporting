package com.community.reporting.dto.incident;

import java.time.LocalDateTime;

import com.community.reporting.enums.IncidentStatus;

public record IncidentUpdateResponse(
    Long id,
    IncidentStatus oldStatus,
    IncidentStatus newStatus,
    Long updatedByUserId,
    String updatedByName,
    LocalDateTime createdAt
) {}