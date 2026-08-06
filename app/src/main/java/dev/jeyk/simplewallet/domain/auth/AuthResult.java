package dev.jeyk.simplewallet.domain.auth;

import java.util.Objects;

public final class AuthResult {
    private static final String SUCCESS_MESSAGE = "Signed in successfully";
    private static final String INVALID_MESSAGE = "Incorrect email, username, or password";

    private final AuthStatus status;
    private final String message;

    private AuthResult(AuthStatus status, String message) {
        this.status = Objects.requireNonNull(status, "status");
        this.message = Objects.requireNonNull(message, "message");
    }

    public static AuthResult success() {
        return new AuthResult(AuthStatus.SUCCESS, SUCCESS_MESSAGE);
    }

    public static AuthResult invalidCredentials() {
        return new AuthResult(AuthStatus.INVALID_CREDENTIALS, INVALID_MESSAGE);
    }

    public AuthStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return status == AuthStatus.SUCCESS;
    }
}
