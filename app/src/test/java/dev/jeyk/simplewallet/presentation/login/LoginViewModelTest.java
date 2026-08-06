package dev.jeyk.simplewallet.presentation.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import dev.jeyk.simplewallet.domain.auth.AuthResult;
import dev.jeyk.simplewallet.domain.repository.AuthRepository;
import dev.jeyk.simplewallet.domain.usecase.LoginUseCase;
import dev.jeyk.simplewallet.domain.usecase.ValidateLoginUseCase;
import dev.jeyk.simplewallet.presentation.PausedExecutorService;
import dev.jeyk.simplewallet.presentation.RecordingRequestDelay;
import dev.jeyk.simplewallet.presentation.common.SingleEvent;

public final class LoginViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule =
            new InstantTaskExecutorRule();

    private StubAuthRepository authRepository;
    private PausedExecutorService executor;
    private RecordingRequestDelay requestDelay;
    private LoginViewModel viewModel;

    @Before
    public void setUp() {
        authRepository = new StubAuthRepository();
        executor = new PausedExecutorService();
        requestDelay = new RecordingRequestDelay();
        viewModel = new LoginViewModel(
                new ValidateLoginUseCase(),
                new LoginUseCase(authRepository),
                requestDelay,
                executor
        );
    }

    @Test
    public void emptyCredentialsExposeBothValidationErrorsWithoutStartingWork() {
        viewModel.login("", "");

        LoginUiState state = requireState();
        assertFalse(state.isLoading());
        assertNotNull(state.getIdentifierError());
        assertNotNull(state.getPasswordError());
        assertEquals(0, executor.queuedTaskCount());
        assertEquals(0, requestDelay.getAwaitCalls());
    }

    @Test
    public void validCredentialsTransitionThroughLoadingAndEmitOneNavigationEvent() {
        authRepository.result = AuthResult.success();

        viewModel.login("demo@example.com", "password123");

        assertTrue(requireState().isLoading());
        assertEquals(1, executor.queuedTaskCount());

        executor.runNext();

        assertEquals(1, requestDelay.getAwaitCalls());
        assertFalse(requireState().isLoading());
        SingleEvent<Boolean> event = viewModel.getAuthenticationEvents().getValue();
        assertNotNull(event);
        assertEquals(Boolean.TRUE, event.consume());
        assertNull(event.consume());
    }

    @Test
    public void invalidCredentialsBecomeAVisibleFailure() {
        authRepository.result = AuthResult.invalidCredentials();

        viewModel.login("unknown@example.com", "incorrect-password");
        assertTrue(requireState().isLoading());
        executor.runNext();

        LoginUiState state = requireState();
        assertFalse(state.isLoading());
        assertNotNull(state.getMessage());
        assertFalse(state.getMessage().isEmpty());
        assertNull(viewModel.getAuthenticationEvents().getValue());
    }

    @Test
    public void repositoryExceptionBecomesGenericPresentationFailure() {
        authRepository.failure = new IllegalStateException("service unavailable");

        viewModel.login("error@example.com", "password123");
        executor.runNext();

        LoginUiState state = requireState();
        assertFalse(state.isLoading());
        assertNull(state.getMessage());
        assertNull(viewModel.getAuthenticationEvents().getValue());
    }

    @Test
    public void repeatedSubmitWhileLoadingQueuesOnlyOneRequest() {
        viewModel.login("demo@example.com", "password123");
        viewModel.login("demo@example.com", "password123");

        assertEquals(1, executor.queuedTaskCount());
        executor.runNext();
        assertEquals(1, authRepository.loginCalls);
        assertEquals(1, requestDelay.getAwaitCalls());
    }

    @Test
    public void interruptedDelayRestoresInterruptAndProducesGenericFailure() {
        requestDelay.interruptOnAwait();

        viewModel.login("demo@example.com", "password123");
        executor.runNext();

        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
        assertNull(requireState().getMessage());
        assertEquals(0, authRepository.loginCalls);
    }

    private LoginUiState requireState() {
        LoginUiState state = viewModel.getState().getValue();
        assertNotNull(state);
        return state;
    }

    private static final class StubAuthRepository implements AuthRepository {
        private AuthResult result = AuthResult.success();
        private RuntimeException failure;
        private int loginCalls;
        private boolean authenticated;

        @Override
        public AuthResult login(String identifier, String password) {
            loginCalls++;
            if (failure != null) {
                throw failure;
            }
            authenticated = result.isSuccess();
            return result;
        }

        @Override
        public void logout() {
            authenticated = false;
        }

        @Override
        public boolean isAuthenticated() {
            return authenticated;
        }
    }

}
