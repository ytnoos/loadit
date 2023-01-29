package it.ytnoos.loadit;

public class SettingsBuilder {

    private int parallelism = 1;

    public SettingsBuilder setParallelism(int parallelism) {
        this.parallelism = parallelism;
        return this;
    }

    public Settings createSettings() {
        return new Settings(parallelism);
    }
}