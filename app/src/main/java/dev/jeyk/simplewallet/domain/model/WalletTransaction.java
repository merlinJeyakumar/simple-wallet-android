package dev.jeyk.simplewallet.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public final class WalletTransaction {
    private final String id;
    private final String accountId;
    private final Instant occurredAt;
    private final String description;
    private final BigDecimal amount;
    private final TransactionType type;
    private final BigDecimal balanceAfter;

    public WalletTransaction(
            String id,
            String accountId,
            Instant occurredAt,
            String description,
            BigDecimal amount,
            TransactionType type,
            BigDecimal balanceAfter
    ) {
        this.id = requireText(id, "id");
        this.accountId = requireText(accountId, "accountId");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.description = requireText(description, "description");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.type = Objects.requireNonNull(type, "type");
        this.balanceAfter = Objects.requireNonNull(balanceAfter, "balanceAfter");
    }

    public String getId() {
        return id;
    }

    public String getAccountId() {
        return accountId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof WalletTransaction)) {
            return false;
        }
        WalletTransaction that = (WalletTransaction) other;
        return id.equals(that.id)
                && accountId.equals(that.accountId)
                && occurredAt.equals(that.occurredAt)
                && description.equals(that.description)
                && amount.equals(that.amount)
                && type == that.type
                && balanceAfter.equals(that.balanceAfter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, accountId, occurredAt, description, amount, type, balanceAfter);
    }

    @Override
    public String toString() {
        return "WalletTransaction{" + "id='" + id + '\'' + ", occurredAt=" + occurredAt + '}';
    }
}
