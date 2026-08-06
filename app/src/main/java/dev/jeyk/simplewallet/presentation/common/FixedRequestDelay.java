package dev.jeyk.simplewallet.presentation.common;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** A deterministic delay used to make local mock backend loading states observable. */
public final class FixedRequestDelay implements RequestDelay {
    private final long delayNanos;

    public FixedRequestDelay(Duration duration) {
        Duration requiredDuration = Objects.requireNonNull(duration, "duration");
        if (requiredDuration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative");
        }
        delayNanos = requiredDuration.toNanos();
    }

    @Override
    public void await() throws InterruptedException {
        TimeUnit.NANOSECONDS.sleep(delayNanos);
    }
}
