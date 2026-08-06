package dev.jeyk.simplewallet;

import android.content.Context;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import dev.jeyk.simplewallet.data.datasource.AuthDataSource;
import dev.jeyk.simplewallet.data.datasource.FakeAuthDataSource;
import dev.jeyk.simplewallet.data.datasource.InMemoryWalletDataSource;
import dev.jeyk.simplewallet.data.datasource.WalletDataSource;
import dev.jeyk.simplewallet.data.repository.AuthRepositoryImpl;
import dev.jeyk.simplewallet.data.repository.WalletRepositoryImpl;
import dev.jeyk.simplewallet.data.session.EncryptedDataStoreSessionStore;
import dev.jeyk.simplewallet.domain.repository.AuthRepository;
import dev.jeyk.simplewallet.domain.repository.WalletRepository;
import dev.jeyk.simplewallet.domain.usecase.GetAccountStatementUseCase;
import dev.jeyk.simplewallet.domain.usecase.GetAccountsUseCase;
import dev.jeyk.simplewallet.domain.usecase.IsAuthenticatedUseCase;
import dev.jeyk.simplewallet.domain.usecase.LoginUseCase;
import dev.jeyk.simplewallet.domain.usecase.LogoutUseCase;
import dev.jeyk.simplewallet.domain.usecase.ValidateLoginUseCase;
import dev.jeyk.simplewallet.presentation.common.FixedRequestDelay;
import dev.jeyk.simplewallet.presentation.common.RequestDelay;

public final class AppContainer implements AutoCloseable {
    private static final Duration MOCK_REQUEST_DELAY = Duration.ofMillis(1_500L);

    private final ValidateLoginUseCase validateLoginUseCase;
    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final IsAuthenticatedUseCase isAuthenticatedUseCase;
    private final GetAccountsUseCase getAccountsUseCase;
    private final GetAccountStatementUseCase getAccountStatementUseCase;
    private final ExecutorService executorService;
    private final RequestDelay requestDelay;

    public AppContainer() {
        this(Clock.systemUTC());
    }

    public AppContainer(Context context) {
        this(Clock.systemUTC(),
                new FakeAuthDataSource(new EncryptedDataStoreSessionStore(context)),
                new InMemoryWalletDataSource(Clock.systemUTC()),
                createExecutorService(),
                new FixedRequestDelay(MOCK_REQUEST_DELAY));
    }

    public AppContainer(Clock clock) {
        this(clock, new FakeAuthDataSource(), new InMemoryWalletDataSource(clock),
                createExecutorService(), new FixedRequestDelay(MOCK_REQUEST_DELAY));
    }

    public AppContainer(Clock clock, AuthDataSource authDataSource,
            WalletDataSource walletDataSource, ExecutorService executorService) {
        this(clock, authDataSource, walletDataSource, executorService,
                new FixedRequestDelay(MOCK_REQUEST_DELAY));
    }

    public AppContainer(Clock clock, AuthDataSource authDataSource,
            WalletDataSource walletDataSource, ExecutorService executorService,
            RequestDelay requestDelay) {
        Objects.requireNonNull(clock, "clock");
        AuthRepository authRepository = new AuthRepositoryImpl(
                Objects.requireNonNull(authDataSource, "authDataSource"));
        WalletRepository walletRepository = new WalletRepositoryImpl(
                Objects.requireNonNull(walletDataSource, "walletDataSource"));
        this.executorService = Objects.requireNonNull(executorService, "executorService");
        this.requestDelay = Objects.requireNonNull(requestDelay, "requestDelay");
        validateLoginUseCase = new ValidateLoginUseCase();
        loginUseCase = new LoginUseCase(authRepository);
        logoutUseCase = new LogoutUseCase(authRepository);
        isAuthenticatedUseCase = new IsAuthenticatedUseCase(authRepository);
        getAccountsUseCase = new GetAccountsUseCase(walletRepository);
        getAccountStatementUseCase = new GetAccountStatementUseCase(walletRepository, clock);
    }

    public ValidateLoginUseCase getValidateLoginUseCase() {
        return validateLoginUseCase;
    }

    public LoginUseCase getLoginUseCase() {
        return loginUseCase;
    }

    public LogoutUseCase getLogoutUseCase() {
        return logoutUseCase;
    }

    public IsAuthenticatedUseCase getIsAuthenticatedUseCase() {
        return isAuthenticatedUseCase;
    }

    public GetAccountsUseCase getGetAccountsUseCase() {
        return getAccountsUseCase;
    }

    public GetAccountStatementUseCase getGetAccountStatementUseCase() {
        return getAccountStatementUseCase;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public RequestDelay getRequestDelay() {
        return requestDelay;
    }

    @Override
    public void close() {
        executorService.shutdownNow();
    }

    private static ExecutorService createExecutorService() {
        return new ThreadPoolExecutor(2, 2, 30L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(32), new WalletThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    private static final class WalletThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "wallet-worker-" + counter.getAndIncrement());
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }
}
