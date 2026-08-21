package dev.jeyk.simplewallet;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import dev.jeyk.simplewallet.domain.usecase.IsAuthenticatedUseCase;
import dev.jeyk.simplewallet.presentation.dashboard.DashboardFragment;
import dev.jeyk.simplewallet.presentation.login.LoginFragment;
import dev.jeyk.simplewallet.presentation.navigation.WalletNavigator;
import dev.jeyk.simplewallet.presentation.statement.StatementFragment;

@AndroidEntryPoint
public final class MainActivity extends AppCompatActivity implements WalletNavigator {
    private static final String TAG_LOGIN = "login";
    private static final String TAG_DASHBOARD = "dashboard";
    private static final String TAG_STATEMENT = "statement";
    @Nullable
    private Boolean pendingSessionAuthenticated;
    private boolean sessionRestoreFreshLaunch;
    @Inject
    ExecutorService executorService;
    @Inject
    IsAuthenticatedUseCase isAuthenticatedUseCase;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        applySystemBarInsets(findViewById(R.id.fragment_container));

        sessionRestoreFreshLaunch = savedInstanceState == null;
        restoreSession();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (pendingSessionAuthenticated != null) {
            boolean authenticated = pendingSessionAuthenticated;
            pendingSessionAuthenticated = null;
            finishSessionRestore(sessionRestoreFreshLaunch, authenticated);
        }
    }

    @Override
    public void showDashboard() {
        hideKeyboard();
        replaceRoot(new DashboardFragment(), TAG_DASHBOARD, false);
    }

    @Override
    public void showStatement(String accountId) {
        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                        R.id.fragment_container,
                        StatementFragment.newInstance(accountId),
                        TAG_STATEMENT
                )
                .addToBackStack(TAG_STATEMENT)
                .commit();
    }

    @Override
    public void showLoginAfterLogout() {
        replaceRoot(new LoginFragment(), TAG_LOGIN, false);
    }

    @Override
    public void goBack() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            getOnBackPressedDispatcher().onBackPressed();
        }
    }

    private void replaceRoot(Fragment fragment, String tag, boolean immediately) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        androidx.fragment.app.FragmentTransaction transaction = fragmentManager
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment, tag);
        if (immediately) {
            transaction.commitNow();
        } else {
            transaction.commit();
        }
    }

    private void restoreSession() {
        executorService.execute(() -> {
            boolean authenticated = false;
            try {
                authenticated = isAuthenticatedUseCase.execute();
            } catch (RuntimeException ignored) {
                // A session-storage failure must fail closed to the login screen.
            }
            boolean sessionAuthenticated = authenticated;
            runOnUiThread(() -> finishSessionRestore(
                    sessionRestoreFreshLaunch,
                    sessionAuthenticated
            ));
        });
    }

    private void finishSessionRestore(boolean freshLaunch, boolean authenticated) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        if (getSupportFragmentManager().isStateSaved()) {
            pendingSessionAuthenticated = authenticated;
            sessionRestoreFreshLaunch = freshLaunch;
            return;
        }
        Fragment current = getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);
        if (freshLaunch) {
            if (authenticated) {
                replaceRoot(new DashboardFragment(), TAG_DASHBOARD, true);
            } else {
                replaceRoot(new LoginFragment(), TAG_LOGIN, true);
            }
        } else if (!authenticated && !(current instanceof LoginFragment)) {
            replaceRoot(new LoginFragment(), TAG_LOGIN, true);
        }
        findViewById(R.id.startup_progress).setVisibility(View.GONE);
    }

    private void applySystemBarInsets(View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void hideKeyboard() {
        View focusedView = getCurrentFocus();
        if (focusedView == null) {
            return;
        }
        InputMethodManager inputMethodManager = getSystemService(InputMethodManager.class);
        inputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        focusedView.clearFocus();
    }
}
