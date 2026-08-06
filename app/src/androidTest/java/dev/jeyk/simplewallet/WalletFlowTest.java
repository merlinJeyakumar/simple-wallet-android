package dev.jeyk.simplewallet;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.swipeDown;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static dev.jeyk.simplewallet.TestViewActions.clickItemAtPosition;
import static dev.jeyk.simplewallet.TestViewActions.waitForItemCount;
import static dev.jeyk.simplewallet.TestViewActions.waitUntil;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.textfield.TextInputLayout;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@RunWith(AndroidJUnit4.class)
public final class WalletFlowTest {
    private static final long ASYNC_TIMEOUT_MILLIS = 5_000L;

    @Rule
    public final ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void returnToLoggedOutStart() {
        onView(isRoot()).perform(waitUntil(
                anyOf(
                        allOf(withId(R.id.login_button), isDisplayed()),
                        allOf(withId(R.id.dashboard_logout_button), isDisplayed())),
                ASYNC_TIMEOUT_MILLIS));
        activityRule.getScenario().onActivity(activity -> {
            View logout = activity.findViewById(R.id.dashboard_logout_button);
            if (logout != null && logout.getVisibility() == View.VISIBLE) {
                logout.performClick();
            }
        });
        onView(isRoot()).perform(waitUntil(
                allOf(withId(R.id.login_button), isDisplayed()),
                ASYNC_TIMEOUT_MILLIS));
    }

    @Test
    public void emptyCredentialsShowInlineValidation() {
        onView(withId(R.id.login_identifier_input)).perform(clearText());
        onView(withId(R.id.login_password_input)).perform(clearText(), closeSoftKeyboard());
        onView(withId(R.id.login_button)).perform(click());

        onView(withId(R.id.login_identifier_layout)).check(matches(hasAnyError()));
        onView(withId(R.id.login_password_layout)).check(matches(hasAnyError()));
    }

    @Test
    public void invalidCredentialsShowGenericFailure() {
        submitCredentials("unknown@example.com", "incorrect-password");

        onView(isRoot()).perform(waitUntil(
                allOf(withId(R.id.login_error_text), isDisplayed()),
                ASYNC_TIMEOUT_MILLIS));
        onView(withId(R.id.login_error_text)).check(matches(not(withText(""))));
    }

    @Test
    public void authenticationServiceFailureIsRecoverable() {
        submitCredentials("error@example.com", "password123");

        onView(isRoot()).perform(waitUntil(
                allOf(withId(R.id.login_error_text), isDisplayed()),
                ASYNC_TIMEOUT_MILLIS));
        onView(withId(R.id.login_error_text)).check(matches(not(withText(""))));
        onView(withId(R.id.login_button)).check(matches(isEnabled()));
    }

    @Test
    public void validEmailShowsMultipleAccounts() {
        loginWithDemoEmail();

        onView(withId(R.id.dashboard_accounts_list))
                .perform(waitForItemCount(3, ASYNC_TIMEOUT_MILLIS));
        onView(withId(R.id.dashboard_accounts_list)).check(matches(isDisplayed()));
    }

    @Test
    public void usernameAliasCanAuthenticate() {
        submitCredentials("demo", "password123");

        waitForDashboard();
        onView(withId(R.id.dashboard_accounts_list))
                .perform(waitForItemCount(3, ASYNC_TIMEOUT_MILLIS));
    }

    @Test
    public void accountSelectionOpensStatementAndBackReturnsToDashboard() {
        loginWithDemoEmail();
        onView(withId(R.id.dashboard_accounts_list))
                .perform(waitForItemCount(1, ASYNC_TIMEOUT_MILLIS))
                .perform(clickItemAtPosition(0));

        onView(isRoot()).perform(waitUntil(
                allOf(withId(R.id.statement_transactions_list), isDisplayed()),
                ASYNC_TIMEOUT_MILLIS));
        onView(withId(R.id.statement_transactions_list))
                .perform(waitForItemCount(1, ASYNC_TIMEOUT_MILLIS));

        pressBack();
        waitForDashboard();
    }

    @Test
    public void authenticatedDashboardSurvivesRotation() {
        loginWithDemoEmail();
        onView(withId(R.id.dashboard_accounts_list))
                .perform(waitForItemCount(3, ASYNC_TIMEOUT_MILLIS));

        activityRule.getScenario().recreate();

        waitForDashboard();
        onView(withId(R.id.dashboard_accounts_list))
                .perform(waitForItemCount(3, ASYNC_TIMEOUT_MILLIS));
    }

    @Test
    public void encryptedSessionSurvivesActivityRelaunchWithoutPersistingCredentials()
            throws IOException {
        loginWithDemoEmail();
        onView(withId(R.id.dashboard_accounts_list))
                .perform(waitForItemCount(3, ASYNC_TIMEOUT_MILLIS));

        File dataStoreFile = new File(
                ApplicationProvider.getApplicationContext().getFilesDir(),
                "datastore/encrypted_session.preferences_pb"
        );
        assertTrue(dataStoreFile.isFile());
        String persistedBytes = new String(
                Files.readAllBytes(dataStoreFile.toPath()),
                StandardCharsets.UTF_8
        );
        assertFalse(persistedBytes.contains("password123"));
        assertFalse(persistedBytes.contains("demo@example.com"));
        assertFalse(persistedBytes.contains("simple-wallet-authenticated-v1"));

        activityRule.getScenario().close();
        try (ActivityScenario<MainActivity> ignored =
                     ActivityScenario.launch(MainActivity.class)) {
            waitForDashboard();
            onView(withId(R.id.dashboard_accounts_list))
                    .perform(waitForItemCount(3, ASYNC_TIMEOUT_MILLIS));
        }
    }

    @Test
    public void logoutClearsSessionAndReturnsToLogin() {
        loginWithDemoEmail();
        onView(withId(R.id.dashboard_logout_button)).perform(click());

        onView(isRoot()).perform(waitUntil(
                allOf(withId(R.id.login_button), isDisplayed()),
                ASYNC_TIMEOUT_MILLIS));
        onView(withId(R.id.login_identifier_input)).check(matches(withText("")));
        onView(withId(R.id.login_password_input)).check(matches(withText("")));

        activityRule.getScenario().recreate();
        onView(withId(R.id.login_button)).check(matches(isDisplayed()));

        activityRule.getScenario().close();
        try (ActivityScenario<MainActivity> ignored =
                     ActivityScenario.launch(MainActivity.class)) {
            onView(isRoot()).perform(waitUntil(
                    allOf(withId(R.id.login_button), isDisplayed()),
                    ASYNC_TIMEOUT_MILLIS));
        }
    }

    @Test
    public void mockBackendDelayKeepsEveryLoaderVisible() {
        long loginStartedAt = SystemClock.elapsedRealtime();
        submitCredentials("demo@example.com", "password123");
        onView(withId(R.id.login_progress))
                .check(matches(allOf(isDisplayed(), fillsParent(), hasBackgroundAlpha(255))));
        onView(withId(R.id.login_button)).check(matches(not(isEnabled())));

        onView(isRoot()).perform(waitUntil(
                allOf(withId(R.id.dashboard_progress_panel), isDisplayed()),
                ASYNC_TIMEOUT_MILLIS));
        assertTrue(SystemClock.elapsedRealtime() - loginStartedAt >= 1_400L);
        onView(withId(R.id.dashboard_progress_panel))
                .check(matches(allOf(isDisplayed(), fillsParent(), hasBackgroundAlpha(153))));
        onView(withId(R.id.dashboard_accounts_list))
                .perform(waitForItemCount(1, ASYNC_TIMEOUT_MILLIS))
                .perform(clickItemAtPosition(0));

        onView(withId(R.id.statement_progress_panel))
                .check(matches(allOf(isDisplayed(), fillsParent(), hasBackgroundAlpha(153))));
        onView(withId(R.id.statement_transactions_list))
                .perform(waitForItemCount(1, ASYNC_TIMEOUT_MILLIS));
        pressBack();
        waitForDashboard();

        onView(withId(R.id.dashboard_logout_button)).perform(click());
        onView(withId(R.id.dashboard_progress_panel)).check(matches(allOf(
                isDisplayed(),
                fillsParent(),
                hasBackgroundAlpha(153),
                withContentDescription(R.string.logout_loading)
        )));
        onView(isRoot()).perform(waitUntil(
                allOf(withId(R.id.login_button), isDisplayed()),
                ASYNC_TIMEOUT_MILLIS));
    }

    @Test
    public void swipeRefreshKeepsBothListsVisibleWithoutBlockingOverlay() {
        loginWithDemoEmail();
        onView(withId(R.id.dashboard_accounts_list))
                .perform(waitForItemCount(3, ASYNC_TIMEOUT_MILLIS));

        onView(withId(R.id.dashboard_swipe_refresh)).perform(swipeDown());
        onView(withId(R.id.dashboard_swipe_refresh))
                .check(matches(isSwipeRefreshing(true)));
        onView(withId(R.id.dashboard_progress_panel)).check(matches(not(isDisplayed())));
        onView(withId(R.id.dashboard_accounts_list))
                .check(matches(isDisplayed()))
                .perform(waitForItemCount(3, ASYNC_TIMEOUT_MILLIS));
        onView(isRoot()).perform(waitUntil(
                allOf(
                        withId(R.id.dashboard_swipe_refresh),
                        isSwipeRefreshing(false)
                ),
                ASYNC_TIMEOUT_MILLIS
        ));

        onView(withId(R.id.dashboard_accounts_list)).perform(clickItemAtPosition(0));
        onView(withId(R.id.statement_transactions_list))
                .perform(waitForItemCount(1, ASYNC_TIMEOUT_MILLIS));
        onView(withId(R.id.statement_swipe_refresh)).perform(swipeDown());
        onView(withId(R.id.statement_swipe_refresh))
                .check(matches(isSwipeRefreshing(true)));
        onView(withId(R.id.statement_progress_panel)).check(matches(not(isDisplayed())));
        onView(withId(R.id.statement_transactions_list))
                .check(matches(isDisplayed()))
                .perform(waitForItemCount(1, ASYNC_TIMEOUT_MILLIS));
        onView(isRoot()).perform(waitUntil(
                allOf(
                        withId(R.id.statement_swipe_refresh),
                        isSwipeRefreshing(false)
                ),
                ASYNC_TIMEOUT_MILLIS
        ));
    }

    private void loginWithDemoEmail() {
        submitCredentials("demo@example.com", "password123");
        waitForDashboard();
    }

    private void submitCredentials(String identifier, String password) {
        onView(withId(R.id.login_identifier_input))
                .perform(replaceText(identifier));
        onView(withId(R.id.login_password_input))
                .perform(replaceText(password), closeSoftKeyboard());
        onView(withId(R.id.login_button)).perform(click());
    }

    private void waitForDashboard() {
        onView(isRoot()).perform(waitUntil(
                allOf(withId(R.id.dashboard_accounts_list), isDisplayed()),
                ASYNC_TIMEOUT_MILLIS));
    }

    private static Matcher<View> hasAnyError() {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(View view) {
                if (!(view instanceof TextInputLayout)) {
                    return false;
                }
                CharSequence error = ((TextInputLayout) view).getError();
                return error != null && error.length() > 0;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("TextInputLayout with a non-empty error");
            }
        };
    }

    private static Matcher<View> fillsParent() {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(View view) {
                if (!(view.getParent() instanceof View)) {
                    return false;
                }
                View parent = (View) view.getParent();
                return view.getWidth() == parent.getWidth()
                        && view.getHeight() == parent.getHeight();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("view filling its parent");
            }
        };
    }

    private static Matcher<View> hasBackgroundAlpha(int expectedAlpha) {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(View view) {
                if (!(view.getBackground() instanceof ColorDrawable)) {
                    return false;
                }
                ColorDrawable background = (ColorDrawable) view.getBackground();
                return Color.alpha(background.getColor()) == expectedAlpha;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("view with background alpha ")
                        .appendValue(expectedAlpha);
            }
        };
    }

    private static Matcher<View> isSwipeRefreshing(boolean expected) {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(View view) {
                return view instanceof SwipeRefreshLayout
                        && ((SwipeRefreshLayout) view).isRefreshing() == expected;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("SwipeRefreshLayout refreshing=")
                        .appendValue(expected);
            }
        };
    }
}
