package it.ytnoos.loadit.api;

import org.jspecify.annotations.Nullable;

/**
 * Expected load failure that should be exposed through {@link LoadResult#cause()}
 * without being logged as an internal server error.
 */
public class LoadFailureException extends RuntimeException {

    public LoadFailureException(String message) {
        this(message, null);
    }

    public LoadFailureException(String message, @Nullable Throwable cause) {
        super(message, cause, false, false);
    }
}
