package dev.jeyk.simplewallet.presentation.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dev.jeyk.simplewallet.domain.auth.AuthResult;
import dev.jeyk.simplewallet.domain.auth.LoginValidationResult;
import dev.jeyk.simplewallet.domain.usecase.LoginUseCase;
import dev.jeyk.simplewallet.domain.usecase.ValidateLoginUseCase;
import dev.jeyk.simplewallet.presentation.common.RequestDelay;
import dev.jeyk.simplewallet.presentation.common.SingleEvent;

@HiltViewModel
public final class LoginViewModel extends ViewModel {
    private final ValidateLoginUseCase validateLoginUseCase;
    private final LoginUseCase loginUseCase;
    private final RequestDelay requestDelay;
    private final ExecutorService executorService;
    private final AtomicBoolean requestInFlight = new AtomicBoolean(false);
    private final MutableLiveData<LoginUiState> state =
            new MutableLiveData<>(LoginUiState.idle());
    private final MutableLiveData<SingleEvent<Boolean>> authenticationEvents =
            new MutableLiveData<>();

    @Inject
    public LoginViewModel(
            ValidateLoginUseCase validateLoginUseCase,
            LoginUseCase loginUseCase,
            RequestDelay requestDelay,
            ExecutorService executorService
    ) {
        this.validateLoginUseCase = Objects.requireNonNull(
                validateLoginUseCase,
                "validateLoginUseCase"
        );
        this.loginUseCase = Objects.requireNonNull(loginUseCase, "loginUseCase");
        this.requestDelay = Objects.requireNonNull(requestDelay, "requestDelay");
        this.executorService = Objects.requireNonNull(executorService, "executorService");
    }

    public LiveData<LoginUiState> getState() {
        return state;
    }

    public LiveData<SingleEvent<Boolean>> getAuthenticationEvents() {
        return authenticationEvents;
    }

    public void login(String identifier, String password) {
        if (requestInFlight.get()) {
            return;
        }

        LoginValidationResult validation = validateLoginUseCase.execute(identifier, password);
        if (!validation.isValid()) {
            state.setValue(LoginUiState.validation(
                    validation.getIdentifierError(),
                    validation.getPasswordError()
            ));
            return;
        }

        if (!requestInFlight.compareAndSet(false, true)) {
            return;
        }
        state.setValue(LoginUiState.loading());
        executorService.execute(() -> {
            try {
                requestDelay.await();
                AuthResult result = loginUseCase.execute(identifier, password);
                if (result.isSuccess()) {
                    state.postValue(LoginUiState.idle());
                    authenticationEvents.postValue(new SingleEvent<>(true));
                } else {
                    state.postValue(LoginUiState.failure(result.getMessage()));
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                state.postValue(LoginUiState.failure(null));
            } catch (RuntimeException exception) {
                state.postValue(LoginUiState.failure(null));
            } finally {
                requestInFlight.set(false);
            }
        });
    }
}
