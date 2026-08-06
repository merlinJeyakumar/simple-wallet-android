package dev.jeyk.simplewallet.presentation.dashboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

import dev.jeyk.simplewallet.domain.auth.AuthResult;
import dev.jeyk.simplewallet.domain.model.WalletAccount;
import dev.jeyk.simplewallet.domain.model.WalletTransaction;
import dev.jeyk.simplewallet.domain.repository.AuthRepository;
import dev.jeyk.simplewallet.domain.repository.WalletRepository;
import dev.jeyk.simplewallet.domain.usecase.GetAccountsUseCase;
import dev.jeyk.simplewallet.domain.usecase.LogoutUseCase;
import dev.jeyk.simplewallet.presentation.PausedExecutorService;
import dev.jeyk.simplewallet.presentation.RecordingRequestDelay;
import dev.jeyk.simplewallet.presentation.common.SingleEvent;
import dev.jeyk.simplewallet.presentation.common.UiState;

public final class DashboardViewModelTest {
    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule =
            new InstantTaskExecutorRule();

    private StubWalletRepository walletRepository;
    private StubAuthRepository authRepository;
    private PausedExecutorService executor;
    private RecordingRequestDelay requestDelay;
    private DashboardViewModel viewModel;

    @Before
    public void setUp() {
        walletRepository = new StubWalletRepository();
        authRepository = new StubAuthRepository();
        executor = new PausedExecutorService();
        requestDelay = new RecordingRequestDelay();
        viewModel = new DashboardViewModel(
                new GetAccountsUseCase(walletRepository),
                new LogoutUseCase(authRepository),
                requestDelay,
                executor
        );
    }

    @Test
    public void accountsTransitionFromLoadingToSuccess() {
        WalletAccount account = account("checking");
        walletRepository.accounts = List.of(account);

        viewModel.loadAccounts();
        assertEquals(UiState.Status.LOADING, requireAccountsState().getStatus());
        executor.runNext();

        assertEquals(1, requestDelay.getAwaitCalls());
        UiState<List<WalletAccount>> state = requireAccountsState();
        assertEquals(UiState.Status.SUCCESS, state.getStatus());
        assertEquals(List.of(account), state.getData());
    }

    @Test
    public void emptyRepositoryProducesExplicitEmptyState() {
        walletRepository.accounts = List.of();

        viewModel.loadAccounts();
        executor.runNext();

        UiState<List<WalletAccount>> state = requireAccountsState();
        assertEquals(UiState.Status.EMPTY, state.getStatus());
        assertNotNull(state.getData());
        assertTrue(state.getData().isEmpty());
    }

    @Test
    public void repositoryFailureProducesGenericErrorState() {
        walletRepository.failure = new IllegalStateException("fixture failure");

        viewModel.loadAccounts();
        executor.runNext();

        UiState<List<WalletAccount>> state = requireAccountsState();
        assertEquals(UiState.Status.ERROR, state.getStatus());
        assertNull(state.getMessage());
    }

    @Test
    public void repeatedLoadWhilePendingQueuesOnlyOneRequest() {
        viewModel.loadAccounts();
        viewModel.loadAccounts();

        assertEquals(1, executor.queuedTaskCount());
        executor.runNext();
        assertEquals(1, walletRepository.accountsCalls);
        assertEquals(1, requestDelay.getAwaitCalls());
    }

    @Test
    public void refreshPreservesVisibleAccountsUntilSuccessThenReplacesThem() {
        WalletAccount original = account("checking");
        WalletAccount replacement = account("savings");
        walletRepository.accounts = List.of(original);
        viewModel.loadAccounts();
        executor.runNext();

        walletRepository.accounts = List.of(replacement);
        viewModel.refreshAccounts();

        assertEquals(Boolean.TRUE, viewModel.getAccountsRefreshing().getValue());
        assertEquals(List.of(original), requireAccountsState().getData());
        assertEquals(1, executor.queuedTaskCount());

        executor.runNext();

        assertEquals(Boolean.FALSE, viewModel.getAccountsRefreshing().getValue());
        assertEquals(UiState.Status.SUCCESS, requireAccountsState().getStatus());
        assertEquals(List.of(replacement), requireAccountsState().getData());
        assertEquals(2, walletRepository.accountsCalls);
        assertEquals(2, requestDelay.getAwaitCalls());
    }

    @Test
    public void refreshCanReplaceVisibleAccountsWithEmptyState() {
        walletRepository.accounts = List.of(account("checking"));
        viewModel.loadAccounts();
        executor.runNext();

        walletRepository.accounts = List.of();
        viewModel.refreshAccounts();
        executor.runNext();

        assertEquals(UiState.Status.EMPTY, requireAccountsState().getStatus());
        assertTrue(requireAccountsState().getData().isEmpty());
        assertEquals(Boolean.FALSE, viewModel.getAccountsRefreshing().getValue());
    }

    @Test
    public void refreshFailurePreservesAccountsAndEmitsSingleFailureEvent() {
        WalletAccount original = account("checking");
        walletRepository.accounts = List.of(original);
        viewModel.loadAccounts();
        executor.runNext();

        walletRepository.failure = new IllegalStateException("private backend detail");
        viewModel.refreshAccounts();
        executor.runNext();

        assertEquals(UiState.Status.SUCCESS, requireAccountsState().getStatus());
        assertEquals(List.of(original), requireAccountsState().getData());
        assertEquals(Boolean.FALSE, viewModel.getAccountsRefreshing().getValue());
        SingleEvent<Boolean> event = viewModel.getAccountsRefreshFailureEvents().getValue();
        assertNotNull(event);
        assertEquals(Boolean.TRUE, event.consume());
        assertNull(event.consume());
    }

    @Test
    public void repeatedRefreshWhilePendingQueuesOnlyOneRequest() {
        walletRepository.accounts = List.of(account("checking"));
        viewModel.loadAccounts();
        executor.runNext();

        viewModel.refreshAccounts();
        viewModel.refreshAccounts();

        assertEquals(Boolean.TRUE, viewModel.getAccountsRefreshing().getValue());
        assertEquals(1, executor.queuedTaskCount());
        executor.runNext();
        assertEquals(2, walletRepository.accountsCalls);
        assertEquals(2, requestDelay.getAwaitCalls());
    }

    @Test
    public void interruptedRefreshPreservesAccountsAndRestoresInterrupt() {
        WalletAccount original = account("checking");
        walletRepository.accounts = List.of(original);
        viewModel.loadAccounts();
        executor.runNext();
        requestDelay.interruptOnAwait();

        viewModel.refreshAccounts();
        executor.runNext();

        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
        assertEquals(UiState.Status.SUCCESS, requireAccountsState().getStatus());
        assertEquals(List.of(original), requireAccountsState().getData());
        assertEquals(Boolean.FALSE, viewModel.getAccountsRefreshing().getValue());
        assertEquals(1, walletRepository.accountsCalls);
        assertNotNull(viewModel.getAccountsRefreshFailureEvents().getValue());
    }

    @Test
    public void logoutTransitionsLoadingAndEmitsSuccessOnce() {
        viewModel.logout();
        assertEquals(Boolean.TRUE, viewModel.getLogoutLoading().getValue());

        executor.runNext();

        assertEquals(Boolean.FALSE, viewModel.getLogoutLoading().getValue());
        assertEquals(1, authRepository.logoutCalls);
        assertEquals(1, requestDelay.getAwaitCalls());
        SingleEvent<Boolean> event = viewModel.getLogoutEvents().getValue();
        assertNotNull(event);
        assertEquals(Boolean.TRUE, event.consume());
        assertNull(event.consume());
    }

    @Test
    public void logoutFailureIsReportedWithoutLeakingExceptionText() {
        authRepository.logoutFailure = new IllegalStateException("private backend detail");

        viewModel.logout();
        executor.runNext();

        assertEquals(Boolean.FALSE, viewModel.getLogoutLoading().getValue());
        SingleEvent<Boolean> event = viewModel.getLogoutEvents().getValue();
        assertNotNull(event);
        assertEquals(Boolean.FALSE, event.consume());
    }

    @Test
    public void interruptedAccountsDelayRestoresInterruptAndProducesGenericError() {
        requestDelay.interruptOnAwait();

        viewModel.loadAccounts();
        executor.runNext();

        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
        assertEquals(UiState.Status.ERROR, requireAccountsState().getStatus());
        assertEquals(0, walletRepository.accountsCalls);
    }

    @Test
    public void interruptedLogoutDelayRestoresInterruptAndReportsFailure() {
        requestDelay.interruptOnAwait();

        viewModel.logout();
        executor.runNext();

        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
        assertEquals(0, authRepository.logoutCalls);
        SingleEvent<Boolean> event = viewModel.getLogoutEvents().getValue();
        assertNotNull(event);
        assertEquals(Boolean.FALSE, event.consume());
    }

    private UiState<List<WalletAccount>> requireAccountsState() {
        UiState<List<WalletAccount>> state = viewModel.getAccountsState().getValue();
        assertNotNull(state);
        return state;
    }

    private static WalletAccount account(String id) {
        return new WalletAccount(
                id,
                "Test Account",
                Currency.getInstance("USD"),
                new BigDecimal("25.00"),
                "**** 1000"
        );
    }

    private static final class StubWalletRepository implements WalletRepository {
        private List<WalletAccount> accounts = List.of();
        private RuntimeException failure;
        private int accountsCalls;

        @Override
        public List<WalletAccount> getAccounts() {
            accountsCalls++;
            if (failure != null) {
                throw failure;
            }
            return accounts;
        }

        @Override
        public List<WalletTransaction> getTransactions(String accountId) {
            return List.of();
        }
    }

    private static final class StubAuthRepository implements AuthRepository {
        private RuntimeException logoutFailure;
        private int logoutCalls;

        @Override
        public AuthResult login(String identifier, String password) {
            return AuthResult.invalidCredentials();
        }

        @Override
        public void logout() {
            logoutCalls++;
            if (logoutFailure != null) {
                throw logoutFailure;
            }
        }

        @Override
        public boolean isAuthenticated() {
            return false;
        }
    }
}
