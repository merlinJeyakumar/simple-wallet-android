package dev.jeyk.simplewallet.presentation.login;

public final class LoginUiState {
    private final boolean loading;
    private final String identifierError;
    private final String passwordError;
    private final String message;
    private final boolean failure;

    private LoginUiState(
            boolean loading,
            String identifierError,
            String passwordError,
            String message,
            boolean failure
    ) {
        this.loading = loading;
        this.identifierError = identifierError;
        this.passwordError = passwordError;
        this.message = message;
        this.failure = failure;
    }

    public static LoginUiState idle() {
        return new LoginUiState(false, null, null, null, false);
    }

    public static LoginUiState loading() {
        return new LoginUiState(true, null, null, null, false);
    }

    public static LoginUiState validation(String identifierError, String passwordError) {
        return new LoginUiState(false, identifierError, passwordError, null, false);
    }

    public static LoginUiState failure(String message) {
        return new LoginUiState(false, null, null, message, true);
    }

    public boolean isLoading() {
        return loading;
    }

    public String getIdentifierError() {
        return identifierError;
    }

    public String getPasswordError() {
        return passwordError;
    }

    public String getMessage() {
        return message;
    }

    public boolean isFailure() {
        return failure;
    }
}
