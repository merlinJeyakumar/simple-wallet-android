package dev.jeyk.simplewallet.data.datasource;

import java.util.List;

import dev.jeyk.simplewallet.domain.model.WalletAccount;
import dev.jeyk.simplewallet.domain.model.WalletTransaction;

public interface WalletDataSource {
    List<WalletAccount> getAccounts();

    List<WalletTransaction> getTransactions(String accountId);
}
