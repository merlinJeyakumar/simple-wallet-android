package dev.jeyk.simplewallet.domain.usecase;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import dev.jeyk.simplewallet.domain.model.WalletAccount;
import dev.jeyk.simplewallet.domain.repository.WalletRepository;

public final class GetAccountsUseCase {
    private final WalletRepository walletRepository;

    @Inject
    public GetAccountsUseCase(WalletRepository walletRepository) {
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository");
    }

    public List<WalletAccount> execute() {
        return walletRepository.getAccounts();
    }
}
