package dev.jeyk.simplewallet.data.datasource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import dev.jeyk.simplewallet.data.exception.DataSourceException;
import dev.jeyk.simplewallet.data.session.InMemorySessionStore;

public final class FakeAuthDataSourceTest {
    @Test
    public void demoEmailAndUsernameAuthenticate() {
        FakeAuthDataSource source = new FakeAuthDataSource();

        assertTrue(source.login(" DEMO@EXAMPLE.COM ", "password123").isSuccess());
        source.logout();
        assertTrue(source.login("demo", "password123").isSuccess());
        assertTrue(source.isAuthenticated());
    }

    @Test
    public void invalidCredentialsAndLogoutClearSession() {
        FakeAuthDataSource source = new FakeAuthDataSource();
        source.login("demo", "password123");

        assertFalse(source.login("demo", "incorrect").isSuccess());
        assertFalse(source.isAuthenticated());
        source.login("demo", "password123");
        source.logout();
        assertFalse(source.isAuthenticated());
    }

    @Test
    public void errorIdentifierProducesDeterministicServiceFailure() {
        FakeAuthDataSource source = new FakeAuthDataSource();

        assertThrows(DataSourceException.class,
                () -> source.login("error@example.com", "password123"));
        assertFalse(source.isAuthenticated());
    }

    @Test
    public void sharedSessionStoreRestoresAuthenticationAcrossDataSourceInstances() {
        InMemorySessionStore sessionStore = new InMemorySessionStore();
        FakeAuthDataSource firstInstance = new FakeAuthDataSource(sessionStore);

        assertTrue(firstInstance.login("demo", "password123").isSuccess());
        assertTrue(new FakeAuthDataSource(sessionStore).isAuthenticated());

        new FakeAuthDataSource(sessionStore).logout();
        assertFalse(firstInstance.isAuthenticated());
    }
}
