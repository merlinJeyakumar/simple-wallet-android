package dev.jeyk.simplewallet.domain.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public final class WalletAccount {
    private final String id;
    private final String name;
    private final Currency currency;
    private final BigDecimal balance;
    private final String maskedNumber;

    public WalletAccount(
            String id,
            String name,
            Currency currency,
            BigDecimal balance,
            String maskedNumber
    ) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.currency = Objects.requireNonNull(currency, "currency");
        this.balance = Objects.requireNonNull(balance, "balance");
        this.maskedNumber = requireText(maskedNumber, "maskedNumber");
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getMaskedNumber() {
        return maskedNumber;
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
        if (!(other instanceof WalletAccount)) {
            return false;
        }
        WalletAccount that = (WalletAccount) other;
        return id.equals(that.id)
                && name.equals(that.name)
                && currency.equals(that.currency)
                && balance.equals(that.balance)
                && maskedNumber.equals(that.maskedNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, currency, balance, maskedNumber);
    }

    @Override
    public String toString() {
        return "WalletAccount{" + "id='" + id + '\'' + ", currency=" + currency + '}';
    }
}
