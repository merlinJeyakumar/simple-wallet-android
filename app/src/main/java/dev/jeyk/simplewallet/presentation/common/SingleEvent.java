package dev.jeyk.simplewallet.presentation.common;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SingleEvent<T> {
    private final T value;
    private final AtomicBoolean consumed = new AtomicBoolean(false);

    public SingleEvent(T value) {
        this.value = value;
    }

    public T consume() {
        return consumed.compareAndSet(false, true) ? value : null;
    }
}
