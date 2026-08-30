package com.bit.recall.security;

import com.bit.recall.domain.User;
import com.bit.recall.repo.UserRepository;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.provider.HttpRequestAuthenticationProvider;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import javax.crypto.spec.OAEPParameterSpec;
import java.util.Optional;

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
        String user = authRequest.getIdentity();
        String password = authRequest.getSecret();
        Optional<User> u =  userRepository.findByUsername(user);

        return Optional.ofNullable(userRepository.findByUsername(user))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(dbUser -> passwordEncoder.matches(password, dbUser.getPassword()))
                .map(dbUser -> AuthenticationResponse.success(dbUser.getUsername()))
                .orElse(AuthenticationResponse.failure("Invalid credentials"));

    }
}