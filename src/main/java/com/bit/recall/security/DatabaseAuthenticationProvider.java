package com.bit.recall.security;

import com.bit.recall.repo.UserRepository;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.provider.HttpRequestAuthenticationProvider;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Database-backed authentication provider.
 * Validates user credentials against the database.
 */
@Singleton
@RequiredArgsConstructor
public class DatabaseAuthenticationProvider implements HttpRequestAuthenticationProvider<Object> {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public @NonNull AuthenticationResponse authenticate(@Nullable HttpRequest<Object> requestContext, @NonNull AuthenticationRequest<String, String> authRequest) {
        String username = authRequest.getIdentity();
        String password = authRequest.getSecret();

        return userRepository.findByUsername(username)
                .filter(dbUser -> passwordEncoder.matches(password, dbUser.getPassword()))
                .map(dbUser -> {
                    Map<String, Object> attributes = Map.of(
                            "username", dbUser.getUsername(),
                            "email", dbUser.getEmail() != null ? dbUser.getEmail() : ""
                    );
                    return AuthenticationResponse.success(
                            dbUser.getId().toString(),
                            List.of("ROLE_USER"),
                            attributes
                    );
                })
                .orElse(AuthenticationResponse.failure("Invalid credentials"));
    }
}