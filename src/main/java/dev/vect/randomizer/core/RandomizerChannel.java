package dev.vect.randomizer.core;

public enum RandomizerChannel {
    BLOCKS("blocks"),
    MOBS("mobs"),
    RECIPES("recipes"),
    FISHING("fishing"),
    TRADES("trades");

    private final String key;

    RandomizerChannel(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}