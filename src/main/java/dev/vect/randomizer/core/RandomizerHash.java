package dev.vect.randomizer.core;

import net.minecraft.resources.ResourceLocation;

public final class RandomizerHash {

    private static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;

    private RandomizerHash() {
    }

    public static long hashString(long seed, String value) {
        long hash = FNV_OFFSET_BASIS ^ seed;

        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash *= FNV_PRIME;
        }

        return mix64(hash);
    }

    public static long deriveSeed(
            long masterSeed,
            long salt,
            RandomizerChannel channel
    ) {
        long channelHash = hashString(
                masterSeed,
                channel.key()
        );

        return mix64(channelHash ^ salt);
    }

    public static int index(
            long seed,
            ResourceLocation id,
            int size
    ) {
        return index(seed, id.toString(), size);
    }

    public static int index(
            long seed,
            String id,
            int size
    ) {
        if (size <= 0) {
            throw new IllegalArgumentException(
                    "Randomizer pool cannot be empty"
            );
        }

        long hash = hashString(seed, id);

        return (int) Math.floorMod(
                hash,
                (long) size
        );
    }

    public static long mix64(long value) {
        value ^= value >>> 30;
        value *= 0xbf58476d1ce4e5b9L;

        value ^= value >>> 27;
        value *= 0x94d049bb133111ebL;

        value ^= value >>> 31;

        return value;
    }
}