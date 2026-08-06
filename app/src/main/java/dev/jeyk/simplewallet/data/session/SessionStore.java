package dev.jeyk.simplewallet.data.session;

public interface SessionStore {
    boolean isAuthenticated();

    void setAuthenticated(boolean authenticated);
}
