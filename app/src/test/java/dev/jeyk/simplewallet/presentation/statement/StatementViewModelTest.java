package dev.jeyk.simplewallet.presentation.statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;

import dev.jeyk.simplewallet.domain.model.AccountStatement;
import dev.jeyk.simplewallet.domain.model.TransactionType;
import dev.jeyk.simplewallet.domain.model.WalletAccount;
import dev.jeyk.simplewallet.domain.model.WalletTransaction;
import dev.jeyk.simplewallet.domain.repository.WalletRepository;
import dev.jeyk.simplewallet.domain.usecase.GetAccountStatementUseCase;
import dev.jeyk.simplewallet.presentation.PausedExecutorService;
import dev.jeyk.simplewallet.presentation.RecordingRequestDelay;
import dev.jeyk.simplewallet.presentation.common.SingleEvent;
import dev.jeyk.simplewallet.presentation.common.UiState;

public final class StatementViewModelTest {
    private static final Instant NOW = Instant.parse("2026-08-05T12:00:00Z");
    private static final String ACCOUNT_ID = "checking-usd";

    @Rule
    public final InstantTaskExecutorRule instantTaskExecutorRule =
            new InstantTaskExecutorRule();

    private StubWalletRepository walletRepository;
    private PausedExecutorService executor;
    private RecordingRequestDelay requestDelay;
    private StatementViewModel viewModel;

    @Before
    public void setUp() {
        walletRepository = new StubWalletRepository();
        executor = new PausedExecutorService();
        requestDelay = new RecordingRequestDelay();
        viewModel = new StatementViewModel(
                ACCOUNT_ID,
                new GetAccountStatementUseCase(
                        walletRepository,
                        Clock.fixed(NOW, ZoneOffset.UTC)
                ),
                requestDelay,
                executor
        );
    }

    @Test
    public void statementTransitionsFromLoadingToSuccess() {
        WalletTransaction transaction = transaction("tx-1", NOW.minusSeconds(60));
        walletRepository.transactions = List.of(transaction);

        viewModel.loadStatement();
        assertEquals(UiState.Status.LOADING, requireState().getStatus());
        executor.runNext();

        assertEquals(1, requestDelay.getAwaitCalls());
        UiState<AccountStatement> state = requireState();
        assertEquals(UiState.Status.SUCCESS, state.getStatus());
        assertEquals(List.of(transaction), state.getData().getTransactions());
    }

    @Test
    public void noRecentTransactionsProducesExplicitEmptyState() {
        walletRepository.transactions = List.of();

        viewModel.loadStatement();
        executor.runNext();

        UiState<AccountStatement> state = requireState();
        assertEquals(UiState.Status.EMPTY, state.getStatus());
        assertNotNull(state.getData());
        assertTrue(state.getData().getTransactions().isEmpty());
    }

    @Test
    public void repositoryFailureProducesGenericErrorState() {
        walletRepository.failure = new IllegalStateException("private fixture detail");

        viewModel.loadStatement();
        executor.runNext();

        UiState<AccountStatement> state = requireState();
        assertEquals(UiState.Status.ERROR, state.getStatus());
        assertNull(state.getMessage());
    }

    @Test
    public void repeatedLoadWhilePendingQueuesOnlyOneRequest() {
        viewModel.loadStatement();
        viewModel.loadStatement();

        assertEquals(1, executor.queuedTaskCount());
        executor.runNext();
        assertEquals(1, walletRepository.accountsCalls);
        assertEquals(1, walletRepository.transactionCalls);
        assertEquals(1, requestDelay.getAwaitCalls());
    }

    @Test
    public void refreshPreservesVisibleStatementUntilSuccessThenReplacesIt() {
        WalletTransaction original = transaction("tx-original", NOW.minusSeconds(60));
        WalletTransaction replacement = transaction("tx-replacement", NOW.minusSeconds(30));
        walletRepository.transactions = List.of(original);
        viewModel.loadStatement();
        executor.runNext();

        walletRepository.transactions = List.of(replacement);
        viewModel.refreshStatement();

        assertEquals(Boolean.TRUE, viewModel.getRefreshing().getValue());
        assertEquals(List.of(original), requireState().getData().getTransactions());
        assertEquals(1, executor.queuedTaskCount());

        executor.runNext();

        assertEquals(Boolean.FALSE, viewModel.getRefreshing().getValue());
        assertEquals(UiState.Status.SUCCESS, requireState().getStatus());
        assertEquals(List.of(replacement), requireState().getData().getTransactions());
        assertEquals(2, walletRepository.accountsCalls);
        assertEquals(2, walletRepository.transactionCalls);
        assertEquals(2, requestDelay.getAwaitCalls());
    }

    @Test
    public void refreshCanReplaceVisibleStatementWithEmptyState() {
        walletRepository.transactions = List.of(
                transaction("tx-original", NOW.minusSeconds(60))
        );
        viewModel.loadStatement();
        executor.runNext();

        walletRepository.transactions = List.of();
        viewModel.refreshStatement();
        executor.runNext();

        assertEquals(UiState.Status.EMPTY, requireState().getStatus());
        assertTrue(requireState().getData().getTransactions().isEmpty());
        assertEquals(Boolean.FALSE, viewModel.getRefreshing().getValue());
    }

    @Test
    public void refreshFailurePreservesStatementAndEmitsSingleFailureEvent() {
        WalletTransaction original = transaction("tx-original", NOW.minusSeconds(60));
        walletRepository.transactions = List.of(original);
        viewModel.loadStatement();
        executor.runNext();

        walletRepository.failure = new IllegalStateException("private backend detail");
        viewModel.refreshStatement();
        executor.runNext();

        assertEquals(UiState.Status.SUCCESS, requireState().getStatus());
        assertEquals(List.of(original), requireState().getData().getTransactions());
        assertEquals(Boolean.FALSE, viewModel.getRefreshing().getValue());
        SingleEvent<Boolean> event = viewModel.getRefreshFailureEvents().getValue();
        assertNotNull(event);
        assertEquals(Boolean.TRUE, event.consume());
        assertNull(event.consume());
    }

    @Test
    public void repeatedRefreshWhilePendingQueuesOnlyOneRequest() {
        walletRepository.transactions = List.of(
                transaction("tx-original", NOW.minusSeconds(60))
        );
        viewModel.loadStatement();
        executor.runNext();

        viewModel.refreshStatement();
        viewModel.refreshStatement();

        assertEquals(Boolean.TRUE, viewModel.getRefreshing().getValue());
        assertEquals(1, executor.queuedTaskCount());
        executor.runNext();
        assertEquals(2, walletRepository.accountsCalls);
        assertEquals(2, walletRepository.transactionCalls);
        assertEquals(2, requestDelay.getAwaitCalls());
    }

    @Test
    public void interruptedRefreshPreservesStatementAndRestoresInterrupt() {
        WalletTransaction original = transaction("tx-original", NOW.minusSeconds(60));
        walletRepository.transactions = List.of(original);
        viewModel.loadStatement();
        executor.runNext();
        requestDelay.interruptOnAwait();

        viewModel.refreshStatement();
        executor.runNext();

        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
        assertEquals(UiState.Status.SUCCESS, requireState().getStatus());
        assertEquals(List.of(original), requireState().getData().getTransactions());
        assertEquals(Boolean.FALSE, viewModel.getRefreshing().getValue());
        assertEquals(1, walletRepository.accountsCalls);
        assertEquals(1, walletRepository.transactionCalls);
        assertNotNull(viewModel.getRefreshFailureEvents().getValue());
    }

    @Test
    public void interruptedDelayRestoresInterruptAndProducesGenericError() {
        requestDelay.interruptOnAwait();

        viewModel.loadStatement();
        executor.runNext();

        assertTrue(Thread.currentThread().isInterrupted());
        Thread.interrupted();
        assertEquals(UiState.Status.ERROR, requireState().getStatus());
        assertEquals(0, walletRepository.accountsCalls);
        assertEquals(0, walletRepository.transactionCalls);
    }

    private UiState<AccountStatement> requireState() {
        UiState<AccountStatement> state = viewModel.getState().getValue();
        assertNotNull(state);
        return state;
    }

    private static WalletTransaction transaction(String id, Instant instant) {
        return new WalletTransaction(
                id,
                ACCOUNT_ID,
                instant,
                "Test transaction",
                new BigDecimal("25.00"),
                TransactionType.CREDIT,
                new BigDecimal("100.00")
        );
    }

    private static final class StubWalletRepository implements WalletRepository {
        private final WalletAccount account = new WalletAccount(
                ACCOUNT_ID,
                "Everyday Checking",
                Currency.getInstance("USD"),
                new BigDecimal("100.00"),
                "**** 1000"
        );
        private List<WalletTransaction> transactions = List.of();
        private RuntimeException failure;
        private int accountsCalls;
        private int transactionCalls;

        @Override
        public List<WalletAccount> getAccounts() {
            accountsCalls++;
            if (failure != null) {
                throw failure;
            }
            return List.of(account);
        }

        @Override
        public List<WalletTransaction> getTransactions(String accountId) {
            transactionCalls++;
            if (failure != null) {
                throw failure;
            }
            return transactions;
        }
    }
}
