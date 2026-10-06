package com.community.reporting.dto.incident;

import java.math.BigDecimal;

import com.community.reporting.enums.IncidentCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;

public record CreateIncidentRequest(

    @NotNull
    IncidentCategory category,

    @NotBlank
    @Size(max = 1000)
    String description,

    @NotNull
    @DecimalMin("-90")
    @DecimalMax("90")
    @Digits(integer = 3, fraction = 7)
    BigDecimal latitude,

    @NotNull
    @DecimalMin("-180")
    @DecimalMax("180")
    @Digits(integer = 3, fraction = 7)
    BigDecimal longitude

) {}
