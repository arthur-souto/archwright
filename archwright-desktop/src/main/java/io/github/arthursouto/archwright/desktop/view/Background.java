package io.github.arthursouto.archwright.desktop.view;

import javafx.concurrent.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Background {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(Background::daemon);

    private Background() {
    }

    public static <T> void run(Supplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return work.get();
            }
        };

        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> onFailure.accept(task.getException()));
        EXECUTOR.execute(task);
    }

    public static <T> void run(Supplier<T> work, Consumer<T> onSuccess) {
        run(work, onSuccess, failure -> {
            throw new IllegalStateException(failure);
        });
    }

    private static Thread daemon(Runnable runnable) {
        Thread thread = new Thread(runnable, "archwright-background");
        thread.setDaemon(true);

        return thread;
    }
}
