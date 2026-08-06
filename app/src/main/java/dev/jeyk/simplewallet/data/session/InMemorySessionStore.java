package dev.jeyk.simplewallet.data.session;

import java.util.concurrent.atomic.AtomicBoolean;

public final class InMemorySessionStore implements SessionStore {
    private final AtomicBoolean authenticated = new AtomicBoolean(false);

    @Override
    public boolean isAuthenticated() {
        return authenticated.get();
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        this.authenticated.set(authenticated);
    }
}
