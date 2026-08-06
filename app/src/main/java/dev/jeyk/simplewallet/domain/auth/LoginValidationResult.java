package dev.jeyk.simplewallet.domain.auth;

public final class LoginValidationResult {
    private final String identifierError;
    private final String passwordError;

    private LoginValidationResult(String identifierError, String passwordError) {
        this.identifierError = identifierError;
        this.passwordError = passwordError;
    }

    public static LoginValidationResult of(String identifierError, String passwordError) {
        return new LoginValidationResult(identifierError, passwordError);
    }

    public boolean isValid() {
        return identifierError == null && passwordError == null;
    }

    public String getIdentifierError() {
        return identifierError;
    }

    public String getPasswordError() {
        return passwordError;
    }
}
