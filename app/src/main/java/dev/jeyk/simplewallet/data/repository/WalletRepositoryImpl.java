package dev.jeyk.simplewallet.data.repository;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import dev.jeyk.simplewallet.data.datasource.WalletDataSource;
import dev.jeyk.simplewallet.domain.model.WalletAccount;
import dev.jeyk.simplewallet.domain.model.WalletTransaction;
import dev.jeyk.simplewallet.domain.repository.WalletRepository;

public final class WalletRepositoryImpl implements WalletRepository {
    private final WalletDataSource dataSource;

    @Inject
    public WalletRepositoryImpl(WalletDataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public List<WalletAccount> getAccounts() {
        return dataSource.getAccounts();
    }

    @Override
    public List<WalletTransaction> getTransactions(String accountId) {
        return dataSource.getTransactions(accountId);
    }
}
