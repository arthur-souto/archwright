package io.github.arthursouto.archwright.desktop.view;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

final class Fx {

    private static final long TIMEOUT_SECONDS = 15;
    private static final long POLL_MILLIS = 50;
    private static final boolean AVAILABLE = start();

    private Fx() {
    }

    static boolean available() {
        return AVAILABLE;
    }

    static <T> T call(Supplier<T> action) {
        if (Platform.isFxApplicationThread()) {
            return action.get();
        }

        FutureTask<T> task = new FutureTask<>(action::get);
        Platform.runLater(task);

        try {
            return task.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    static void run(Runnable action) {
        call(() -> {
            action.run();
            return null;
        });
    }

    static void await(BooleanSupplier condition) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);

        while (System.nanoTime() < deadline) {
            if (call(condition::getAsBoolean)) {
                return;
            }

            pause();
        }

        throw new AssertionError("condition not met in " + TIMEOUT_SECONDS + "s");
    }

    private static void pause() {
        try {
            Thread.sleep(POLL_MILLIS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean start() {
        try {
            CountDownLatch started = new CountDownLatch(1);
            Platform.startup(started::countDown);
            Platform.setImplicitExit(false);

            return started.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (IllegalStateException alreadyRunning) {
            return true;
        } catch (Throwable unavailable) {
            return false;
        }
    }
}
