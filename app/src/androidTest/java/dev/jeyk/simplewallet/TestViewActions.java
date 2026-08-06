package dev.jeyk.simplewallet;

import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;

import android.os.SystemClock;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.util.HumanReadables;
import androidx.test.espresso.util.TreeIterables;

import org.hamcrest.Matcher;

import java.util.concurrent.TimeoutException;

final class TestViewActions {
    private TestViewActions() {
    }

    static ViewAction waitUntil(Matcher<View> matcher, long timeoutMillis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "wait up to " + timeoutMillis + " ms for " + matcher;
            }

            @Override
            public void perform(UiController uiController, View root) {
                long deadline = SystemClock.uptimeMillis() + timeoutMillis;
                do {
                    for (View child : TreeIterables.breadthFirstViewTraversal(root)) {
                        if (matcher.matches(child)) {
                            return;
                        }
                    }
                    uiController.loopMainThreadForAtLeast(50L);
                } while (SystemClock.uptimeMillis() < deadline);

                throw new PerformException.Builder()
                        .withActionDescription(getDescription())
                        .withViewDescription(HumanReadables.describe(root))
                        .withCause(new TimeoutException())
                        .build();
            }
        };
    }

    static ViewAction waitForItemCount(int minimumCount, long timeoutMillis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(RecyclerView.class);
            }

            @Override
            public String getDescription() {
                return "wait for at least " + minimumCount + " RecyclerView items";
            }

            @Override
            public void perform(UiController uiController, View view) {
                RecyclerView recyclerView = (RecyclerView) view;
                long deadline = SystemClock.uptimeMillis() + timeoutMillis;
                do {
                    RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
                    if (adapter != null && adapter.getItemCount() >= minimumCount) {
                        return;
                    }
                    uiController.loopMainThreadForAtLeast(50L);
                } while (SystemClock.uptimeMillis() < deadline);

                throw new PerformException.Builder()
                        .withActionDescription(getDescription())
                        .withViewDescription(HumanReadables.describe(view))
                        .withCause(new TimeoutException())
                        .build();
            }
        };
    }

    static ViewAction clickItemAtPosition(int position) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(RecyclerView.class);
            }

            @Override
            public String getDescription() {
                return "click RecyclerView item at position " + position;
            }

            @Override
            public void perform(UiController uiController, View view) {
                RecyclerView recyclerView = (RecyclerView) view;
                recyclerView.scrollToPosition(position);
                uiController.loopMainThreadUntilIdle();

                RecyclerView.ViewHolder holder =
                        recyclerView.findViewHolderForAdapterPosition(position);
                if (holder == null) {
                    throw new PerformException.Builder()
                            .withActionDescription(getDescription())
                            .withViewDescription(HumanReadables.describe(view))
                            .withCause(new IllegalStateException("RecyclerView item is not attached"))
                            .build();
                }
                holder.itemView.performClick();
                uiController.loopMainThreadUntilIdle();
            }
        };
    }
}
