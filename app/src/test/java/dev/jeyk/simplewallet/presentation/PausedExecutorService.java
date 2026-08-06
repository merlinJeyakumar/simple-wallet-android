package dev.jeyk.simplewallet.presentation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

/** Deterministic executor used to assert state before and after background work. */
public final class PausedExecutorService extends AbstractExecutorService {
    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private boolean shutdown;

    public int queuedTaskCount() {
        return tasks.size();
    }

    public void runNext() {
        Runnable task = tasks.remove();
        task.run();
    }

    @Override
    public void shutdown() {
        shutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown = true;
        List<Runnable> remaining = new ArrayList<>(tasks);
        tasks.clear();
        return remaining;
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isTerminated() {
        return shutdown && tasks.isEmpty();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return isTerminated();
    }

    @Override
    public void execute(Runnable command) {
        if (shutdown) {
            throw new IllegalStateException("executor is shut down");
        }
        tasks.add(command);
    }
}
