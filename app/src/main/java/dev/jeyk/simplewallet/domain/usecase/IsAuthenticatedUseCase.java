package dev.jeyk.simplewallet.domain.usecase;

import java.util.Objects;

import dev.jeyk.simplewallet.domain.repository.AuthRepository;

public final class IsAuthenticatedUseCase {
    private final AuthRepository authRepository;

    public IsAuthenticatedUseCase(AuthRepository authRepository) {
        this.authRepository = Objects.requireNonNull(authRepository, "authRepository");
    }

    public boolean execute() {
        return authRepository.isAuthenticated();
    }
}
