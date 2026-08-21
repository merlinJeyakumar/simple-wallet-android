package dev.jeyk.simplewallet.data.repository;

import java.util.Objects;

import javax.inject.Inject;

import dev.jeyk.simplewallet.data.datasource.AuthDataSource;
import dev.jeyk.simplewallet.domain.auth.AuthResult;
import dev.jeyk.simplewallet.domain.repository.AuthRepository;

public final class AuthRepositoryImpl implements AuthRepository {
    private final AuthDataSource dataSource;

    @Inject
    public AuthRepositoryImpl(AuthDataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public AuthResult login(String identifier, String password) {
        return dataSource.login(identifier, password);
    }

    @Override
    public void logout() {
        dataSource.logout();
    }

    @Override
    public boolean isAuthenticated() {
        return dataSource.isAuthenticated();
    }
}
