package dev.jeyk.simplewallet.data.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.Test;

import dev.jeyk.simplewallet.data.exception.DataSourceException;
import dev.jeyk.simplewallet.domain.model.WalletAccount;
import dev.jeyk.simplewallet.domain.model.WalletTransaction;

public final class InMemoryWalletDataSourceTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-05T12:00:00Z"), ZoneOffset.UTC);

    @Test
    public void accountsAreDataDrivenAndMatchApprovedOrder() {
        InMemoryWalletDataSource source = new InMemoryWalletDataSource(CLOCK);
        List<WalletAccount> accounts = source.getAccounts();

        assertEquals(3, accounts.size());
        assertAccount(accounts.get(0), "Everyday Account", "USD", "4820.45", "4821");
        assertAccount(accounts.get(1), "Travel Account", "EUR", "1275.80", "7310");
        assertAccount(accounts.get(2), "Savings Account", "SGD", "12340.00", "9071");
        assertThrows(UnsupportedOperationException.class, () -> accounts.clear());
    }

    @Test
    public void eachAccountReturnsOnlyItsOwnTransactions() {
        InMemoryWalletDataSource source = new InMemoryWalletDataSource(CLOCK);

        for (WalletAccount account : source.getAccounts()) {
            List<WalletTransaction> transactions = source.getTransactions(account.getId());
            assertFalse(transactions.isEmpty());
            for (WalletTransaction transaction : transactions) {
                assertEquals(account.getId(), transaction.getAccountId());
            }
        }
        assertThrows(DataSourceException.class, () -> source.getTransactions("missing"));
    }

    private static void assertAccount(WalletAccount account, String name, String currency,
            String balance, String ending) {
        assertEquals(name, account.getName());
        assertEquals(currency, account.getCurrency().getCurrencyCode());
        assertEquals(new BigDecimal(balance), account.getBalance());
        assertTrueEnding(account.getMaskedNumber(), ending);
    }

    private static void assertTrueEnding(String maskedNumber, String ending) {
        if (!maskedNumber.endsWith(ending)) {
            throw new AssertionError(maskedNumber + " did not end with " + ending);
        }
    }
}
