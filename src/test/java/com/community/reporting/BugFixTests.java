package com.community.reporting;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.community.reporting.dto.auth.LoginRequest;
import com.community.reporting.dto.auth.RegisterRequest;
import com.community.reporting.dto.incident.CreateIncidentRequest;
import com.community.reporting.dto.incident.UpdateIncidentStatusRequest;
import com.community.reporting.entities.Incident;
import com.community.reporting.entities.User;
import com.community.reporting.enums.IncidentCategory;
import com.community.reporting.enums.IncidentStatus;
import com.community.reporting.exceptions.*;
import com.community.reporting.repository.*;
import com.community.reporting.security.JwtService;
import com.community.reporting.services.*;
import jakarta.validation.Validation;

class BugFixTests {
    @Test
    void missingUserAndWrongPasswordHaveSameAuthenticationError() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AuthService service = new AuthService(users, encoder, mock(JwtService.class));
        when(users.findByEmail("user@example.com")).thenReturn(Optional.empty());
        var request = new LoginRequest("user@example.com", "password");
        assertThrows(BadCredentialsException.class, () -> service.login(request));
        when(users.findByEmail(request.email())).thenReturn(Optional.of(
            User.builder().password("hashed").build()));
        when(encoder.matches(request.password(), "hashed")).thenReturn(false);
        assertThrows(BadCredentialsException.class, () -> service.login(request));
    }

    @Test
    void duplicateRegistrationIncludingConcurrentInsertIsAConflict() {
        UserRepository users = mock(UserRepository.class);
        AuthService service = new AuthService(users, mock(PasswordEncoder.class), mock(JwtService.class));
        var request = new RegisterRequest("User", "user@example.com", "password");
        when(users.existsByEmail(request.email())).thenReturn(true);
        assertThrows(EmailAlreadyRegisteredException.class, () -> service.register(request));
        verify(users, never()).saveAndFlush(any());
        when(users.existsByEmail(request.email())).thenReturn(false);
        when(users.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate",
            new java.sql.SQLIntegrityConstraintViolationException("Duplicate email", "23000", 1062)));
        assertThrows(EmailAlreadyRegisteredException.class, () -> service.register(request));
    }

    @Test
    void unrelatedDatabaseFailureIsNotReportedAsDuplicateEmail() {
        UserRepository users = mock(UserRepository.class);
        var service = new AuthService(users, mock(PasswordEncoder.class), mock(JwtService.class));
        var failure = new DataIntegrityViolationException("Unexpected constraint");
        when(users.saveAndFlush(any())).thenThrow(failure);
        assertSame(failure, assertThrows(DataIntegrityViolationException.class, () -> service.register(
            new RegisterRequest("User", "user@example.com", "password"))));
    }

    @Test
    void invalidTransitionDoesNotWriteStatusOrHistory() {
        IncidentRepository incidents = mock(IncidentRepository.class);
        UserRepository users = mock(UserRepository.class);
        IncidentUpdateRepository updates = mock(IncidentUpdateRepository.class);
        var incident = Incident.builder().status(IncidentStatus.REPORTED).build();
        when(incidents.findById(1L)).thenReturn(Optional.of(incident));
        when(users.findByEmail("worker@example.com")).thenReturn(Optional.of(new User()));
        var service = new IncidentService(incidents, users, updates);
        assertThrows(InvalidStatusTransitionException.class, () -> service.updateStatus(1L,
            new UpdateIncidentStatusRequest(IncidentStatus.RESOLVED), "worker@example.com"));
        assertEquals(IncidentStatus.REPORTED, incident.getStatus());
        verify(incidents, never()).save(any());
        verifyNoInteractions(updates);
    }

    @Test
    void missingIncidentAndUserUseNotFoundException() {
        var service = new IncidentService(mock(IncidentRepository.class),
            mock(UserRepository.class), mock(IncidentUpdateRepository.class));
        assertThrows(ResourceNotFoundException.class, () -> service.getIncidentById(1L));
        assertThrows(ResourceNotFoundException.class, () -> service.getIncidentsByUser(1L,
            org.springframework.data.domain.Pageable.unpaged()));
    }

    @Test
    void handlersReturnExpectedHttpStatuses() {
        var handler = new GlobalExceptionHandler();
        assertEquals(401, handler.handleBadCredentials(new BadCredentialsException("bad")).getStatusCode().value());
        assertEquals(409, handler.handleDuplicateEmail(new EmailAlreadyRegisteredException()).getStatusCode().value());
        assertEquals(409, handler.handleConcurrentUpdate(new OptimisticLockingFailureException("stale")).getStatusCode().value());
        assertEquals(400, handler.handleInvalidTransition(new InvalidStatusTransitionException("invalid")).getStatusCode().value());
        assertEquals(404, handler.handleNotFound(new ResourceNotFoundException("missing")).getStatusCode().value());
    }

    @Test
    void incidentValidationChecksBoundsLengthAndPrecision() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new CreateIncidentRequest(IncidentCategory.POTHOLE,
                "Road damage", new BigDecimal("-90"), new BigDecimal("180"))).isEmpty());
            var invalid = new CreateIncidentRequest(IncidentCategory.POTHOLE, "x".repeat(1001),
                new BigDecimal("90.0000001"), new BigDecimal("180.00000001"));
            var fields = validator.validate(invalid).stream()
                .map(v -> v.getPropertyPath().toString()).collect(java.util.stream.Collectors.toSet());
            assertEquals(java.util.Set.of("description", "latitude", "longitude"), fields);
            assertFalse(validator.validate(new CreateIncidentRequest(IncidentCategory.POTHOLE,
                "Road damage", new BigDecimal("-90.1"), new BigDecimal("-180.1"))).isEmpty());
        }
    }
}


