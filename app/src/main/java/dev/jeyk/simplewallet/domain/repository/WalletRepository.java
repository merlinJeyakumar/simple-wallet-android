package dev.jeyk.simplewallet.domain.repository;

import java.util.List;

import dev.jeyk.simplewallet.domain.model.WalletAccount;
import dev.jeyk.simplewallet.domain.model.WalletTransaction;

public interface WalletRepository {
    List<WalletAccount> getAccounts();

    List<WalletTransaction> getTransactions(String accountId);
}
