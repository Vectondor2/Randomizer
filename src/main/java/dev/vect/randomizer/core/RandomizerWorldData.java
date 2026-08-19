package dev.vect.randomizer.core;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RandomizerWorldData extends SavedData {

    private static final String FILE_NAME =
            "universal_randomizer";

    private long masterSeed;

    private long blockSalt;
    private long mobSalt;
    private long recipeSalt;
    private long fishingSalt;
    private long tradeSalt;

    private final Set<ResourceLocation> discoveredBlocks =
            new HashSet<>();

    private final Set<ResourceLocation> discoveredMobs =
            new HashSet<>();

    private final Map<ResourceLocation, Integer> mobKills =
            new HashMap<>();

    private RandomizerWorldData(long masterSeed) {
        this.masterSeed = masterSeed;
    }

    /*
     * Вызывается только когда в мире ещё нет наших данных.
     *
     * Поэтому новый мир получает новый случайный seed,
     * а существующий загружает старый из NBT.
     */
    public static RandomizerWorldData create() {
        return new RandomizerWorldData(
                RandomSource.create().nextLong()
        );
    }

    public static RandomizerWorldData load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        long seed;

        if (tag.contains("MasterSeed", Tag.TAG_LONG)) {
            seed = tag.getLong("MasterSeed");
        } else {
            // Совместимость с мирами,
            // созданными до появления master seed.
            seed = RandomSource.create().nextLong();
        }

        RandomizerWorldData data =
                new RandomizerWorldData(seed);

        data.blockSalt =
                tag.getLong("BlockSalt");

        data.mobSalt =
                tag.getLong("MobSalt");

        data.recipeSalt =
                tag.getLong("RecipeSalt");

        data.fishingSalt =
                tag.getLong("FishingSalt");

        data.tradeSalt =
                tag.getLong("TradeSalt");

        readLocationSet(
                tag,
                "DiscoveredBlocks",
                data.discoveredBlocks
        );

        readLocationSet(
                tag,
                "DiscoveredMobs",
                data.discoveredMobs
        );

        ListTag killList = tag.getList(
                "MobKills",
                Tag.TAG_COMPOUND
        );

        for (int i = 0; i < killList.size(); i++) {
            CompoundTag entry =
                    killList.getCompound(i);

            ResourceLocation entityId =
                    ResourceLocation.tryParse(
                            entry.getString("Id")
                    );

            if (entityId == null) {
                continue;
            }

            int kills = entry.getInt("Kills");

            if (kills > 0) {
                data.mobKills.put(
                        entityId,
                        kills
                );
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        tag.putLong(
                "MasterSeed",
                masterSeed
        );

        tag.putLong(
                "BlockSalt",
                blockSalt
        );

        tag.putLong(
                "MobSalt",
                mobSalt
        );

        tag.putLong(
                "RecipeSalt",
                recipeSalt
        );

        tag.putLong(
                "FishingSalt",
                fishingSalt
        );

        tag.putLong(
                "TradeSalt",
                tradeSalt
        );

        writeLocationSet(
                tag,
                "DiscoveredBlocks",
                discoveredBlocks
        );

        writeLocationSet(
                tag,
                "DiscoveredMobs",
                discoveredMobs
        );

        ListTag killList = new ListTag();

        mobKills.entrySet()
                .stream()
                .sorted(
                        Map.Entry.comparingByKey(
                                Comparator.comparing(
                                        ResourceLocation::toString
                                )
                        )
                )
                .forEach(entry -> {
                    CompoundTag killTag =
                            new CompoundTag();

                    killTag.putString(
                            "Id",
                            entry.getKey().toString()
                    );

                    killTag.putInt(
                            "Kills",
                            entry.getValue()
                    );

                    killList.add(killTag);
                });

        tag.put(
                "MobKills",
                killList
        );

        return tag;
    }

    public static RandomizerWorldData get(
            ServerLevel level
    ) {
        ServerLevel overworld =
                level.getServer().overworld();

        return overworld
                .getDataStorage()
                .computeIfAbsent(
                        new SavedData.Factory<>(
                                RandomizerWorldData::create,
                                RandomizerWorldData::load
                        ),
                        FILE_NAME
                );
    }

    public long getMasterSeed() {
        return masterSeed;
    }

    public long getSeed(
            RandomizerChannel channel
    ) {
        return RandomizerHash.deriveSeed(
                masterSeed,
                getSalt(channel),
                channel
        );
    }

    public long getSalt(
            RandomizerChannel channel
    ) {
        return switch (channel) {
            case BLOCKS -> blockSalt;
            case MOBS -> mobSalt;
            case RECIPES -> recipeSalt;
            case FISHING -> fishingSalt;
            case TRADES -> tradeSalt;
        };
    }

    public boolean discoverBlock(
            ResourceLocation blockId
    ) {
        boolean changed =
                discoveredBlocks.add(blockId);

        if (changed) {
            setDirty();
        }

        return changed;
    }

    public boolean isBlockDiscovered(
            ResourceLocation blockId
    ) {
        return discoveredBlocks.contains(
                blockId
        );
    }

    public Set<ResourceLocation>
    getDiscoveredBlocks() {
        return Set.copyOf(discoveredBlocks);
    }

    public boolean discoverMob(
            ResourceLocation entityId
    ) {
        boolean changed =
                discoveredMobs.add(entityId);

        if (changed) {
            setDirty();
        }

        return changed;
    }

    public boolean isMobDiscovered(
            ResourceLocation entityId
    ) {
        return discoveredMobs.contains(
                entityId
        );
    }

    public Set<ResourceLocation>
    getDiscoveredMobs() {
        return Set.copyOf(discoveredMobs);
    }

    public int addMobKill(
            ResourceLocation entityId
    ) {
        int newValue =
                mobKills.merge(
                        entityId,
                        1,
                        Integer::sum
                );

        setDirty();

        return newValue;
    }

    public int getMobKills(
            ResourceLocation entityId
    ) {
        return mobKills.getOrDefault(
                entityId,
                0
        );
    }

    public Map<ResourceLocation, Integer>
    getMobKills() {
        return Map.copyOf(mobKills);
    }

    public void reroll(
            RandomizerChannel channel
    ) {
        long newSalt =
                RandomSource.create().nextLong();

        switch (channel) {
            case BLOCKS -> {
                blockSalt = newSalt;
                discoveredBlocks.clear();
            }

            case MOBS -> {
                mobSalt = newSalt;
                discoveredMobs.clear();
                mobKills.clear();
            }

            case RECIPES ->
                    recipeSalt = newSalt;

            case FISHING ->
                    fishingSalt = newSalt;

            case TRADES ->
                    tradeSalt = newSalt;
        }

        setDirty();
    }

    public void rerollAll() {
        blockSalt =
                RandomSource.create().nextLong();

        mobSalt =
                RandomSource.create().nextLong();

        recipeSalt =
                RandomSource.create().nextLong();

        fishingSalt =
                RandomSource.create().nextLong();

        tradeSalt =
                RandomSource.create().nextLong();

        discoveredBlocks.clear();
        discoveredMobs.clear();
        mobKills.clear();

        setDirty();
    }

    public void setMasterSeed(long masterSeed) {
        this.masterSeed = masterSeed;

        blockSalt = 0L;
        mobSalt = 0L;
        recipeSalt = 0L;
        fishingSalt = 0L;
        tradeSalt = 0L;

        discoveredBlocks.clear();
        discoveredMobs.clear();
        mobKills.clear();

        setDirty();
    }

    private static void readLocationSet(
            CompoundTag tag,
            String key,
            Set<ResourceLocation> destination
    ) {
        ListTag list =
                tag.getList(
                        key,
                        Tag.TAG_STRING
                );

        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id =
                    ResourceLocation.tryParse(
                            list.getString(i)
                    );

            if (id != null) {
                destination.add(id);
            }
        }
    }

    private static void writeLocationSet(
            CompoundTag tag,
            String key,
            Set<ResourceLocation> values
    ) {
        ListTag list = new ListTag();

        values.stream()
                .map(ResourceLocation::toString)
                .sorted()
                .forEach(
                        id -> list.add(
                                StringTag.valueOf(id)
                        )
                );

        tag.put(
                key,
                list
        );
    }
}