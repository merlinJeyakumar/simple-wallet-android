package dev.jeyk.simplewallet.domain.usecase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import dev.jeyk.simplewallet.data.datasource.FakeAuthDataSource;
import dev.jeyk.simplewallet.data.repository.AuthRepositoryImpl;
import dev.jeyk.simplewallet.domain.repository.AuthRepository;

public final class AuthUseCasesTest {
    @Test
    public void loginLogoutAndSessionStateShareRepositoryState() {
        AuthRepository repository = new AuthRepositoryImpl(new FakeAuthDataSource());
        LoginUseCase login = new LoginUseCase(repository);
        LogoutUseCase logout = new LogoutUseCase(repository);
        IsAuthenticatedUseCase isAuthenticated = new IsAuthenticatedUseCase(repository);

        assertFalse(isAuthenticated.execute());
        assertTrue(login.execute("demo", "password123").isSuccess());
        assertTrue(isAuthenticated.execute());
        logout.execute();
        assertFalse(isAuthenticated.execute());
    }
}
