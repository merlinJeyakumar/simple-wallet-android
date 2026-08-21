package dev.jeyk.simplewallet.domain.usecase;

import java.util.Objects;

import javax.inject.Inject;

import dev.jeyk.simplewallet.domain.repository.AuthRepository;

public final class LogoutUseCase {
    private final AuthRepository authRepository;

    @Inject
    public LogoutUseCase(AuthRepository authRepository) {
        this.authRepository = Objects.requireNonNull(authRepository, "authRepository");
    }

    public void execute() {
        authRepository.logout();
    }
}
