package it.ytnoos.loadit.api;

import org.jspecify.annotations.Nullable;

/**
 * Represents the outcome of a data load or player setup operation.
 * Carries the result {@link Type} and an optional {@link Throwable} cause for error cases.
 */
public final class LoadResult {

    public static final LoadResult LOADED = new LoadResult(Type.LOADED);
    public static final LoadResult ALREADY_LOADED = new LoadResult(Type.ALREADY_LOADED);
    public static final LoadResult ALREADY_LOADING = new LoadResult(Type.ALREADY_LOADING);
    public static final LoadResult ERROR = new LoadResult(Type.ERROR);
    public static final LoadResult NOT_LOADED = new LoadResult(Type.NOT_LOADED);
    public static final LoadResult PRE_LOGIN_REALLOWED = new LoadResult(Type.PRE_LOGIN_REALLOWED);
    public static final LoadResult LOGIN_REALLOWED = new LoadResult(Type.LOGIN_REALLOWED);
    private final Type type;
    private final @Nullable Throwable cause;
    private LoadResult(Type type, @Nullable Throwable cause) {
        this.type = type;
        this.cause = cause;
    }

    private LoadResult(Type type) {
        this(type, null);
    }

    public static LoadResult error(@Nullable Throwable cause) {
        return new LoadResult(Type.ERROR, cause);
    }

    public Type type() {
        return type;
    }

    public @Nullable Throwable cause() {
        return cause;
    }

    public boolean isLoaded() {
        return type == Type.LOADED;
    }

    @Override
    public String toString() {
        if (cause != null) return type.name() + ": " + cause.getMessage();
        return type.name();
    }

    public enum Type {
        LOADED,
        ALREADY_LOADED,
        ALREADY_LOADING,
        ERROR,
        NOT_LOADED,
        PRE_LOGIN_REALLOWED,
        LOGIN_REALLOWED
    }
}