package dev.jeyk.simplewallet.domain.usecase;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dev.jeyk.simplewallet.domain.model.AccountStatement;
import dev.jeyk.simplewallet.domain.model.WalletAccount;
import dev.jeyk.simplewallet.domain.model.WalletTransaction;
import dev.jeyk.simplewallet.domain.repository.WalletRepository;

public final class GetAccountStatementUseCase {
    private static final Duration STATEMENT_WINDOW = Duration.ofDays(30);

    private final WalletRepository walletRepository;
    private final Clock clock;

    @Inject
    public GetAccountStatementUseCase(WalletRepository walletRepository, Clock clock) {
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public AccountStatement execute(String accountId) {
        Objects.requireNonNull(accountId, "accountId");
        Instant now = clock.instant();
        Instant start = now.minus(STATEMENT_WINDOW);
        WalletAccount account = walletRepository.getAccounts().stream()
                .filter(candidate -> candidate.getId().equals(accountId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Account not found: " + accountId));
        List<WalletTransaction> transactions = walletRepository.getTransactions(accountId).stream()
                .filter(transaction -> !transaction.getOccurredAt().isBefore(start))
                .filter(transaction -> !transaction.getOccurredAt().isAfter(now))
                .sorted(Comparator.comparing(WalletTransaction::getOccurredAt)
                        .reversed()
                        .thenComparing(WalletTransaction::getId))
                .collect(Collectors.toList());
        return new AccountStatement(account, transactions);
    }
}
