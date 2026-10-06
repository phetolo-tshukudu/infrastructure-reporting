package com.community.reporting.dto.analytics;

import java.time.LocalDate;

public record DailyIncidentCountResponse(
    LocalDate date,
    Long count
) {}