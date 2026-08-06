package dev.jeyk.simplewallet.domain.usecase;

import java.util.Objects;

import dev.jeyk.simplewallet.domain.auth.AuthResult;
import dev.jeyk.simplewallet.domain.repository.AuthRepository;

public final class LoginUseCase {
    private final AuthRepository authRepository;

    public LoginUseCase(AuthRepository authRepository) {
        this.authRepository = Objects.requireNonNull(authRepository, "authRepository");
    }

    public AuthResult execute(String identifier, String password) {
        return authRepository.login(identifier.trim(), password);
    }
}
