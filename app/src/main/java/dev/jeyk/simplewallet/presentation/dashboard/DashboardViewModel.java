package dev.jeyk.simplewallet.presentation.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dev.jeyk.simplewallet.domain.model.WalletAccount;
import dev.jeyk.simplewallet.domain.usecase.GetAccountsUseCase;
import dev.jeyk.simplewallet.domain.usecase.LogoutUseCase;
import dev.jeyk.simplewallet.presentation.common.RequestDelay;
import dev.jeyk.simplewallet.presentation.common.SingleEvent;
import dev.jeyk.simplewallet.presentation.common.UiState;

@HiltViewModel
public final class DashboardViewModel extends ViewModel {
    private final GetAccountsUseCase getAccountsUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RequestDelay requestDelay;
    private final ExecutorService executorService;
    private final AtomicBoolean accountsRequestInFlight = new AtomicBoolean(false);
    private final AtomicBoolean logoutRequestInFlight = new AtomicBoolean(false);
    private final MutableLiveData<UiState<List<WalletAccount>>> accountsState =
            new MutableLiveData<>(UiState.idle());
    private final MutableLiveData<Boolean> accountsRefreshing = new MutableLiveData<>(false);
    private final MutableLiveData<SingleEvent<Boolean>> accountsRefreshFailureEvents =
            new MutableLiveData<>();
    private final MutableLiveData<Boolean> logoutLoading = new MutableLiveData<>(false);
    private final MutableLiveData<SingleEvent<Boolean>> logoutEvents = new MutableLiveData<>();

    @Inject
    public DashboardViewModel(
            GetAccountsUseCase getAccountsUseCase,
            LogoutUseCase logoutUseCase,
            RequestDelay requestDelay,
            ExecutorService executorService
    ) {
        this.getAccountsUseCase = Objects.requireNonNull(
                getAccountsUseCase,
                "getAccountsUseCase"
        );
        this.logoutUseCase = Objects.requireNonNull(logoutUseCase, "logoutUseCase");
        this.requestDelay = Objects.requireNonNull(requestDelay, "requestDelay");
        this.executorService = Objects.requireNonNull(executorService, "executorService");
    }

    public LiveData<UiState<List<WalletAccount>>> getAccountsState() {
        return accountsState;
    }

    public LiveData<Boolean> getAccountsRefreshing() {
        return accountsRefreshing;
    }

    public LiveData<SingleEvent<Boolean>> getAccountsRefreshFailureEvents() {
        return accountsRefreshFailureEvents;
    }

    public LiveData<Boolean> getLogoutLoading() {
        return logoutLoading;
    }

    public LiveData<SingleEvent<Boolean>> getLogoutEvents() {
        return logoutEvents;
    }

    public void loadAccounts() {
        requestAccounts(false);
    }

    public void refreshAccounts() {
        requestAccounts(true);
    }

    private void requestAccounts(boolean refreshRequest) {
        if (!accountsRequestInFlight.compareAndSet(false, true)) {
            if (refreshRequest && !Boolean.TRUE.equals(accountsRefreshing.getValue())) {
                accountsRefreshing.setValue(false);
            }
            return;
        }
        if (refreshRequest) {
            accountsRefreshing.setValue(true);
        } else {
            accountsState.setValue(UiState.loading());
        }
        executorService.execute(() -> {
            try {
                requestDelay.await();
                List<WalletAccount> accounts = getAccountsUseCase.execute();
                if (accounts.isEmpty()) {
                    accountsState.postValue(UiState.empty(accounts));
                } else {
                    accountsState.postValue(UiState.success(accounts));
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                postAccountsFailure(refreshRequest);
            } catch (RuntimeException exception) {
                postAccountsFailure(refreshRequest);
            } finally {
                accountsRequestInFlight.set(false);
                if (refreshRequest) {
                    accountsRefreshing.postValue(false);
                }
            }
        });
    }

    private void postAccountsFailure(boolean refreshRequest) {
        if (refreshRequest) {
            accountsRefreshFailureEvents.postValue(new SingleEvent<>(true));
        } else {
            accountsState.postValue(UiState.error(null));
        }
    }

    public void logout() {
        if (!logoutRequestInFlight.compareAndSet(false, true)) {
            return;
        }
        logoutLoading.setValue(true);
        executorService.execute(() -> {
            boolean success = false;
            try {
                requestDelay.await();
                logoutUseCase.execute();
                success = true;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException ignored) {
                // Presentation exposes a generic, non-sensitive error message.
            } finally {
                logoutRequestInFlight.set(false);
                logoutLoading.postValue(false);
                logoutEvents.postValue(new SingleEvent<>(success));
            }
        });
    }
}
