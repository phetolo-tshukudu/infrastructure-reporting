package com.community.reporting.dto.incident;

import java.util.List;

import com.community.reporting.entities.IncidentUpdate;
import com.community.reporting.enums.IncidentStatus;

public record IncidentHistoryResponse(
		Long incident_id,
		String incident_name,
		IncidentStatus current_status,
		List<IncidentUpdateResponse> updates_history
) {}
