package dev.jeyk.simplewallet.presentation;

import dev.jeyk.simplewallet.presentation.common.RequestDelay;

/** Immediate test delay that records calls and can simulate interruption. */
public final class RecordingRequestDelay implements RequestDelay {
    private int awaitCalls;
    private boolean interrupt;

    public int getAwaitCalls() {
        return awaitCalls;
    }

    public void interruptOnAwait() {
        interrupt = true;
    }

    @Override
    public void await() throws InterruptedException {
        awaitCalls++;
        if (interrupt) {
            throw new InterruptedException("test interruption");
        }
    }
}
