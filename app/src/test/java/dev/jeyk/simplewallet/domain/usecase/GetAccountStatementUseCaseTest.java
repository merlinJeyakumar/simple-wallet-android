package dev.jeyk.simplewallet.domain.usecase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

import dev.jeyk.simplewallet.domain.model.AccountStatement;
import dev.jeyk.simplewallet.domain.model.TransactionType;
import dev.jeyk.simplewallet.domain.model.WalletAccount;
import dev.jeyk.simplewallet.domain.model.WalletTransaction;
import dev.jeyk.simplewallet.domain.repository.WalletRepository;

public final class GetAccountStatementUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-08-05T12:00:00Z");
    private static final WalletAccount ACCOUNT = new WalletAccount(
            "account-1", "Everyday Account", Currency.getInstance("USD"),
            new BigDecimal("100.00"), "\u2022\u2022\u2022\u2022 4821");

    @Test
    public void windowIsInclusiveAndResultsAreNewestFirstWithIdTieBreak() {
        WalletRepository repository = repositoryWith(List.of(
                transaction("older", NOW.minus(Duration.ofDays(30)).minusNanos(1)),
                transaction("boundary", NOW.minus(Duration.ofDays(30))),
                transaction("b", NOW.minus(Duration.ofDays(1))),
                transaction("future", NOW.plusNanos(1)),
                transaction("a", NOW.minus(Duration.ofDays(1))),
                transaction("now", NOW)
        ));
        GetAccountStatementUseCase useCase = new GetAccountStatementUseCase(
                repository, Clock.fixed(NOW, ZoneOffset.UTC));

        AccountStatement statement = useCase.execute(ACCOUNT.getId());

        assertEquals(ACCOUNT, statement.getAccount());
        assertEquals(List.of("now", "a", "b", "boundary"),
                statement.getTransactions().stream().map(WalletTransaction::getId).toList());
        assertThrows(UnsupportedOperationException.class,
                () -> statement.getTransactions().clear());
    }

    @Test
    public void missingAccountIsReportedBeforeTransactionLookup() {
        GetAccountStatementUseCase useCase = new GetAccountStatementUseCase(
                repositoryWith(List.of()), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThrows(NoSuchElementException.class, () -> useCase.execute("missing"));
    }

    private static WalletRepository repositoryWith(List<WalletTransaction> transactions) {
        return new WalletRepository() {
            @Override
            public List<WalletAccount> getAccounts() {
                return List.of(ACCOUNT);
            }

            @Override
            public List<WalletTransaction> getTransactions(String accountId) {
                return transactions;
            }
        };
    }

    private static WalletTransaction transaction(String id, Instant occurredAt) {
        return new WalletTransaction(id, ACCOUNT.getId(), occurredAt, "Test transaction",
                BigDecimal.ONE, TransactionType.CREDIT, new BigDecimal("100.00"));
    }
}
