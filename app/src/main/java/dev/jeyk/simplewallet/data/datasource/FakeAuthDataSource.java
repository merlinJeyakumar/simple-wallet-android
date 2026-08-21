package dev.jeyk.simplewallet.data.datasource;

import java.util.Locale;
import java.util.Objects;

import javax.inject.Inject;

import dev.jeyk.simplewallet.data.exception.DataSourceException;
import dev.jeyk.simplewallet.data.session.InMemorySessionStore;
import dev.jeyk.simplewallet.data.session.SessionStore;
import dev.jeyk.simplewallet.domain.auth.AuthResult;

public final class FakeAuthDataSource implements AuthDataSource {
    public static final String DEMO_EMAIL = "demo@example.com";
    public static final String DEMO_USERNAME = "demo";
    public static final String DEMO_PASSWORD = "password123";
    public static final String ERROR_IDENTIFIER = "error@example.com";

    private final SessionStore sessionStore;

    public FakeAuthDataSource() {
        this(new InMemorySessionStore());
    }

    @Inject
    public FakeAuthDataSource(SessionStore sessionStore) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "sessionStore");
    }

    @Override
    public AuthResult login(String identifier, String password) {
        String normalizedIdentifier = identifier.trim().toLowerCase(Locale.ROOT);
        sessionStore.setAuthenticated(false);
        if (ERROR_IDENTIFIER.equals(normalizedIdentifier)) {
            throw new DataSourceException("The demo authentication service is unavailable");
        }
        boolean validIdentifier = DEMO_EMAIL.equals(normalizedIdentifier)
                || DEMO_USERNAME.equals(normalizedIdentifier);
        if (validIdentifier && DEMO_PASSWORD.equals(password)) {
            sessionStore.setAuthenticated(true);
            return AuthResult.success();
        }
        return AuthResult.invalidCredentials();
    }

    @Override
    public void logout() {
        sessionStore.setAuthenticated(false);
    }

    @Override
    public boolean isAuthenticated() {
        return sessionStore.isAuthenticated();
    }
}
