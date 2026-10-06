package com.community.reporting.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.community.reporting.dto.auth.AuthResponse;
import com.community.reporting.dto.auth.LoginRequest;
import com.community.reporting.dto.auth.RegisterRequest;
import com.community.reporting.entities.User;
import com.community.reporting.enums.Role;
import com.community.reporting.exceptions.EmailAlreadyRegisteredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.dao.DataIntegrityViolationException;
import com.community.reporting.repository.UserRepository;
import com.community.reporting.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = User.builder()
            .name(request.name())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .role(Role.RESIDENT)
            .build();

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            Throwable cause = ex.getMostSpecificCause();
            if (!(cause instanceof java.sql.SQLException sql)
                    || (sql.getErrorCode() != 1062 && !"23505".equals(sql.getSQLState()))) {
                throw ex;
            }
            // Flush inside this transaction so concurrent duplicate registration
            // receives the same response as the pre-check.
            throw new EmailAlreadyRegisteredException();
        }

        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() ->
                new BadCredentialsException("Invalid credentials")
            );

        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword())) {

            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getEmail());

        return new AuthResponse(token);
    }
}

