package dev.jeyk.simplewallet.presentation.common;

/** Delay applied once before a user-visible mock backend operation. */
public interface RequestDelay {
    void await() throws InterruptedException;
}
