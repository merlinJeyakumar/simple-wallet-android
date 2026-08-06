package dev.jeyk.simplewallet.presentation.statement;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import dev.jeyk.simplewallet.domain.model.AccountStatement;
import dev.jeyk.simplewallet.domain.usecase.GetAccountStatementUseCase;
import dev.jeyk.simplewallet.presentation.common.RequestDelay;
import dev.jeyk.simplewallet.presentation.common.SingleEvent;
import dev.jeyk.simplewallet.presentation.common.UiState;

public final class StatementViewModel extends ViewModel {
    private final String accountId;
    private final GetAccountStatementUseCase getAccountStatementUseCase;
    private final RequestDelay requestDelay;
    private final ExecutorService executorService;
    private final AtomicBoolean requestInFlight = new AtomicBoolean(false);
    private final MutableLiveData<UiState<AccountStatement>> state =
            new MutableLiveData<>(UiState.idle());
    private final MutableLiveData<Boolean> refreshing = new MutableLiveData<>(false);
    private final MutableLiveData<SingleEvent<Boolean>> refreshFailureEvents =
            new MutableLiveData<>();

    public StatementViewModel(
            String accountId,
            GetAccountStatementUseCase getAccountStatementUseCase,
            RequestDelay requestDelay,
            ExecutorService executorService
    ) {
        this.accountId = Objects.requireNonNull(accountId, "accountId");
        this.getAccountStatementUseCase = Objects.requireNonNull(
                getAccountStatementUseCase,
                "getAccountStatementUseCase"
        );
        this.requestDelay = Objects.requireNonNull(requestDelay, "requestDelay");
        this.executorService = Objects.requireNonNull(executorService, "executorService");
    }

    public LiveData<UiState<AccountStatement>> getState() {
        return state;
    }

    public LiveData<Boolean> getRefreshing() {
        return refreshing;
    }

    public LiveData<SingleEvent<Boolean>> getRefreshFailureEvents() {
        return refreshFailureEvents;
    }

    public void loadStatement() {
        requestStatement(false);
    }

    public void refreshStatement() {
        requestStatement(true);
    }

    private void requestStatement(boolean refreshRequest) {
        if (!requestInFlight.compareAndSet(false, true)) {
            if (refreshRequest && !Boolean.TRUE.equals(refreshing.getValue())) {
                refreshing.setValue(false);
            }
            return;
        }
        if (refreshRequest) {
            refreshing.setValue(true);
        } else {
            state.setValue(UiState.loading());
        }
        executorService.execute(() -> {
            try {
                requestDelay.await();
                AccountStatement statement = getAccountStatementUseCase.execute(accountId);
                if (statement.getTransactions().isEmpty()) {
                    state.postValue(UiState.empty(statement));
                } else {
                    state.postValue(UiState.success(statement));
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                postStatementFailure(refreshRequest);
            } catch (RuntimeException exception) {
                postStatementFailure(refreshRequest);
            } finally {
                requestInFlight.set(false);
                if (refreshRequest) {
                    refreshing.postValue(false);
                }
            }
        });
    }

    private void postStatementFailure(boolean refreshRequest) {
        if (refreshRequest) {
            refreshFailureEvents.postValue(new SingleEvent<>(true));
        } else {
            state.postValue(UiState.error(null));
        }
    }
}
