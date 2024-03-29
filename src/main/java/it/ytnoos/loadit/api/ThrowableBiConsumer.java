package it.ytnoos.loadit.api;

@FunctionalInterface
public interface ThrowableBiConsumer<T, U, E extends Exception> {

    void accept(T t, U u) throws E;
}