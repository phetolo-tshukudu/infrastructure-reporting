package com.community.reporting.dto.incident;



import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.community.reporting.enums.IncidentCategory;
import com.community.reporting.enums.IncidentStatus;

public record IncidentResponse(

    Long id,
    IncidentCategory category,
    String description,
    BigDecimal latitude,
    BigDecimal longitude,
    IncidentStatus status,
    Long reportedByUserId,
    String reportedByName,
    LocalDateTime createdAt

) {}