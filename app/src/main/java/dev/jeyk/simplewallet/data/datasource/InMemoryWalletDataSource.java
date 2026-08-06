package dev.jeyk.simplewallet.data.datasource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.jeyk.simplewallet.data.exception.DataSourceException;
import dev.jeyk.simplewallet.domain.model.TransactionType;
import dev.jeyk.simplewallet.domain.model.WalletAccount;
import dev.jeyk.simplewallet.domain.model.WalletTransaction;

public final class InMemoryWalletDataSource implements WalletDataSource {
    private static final String USD_ACCOUNT_ID = "everyday-usd";
    private static final String EUR_ACCOUNT_ID = "travel-eur";
    private static final String SGD_ACCOUNT_ID = "savings-sgd";

    private final List<WalletAccount> accounts;
    private final Map<String, List<WalletTransaction>> transactionsByAccount;

    public InMemoryWalletDataSource(Clock clock) {
        Instant now = Objects.requireNonNull(clock, "clock").instant();
        accounts = immutableList(
                account(USD_ACCOUNT_ID, "Everyday Account", "USD", "4820.45", "\u2022\u2022\u2022\u2022 4821"),
                account(EUR_ACCOUNT_ID, "Travel Account", "EUR", "1275.80", "\u2022\u2022\u2022\u2022 7310"),
                account(SGD_ACCOUNT_ID, "Savings Account", "SGD", "12340.00", "\u2022\u2022\u2022\u2022 9071")
        );
        Map<String, List<WalletTransaction>> fixtures = new LinkedHashMap<>();
        fixtures.put(USD_ACCOUNT_ID, immutableList(
                transaction("usd-salary", USD_ACCOUNT_ID, now.minus(Duration.ofHours(1)),
                        "Salary payment", "3200.00", TransactionType.CREDIT, "4820.45"),
                transaction("usd-grocery", USD_ACCOUNT_ID, now.minus(Duration.ofDays(2)),
                        "Grocery market", "-86.42", TransactionType.DEBIT, "1620.45"),
                transaction("usd-metro", USD_ACCOUNT_ID, now.minus(Duration.ofDays(5)),
                        "Metro transit", "-14.25", TransactionType.DEBIT, "1706.87"),
                transaction("usd-utility", USD_ACCOUNT_ID, now.minus(Duration.ofDays(30)),
                        "Electric utility", "-120.00", TransactionType.DEBIT, "1721.12"),
                transaction("usd-older", USD_ACCOUNT_ID, now.minus(Duration.ofDays(31)),
                        "Older statement item", "1000.00", TransactionType.CREDIT, "1841.12"),
                transaction("usd-future", USD_ACCOUNT_ID, now.plus(Duration.ofDays(1)),
                        "Future scheduled credit", "75.00", TransactionType.CREDIT, "4895.45")
        ));
        fixtures.put(EUR_ACCOUNT_ID, immutableList(
                transaction("eur-refund", EUR_ACCOUNT_ID, now.minus(Duration.ofDays(1)),
                        "Travel refund", "200.00", TransactionType.CREDIT, "1275.80"),
                transaction("eur-hotel", EUR_ACCOUNT_ID, now.minus(Duration.ofDays(5)),
                        "Hotel booking", "-250.00", TransactionType.DEBIT, "1075.80"),
                transaction("eur-exchange", EUR_ACCOUNT_ID, now.minus(Duration.ofDays(25)),
                        "Currency exchange", "1000.00", TransactionType.CREDIT, "1325.80")
        ));
        fixtures.put(SGD_ACCOUNT_ID, immutableList(
                transaction("sgd-interest", SGD_ACCOUNT_ID, now.minus(Duration.ofHours(3)),
                        "Savings interest", "40.00", TransactionType.CREDIT, "12340.00"),
                transaction("sgd-transfer", SGD_ACCOUNT_ID, now.minus(Duration.ofDays(3)),
                        "Transfer to wallet", "-200.00", TransactionType.DEBIT, "12300.00"),
                transaction("sgd-deposit", SGD_ACCOUNT_ID, now.minus(Duration.ofDays(20)),
                        "Savings deposit", "2500.00", TransactionType.CREDIT, "12500.00")
        ));
        transactionsByAccount = Collections.unmodifiableMap(new LinkedHashMap<>(fixtures));
    }

    @Override
    public List<WalletAccount> getAccounts() {
        return accounts;
    }

    @Override
    public List<WalletTransaction> getTransactions(String accountId) {
        List<WalletTransaction> transactions = transactionsByAccount.get(accountId);
        if (transactions == null) {
            throw new DataSourceException("Account not found: " + accountId);
        }
        return transactions;
    }

    private static WalletAccount account(String id, String name, String code, String balance,
            String maskedNumber) {
        return new WalletAccount(id, name, Currency.getInstance(code),
                new BigDecimal(balance), maskedNumber);
    }

    private static WalletTransaction transaction(String id, String accountId, Instant occurredAt,
            String description, String amount, TransactionType type, String balanceAfter) {
        return new WalletTransaction(id, accountId, occurredAt, description,
                new BigDecimal(amount), type, new BigDecimal(balanceAfter));
    }

    @SafeVarargs
    private static <T> List<T> immutableList(T... values) {
        return Collections.unmodifiableList(Arrays.asList(values));
    }
}
