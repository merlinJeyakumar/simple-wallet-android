package dev.jeyk.simplewallet.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class AccountStatement {
    private final WalletAccount account;
    private final List<WalletTransaction> transactions;

    public AccountStatement(WalletAccount account, List<WalletTransaction> transactions) {
        this.account = Objects.requireNonNull(account, "account");
        this.transactions = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(transactions, "transactions"))
        );
    }

    public WalletAccount getAccount() {
        return account;
    }

    public List<WalletTransaction> getTransactions() {
        return transactions;
    }
}
