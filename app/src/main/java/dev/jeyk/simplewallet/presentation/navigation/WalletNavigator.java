package dev.jeyk.simplewallet.presentation.navigation;

public interface WalletNavigator {
    void showDashboard();

    void showStatement(String accountId);

    void showLoginAfterLogout();

    void goBack();
}
