package dev.jeyk.simplewallet.domain.usecase;

import java.util.regex.Pattern;

import javax.inject.Inject;

import dev.jeyk.simplewallet.domain.auth.LoginValidationResult;

public final class ValidateLoginUseCase {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"
    );
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{3,64}$");

    @Inject
    public ValidateLoginUseCase() {
    }

    public LoginValidationResult execute(String identifier, String password) {
        String normalizedIdentifier = identifier == null ? "" : identifier.trim();
        String identifierError = null;
        String passwordError = null;

        if (normalizedIdentifier.isEmpty()) {
            identifierError = "Email or username is required";
        } else if (!isValidIdentifier(normalizedIdentifier)) {
            identifierError = "Enter a valid email or username";
        }
        if (password == null || password.isEmpty()) {
            passwordError = "Password is required";
        }
        return LoginValidationResult.of(identifierError, passwordError);
    }

    private boolean isValidIdentifier(String identifier) {
        Pattern pattern = identifier.indexOf('@') >= 0 ? EMAIL_PATTERN : USERNAME_PATTERN;
        return pattern.matcher(identifier).matches();
    }
}
