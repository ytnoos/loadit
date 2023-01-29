package it.ytnoos.loadit;

public class Settings {

    private final int parallelism;

    public Settings(int parallelism) {
        this.parallelism = parallelism;
    }

    public int getParallelism() {
        return parallelism;
    }
}
