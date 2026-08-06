package dev.jeyk.simplewallet.data.datasource;

import dev.jeyk.simplewallet.domain.auth.AuthResult;

public interface AuthDataSource {
    AuthResult login(String identifier, String password);

    void logout();

    boolean isAuthenticated();
}
