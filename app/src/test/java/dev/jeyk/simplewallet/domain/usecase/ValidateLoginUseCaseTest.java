package dev.jeyk.simplewallet.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import dev.jeyk.simplewallet.domain.auth.LoginValidationResult;

public final class ValidateLoginUseCaseTest {
    private final ValidateLoginUseCase useCase = new ValidateLoginUseCase();

    @Test
    public void blankFieldsReturnFieldSpecificErrors() {
        LoginValidationResult result = useCase.execute("  ", "");

        assertFalse(result.isValid());
        assertEquals("Email or username is required", result.getIdentifierError());
        assertEquals("Password is required", result.getPasswordError());
    }

    @Test
    public void malformedIdentifierIsRejected() {
        LoginValidationResult result = useCase.execute("not@valid", "password123");

        assertFalse(result.isValid());
        assertEquals("Enter a valid email or username", result.getIdentifierError());
        assertNull(result.getPasswordError());
    }

    @Test
    public void emailAndUsernameFormatsAreAccepted() {
        assertTrue(useCase.execute("demo@example.com", "password123").isValid());
        assertTrue(useCase.execute("demo", "password123").isValid());
    }
}
