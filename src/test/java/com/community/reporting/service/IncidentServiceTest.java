package com.community.reporting.service;

import org.mockito.junit.jupiter.MockitoExtension;

import com.community.reporting.dto.incident.CreateIncidentRequest;
import com.community.reporting.dto.incident.IncidentResponse;
import com.community.reporting.entities.User;
import com.community.reporting.repository.IncidentRepository;
import com.community.reporting.repository.IncidentUpdateRepository;
import com.community.reporting.repository.UserRepository;
import com.community.reporting.services.IncidentService;
import com.community.reporting.enums.IncidentCategory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

@ExtendWith(MockitoExtension.class)
public class IncidentServiceTest {
	@Mock
	private IncidentRepository incidentRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private IncidentUpdateRepository incidentUpdateRepository;
	
	@InjectMocks
	private IncidentService incidentService;
	
	@Test
	public void testCreateIncident() {
		// Implement your test logic here
		CreateIncidentRequest request = new CreateIncidentRequest(
				IncidentCategory.POTHOLE
				,"Pothole on Main Street",
				new BigDecimal(12.34),
				new BigDecimal(56.78)
		);
		
		when(userRepository.findByEmail(anyString())).thenReturn(java.util.Optional.of(new User()));
		when(incidentRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
		
		IncidentResponse response = incidentService.createIncident(request, "phetolo@mail.com");
		
		assert(response != null);
		assert(response.category().equals(IncidentCategory.POTHOLE));
		assert(response.description().equals("Pothole on Main Street"));
		
		verify(userRepository, times(1)).findByEmail(anyString());
		verify(incidentRepository, times(1)).save(any());
		
	}
}
