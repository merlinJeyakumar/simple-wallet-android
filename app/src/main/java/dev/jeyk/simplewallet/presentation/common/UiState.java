package dev.jeyk.simplewallet.presentation.common;

public final class UiState<T> {
    public enum Status {
        IDLE,
        LOADING,
        SUCCESS,
        EMPTY,
        ERROR
    }

    private final Status status;
    private final T data;
    private final String message;

    private UiState(Status status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> UiState<T> idle() {
        return new UiState<>(Status.IDLE, null, null);
    }

    public static <T> UiState<T> loading() {
        return new UiState<>(Status.LOADING, null, null);
    }

    public static <T> UiState<T> success(T data) {
        return new UiState<>(Status.SUCCESS, data, null);
    }

    public static <T> UiState<T> empty(T data) {
        return new UiState<>(Status.EMPTY, data, null);
    }

    public static <T> UiState<T> error(String message) {
        return new UiState<>(Status.ERROR, null, message);
    }

    public Status getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }
}
