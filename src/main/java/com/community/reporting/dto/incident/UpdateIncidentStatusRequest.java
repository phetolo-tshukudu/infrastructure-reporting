package com.community.reporting.dto.incident;

import com.community.reporting.enums.IncidentStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateIncidentStatusRequest(

    @NotNull
    IncidentStatus status

) {}