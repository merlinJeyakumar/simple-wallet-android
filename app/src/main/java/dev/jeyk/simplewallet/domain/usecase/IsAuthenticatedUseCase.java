package dev.jeyk.simplewallet.domain.usecase;

import java.util.Objects;

import javax.inject.Inject;

import dev.jeyk.simplewallet.domain.repository.AuthRepository;

public final class IsAuthenticatedUseCase {
    private final AuthRepository authRepository;

    @Inject
    public IsAuthenticatedUseCase(AuthRepository authRepository) {
        this.authRepository = Objects.requireNonNull(authRepository, "authRepository");
    }

    public boolean execute() {
        return authRepository.isAuthenticated();
    }
}
