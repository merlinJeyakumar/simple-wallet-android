package dev.jeyk.simplewallet.domain.repository;

import dev.jeyk.simplewallet.domain.auth.AuthResult;

public interface AuthRepository {
    AuthResult login(String identifier, String password);

    void logout();

    boolean isAuthenticated();
}
