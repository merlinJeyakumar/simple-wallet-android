package dev.jeyk.simplewallet.di;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import dev.jeyk.simplewallet.data.datasource.AuthDataSource;
import dev.jeyk.simplewallet.data.datasource.FakeAuthDataSource;
import dev.jeyk.simplewallet.data.datasource.InMemoryWalletDataSource;
import dev.jeyk.simplewallet.data.datasource.WalletDataSource;
import dev.jeyk.simplewallet.data.repository.AuthRepositoryImpl;
import dev.jeyk.simplewallet.data.repository.WalletRepositoryImpl;
import dev.jeyk.simplewallet.data.session.EncryptedDataStoreSessionStore;
import dev.jeyk.simplewallet.data.session.SessionStore;
import dev.jeyk.simplewallet.domain.repository.AuthRepository;
import dev.jeyk.simplewallet.domain.repository.WalletRepository;
import dev.jeyk.simplewallet.presentation.common.FixedRequestDelay;
import dev.jeyk.simplewallet.presentation.common.RequestDelay;

@Module
@InstallIn(SingletonComponent.class)
public abstract class WalletModule {
    private static final Duration MOCK_REQUEST_DELAY = Duration.ofMillis(1_500L);

    @Binds
    @Singleton
    abstract SessionStore bindSessionStore(EncryptedDataStoreSessionStore implementation);

    @Binds
    @Singleton
    abstract AuthDataSource bindAuthDataSource(FakeAuthDataSource implementation);

    @Binds
    @Singleton
    abstract WalletDataSource bindWalletDataSource(InMemoryWalletDataSource implementation);

    @Binds
    @Singleton
    abstract AuthRepository bindAuthRepository(AuthRepositoryImpl implementation);

    @Binds
    @Singleton
    abstract WalletRepository bindWalletRepository(WalletRepositoryImpl implementation);

    @Provides
    @Singleton
    static Clock provideClock() {
        return Clock.systemUTC();
    }

    @Provides
    @Singleton
    static RequestDelay provideRequestDelay() {
        return new FixedRequestDelay(MOCK_REQUEST_DELAY);
    }

    @Provides
    @Singleton
    static ExecutorService provideExecutorService() {
        return new ThreadPoolExecutor(
                2,
                2,
                30L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(32),
                new WalletThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    private static final class WalletThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(
                    runnable,
                    "wallet-worker-" + counter.getAndIncrement()
            );
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }
}
