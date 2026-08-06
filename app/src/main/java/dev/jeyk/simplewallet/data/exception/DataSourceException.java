package dev.jeyk.simplewallet.data.exception;

public final class DataSourceException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DataSourceException(String message) {
        super(message);
    }

    public DataSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
