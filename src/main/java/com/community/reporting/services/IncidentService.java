package com.community.reporting.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.community.reporting.dto.incident.CreateIncidentRequest;
import com.community.reporting.dto.incident.IncidentResponse;
import com.community.reporting.dto.incident.IncidentUpdateResponse;
import com.community.reporting.dto.incident.UpdateIncidentStatusRequest;
import com.community.reporting.entities.Incident;
import com.community.reporting.entities.IncidentUpdate;
import com.community.reporting.entities.User;
import com.community.reporting.enums.IncidentStatus;
import com.community.reporting.exceptions.InvalidStatusTransitionException;
import com.community.reporting.exceptions.ResourceNotFoundException;
import com.community.reporting.repository.IncidentRepository;
import com.community.reporting.repository.IncidentUpdateRepository;
import com.community.reporting.repository.UserRepository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.community.reporting.enums.IncidentCategory;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final IncidentUpdateRepository incidentUpdateRepository;

    @Transactional
    public IncidentResponse createIncident(
            CreateIncidentRequest request,
            String email) {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() ->
                new ResourceNotFoundException("User not found")
            );

        Incident incident = Incident.builder()
            .category(request.category())
            .description(request.description())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .status(IncidentStatus.REPORTED)
            .reportedBy(user)
            .build();

        Incident saved = incidentRepository.save(incident);

        return toResponse(saved);
    }

    private IncidentResponse toResponse(Incident incident) {

        return new IncidentResponse(
            incident.getId(),
            incident.getCategory(),
            incident.getDescription(),
            incident.getLatitude(),
            incident.getLongitude(),
            incident.getStatus(),
            incident.getReportedBy().getId(),
            incident.getReportedBy().getName(),
            incident.getCreatedAt()
        );
    }
    
    @Transactional(readOnly = true)
    public Page<IncidentResponse> getIncidents(
            IncidentStatus status,
            IncidentCategory category,
            Pageable pageable) {

        Page<Incident> incidents;

        if (status != null && category != null) {
            incidents =
                incidentRepository.findByStatusAndCategory(
                    status,
                    category,
                    pageable
                );

        } else if (status != null) {
            incidents =
                incidentRepository.findByStatus(
                    status,
                    pageable
                );

        } else if (category != null) {
            incidents =
                incidentRepository.findByCategory(
                    category,
                    pageable
                );

        } else {
            incidents =
                incidentRepository.findAll(pageable);
        }

        return incidents.map(this::toResponse);
    }
    
    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(Long id) {

        Incident incident = incidentRepository.findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException("Incident not found")
            );

        return toResponse(incident);
    }
    
    
    @Transactional(readOnly = true)
    public Page<IncidentResponse> getIncidentsByUser(
            Long userId,
            Pageable pageable) {

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        return incidentRepository
            .findByReportedById(userId, pageable)
            .map(this::toResponse);
    }
    
    @Transactional
    public IncidentResponse updateStatus(
            Long incidentId,
            UpdateIncidentStatusRequest request,
            String email) {

        Incident incident = incidentRepository.findById(incidentId)
            .orElseThrow(() ->
                new ResourceNotFoundException("Incident not found")
            );

        User updater = userRepository.findByEmail(email)
            .orElseThrow(() ->
                new ResourceNotFoundException("User not found")
            );

        

        IncidentStatus oldStatus = incident.getStatus();
        IncidentStatus newStatus = request.status();

        validateStatusTransition(oldStatus, newStatus);

        incident.setStatus(newStatus);

        Incident savedIncident =
            incidentRepository.save(incident);

        IncidentUpdate update = IncidentUpdate.builder()
            .incident(savedIncident)
            .oldStatus(oldStatus)
            .newStatus(newStatus)
            .updatedBy(updater)
            .build();

        incidentUpdateRepository.save(update);

        return toResponse(savedIncident);
    }
    
    private void validateStatusTransition(
            IncidentStatus current,
            IncidentStatus next) {

        boolean valid = switch (current) {

            case REPORTED ->
                next == IncidentStatus.VERIFIED;

            case VERIFIED ->
                next == IncidentStatus.IN_PROGRESS;

            case IN_PROGRESS ->
                next == IncidentStatus.RESOLVED;

            case RESOLVED ->
                false;
        };

        if (!valid) {
            throw new InvalidStatusTransitionException(
                "Invalid status transition from "
                + current
                + " to "
                + next
            );
        }
    }
    
    @Transactional(readOnly = true)
    public List<IncidentUpdateResponse> getIncidentHistory(Long incidentId) {

        if (!incidentRepository.existsById(incidentId)) {
            throw new ResourceNotFoundException("Incident not found");
        }

        return incidentUpdateRepository
            .findByIncidentIdOrderByCreatedAtAsc(incidentId)
            .stream()
            .map(update -> new IncidentUpdateResponse(
                update.getId(),
                update.getOldStatus(),
                update.getNewStatus(),
                update.getUpdatedBy().getId(),
                update.getUpdatedBy().getName(),
                update.getCreatedAt()
            ))
            .toList();
    }
    
    
    
    
}
