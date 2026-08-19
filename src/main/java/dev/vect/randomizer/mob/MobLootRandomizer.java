package dev.vect.randomizer.mob;

import dev.vect.randomizer.Randomizer;
import dev.vect.randomizer.config.RandomizerBlacklist;
import dev.vect.randomizer.core.RandomizerChannel;
import dev.vect.randomizer.core.RandomizerHash;
import dev.vect.randomizer.core.RandomizerPools;
import dev.vect.randomizer.core.RandomizerWorldData;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MobLootRandomizer {

    /*
     * Кэшируем permutation до тех пор,
     * пока seed мобов не изменится.
     */
    private static long cachedSeed =
            Long.MIN_VALUE;

    private static Map<
            EntityType<?>,
            EntityType<?>
            > cachedMapping =
            Map.of();

    private MobLootRandomizer() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(
                MobLootRandomizer::onLivingDrops
        );
    }

    private static void onLivingDrops(
            LivingDropsEvent event
    ) {
        /*
         * Игроки и остальные LivingEntity,
         * которые не являются Mob,
         * не участвуют.
         */
        if (!(event.getEntity()
                instanceof Mob mob)) {

            return;
        }

        if (!(mob.level()
                instanceof ServerLevel level)) {

            return;
        }

        ResourceLocation sourceId =
                BuiltInRegistries.ENTITY_TYPE
                        .getKey(
                                mob.getType()
                        );

        if (sourceId == null) {
            return;
        }

        if (RandomizerBlacklist
                .isEntityBlocked(sourceId)) {

            return;
        }

        RandomizerWorldData data =
                RandomizerWorldData.get(
                        level
                );

        EntityType<?> donorType =
                getMappedType(
                        data,
                        mob.getType()
                );

        if (donorType == null) {
            return;
        }

        ResourceLocation donorId =
                BuiltInRegistries.ENTITY_TYPE
                        .getKey(donorType);

        if (donorId == null) {
            return;
        }

        /*
         * Если kill credit принадлежит игроку,
         * открываем запись в бестиарии.
         */
        Player killingPlayer =
                mob.getKillCredit()
                        instanceof Player player
                        ? player
                        : null;

        try {
            LootParams.Builder builder =
                    new LootParams.Builder(level)
                            .withParameter(
                                    LootContextParams.THIS_ENTITY,
                                    mob
                            )
                            .withParameter(
                                    LootContextParams.ORIGIN,
                                    mob.position()
                            )
                            .withParameter(
                                    LootContextParams.DAMAGE_SOURCE,
                                    event.getSource()
                            )
                            .withOptionalParameter(
                                    LootContextParams.ATTACKING_ENTITY,
                                    event.getSource()
                                            .getEntity()
                            )
                            .withOptionalParameter(
                                    LootContextParams.DIRECT_ATTACKING_ENTITY,
                                    event.getSource()
                                            .getDirectEntity()
                            )
                            .withOptionalParameter(
                                    LootContextParams.LAST_DAMAGE_PLAYER,
                                    killingPlayer
                            );

            LootParams params =
                    builder.create(
                            LootContextParamSets.ENTITY
                    );

            LootTable donorLootTable =
                    level.getServer()
                            .reloadableRegistries()
                            .getLootTable(
                                    donorType
                                            .getDefaultLootTable()
                            );

            List<ItemStack> randomizedLoot =
                    donorLootTable
                            .getRandomItems(
                                    params
                            );

            /*
             * Стираем исходный loot только после
             * успешного расчёта таблицы донора.
             *
             * Если модовая таблица оказалась
             * несовместимой — настоящий дроп
             * моба сохранится.
             */
            event.getDrops().clear();

            for (ItemStack stack :
                    randomizedLoot) {

                if (stack.isEmpty()) {
                    continue;
                }

                ItemEntity itemEntity =
                        new ItemEntity(
                                level,
                                mob.getX(),
                                mob.getY()
                                        + 0.25D,
                                mob.getZ(),
                                stack.copy()
                        );

                event.getDrops().add(
                        itemEntity
                );
            }

            if (killingPlayer != null) {
                boolean firstDiscovery =
                        data.discoverMob(
                                sourceId
                        );

                int kills =
                        data.addMobKill(
                                sourceId
                        );

                if (firstDiscovery) {
                    Randomizer.LOGGER.info(
                            "Discovered mob loot: {} -> {}",
                            sourceId,
                            donorId
                    );
                }

                Randomizer.LOGGER.debug(
                        "Mob kill count: {} = {}",
                        sourceId,
                        kills
                );
            }

        } catch (RuntimeException exception) {
            Randomizer.LOGGER.warn(
                    "Failed to roll randomized mob loot: {} -> {}. Original drops preserved.",
                    sourceId,
                    donorId,
                    exception
            );
        }
    }

    public static EntityType<?> getMappedType(
            RandomizerWorldData data,
            EntityType<?> sourceType
    ) {
        ResourceLocation sourceId =
                BuiltInRegistries.ENTITY_TYPE
                        .getKey(sourceType);

        if (sourceId == null
                || RandomizerBlacklist
                .isEntityBlocked(sourceId)) {

            return null;
        }

        List<EntityType<?>> pool =
                RandomizerPools.mobs();

        if (pool.isEmpty()) {
            return null;
        }

        long seed =
                data.getSeed(
                        RandomizerChannel.MOBS
                );

        ensureMapping(seed);

        /*
         * Нормальный случай:
         * EntityType входит в permutation.
         */
        EntityType<?> mapped =
                cachedMapping.get(
                        sourceType
                );

        if (mapped != null) {
            return mapped;
        }

        /*
         * Fallback для настоящего Mob,
         * который зарегистрирован как MISC
         * или иным образом не попал
         * в основной mob pool.
         */
        int index =
                RandomizerHash.index(
                        seed,
                        sourceId,
                        pool.size()
                );

        return pool.get(index);
    }

    private static void ensureMapping(
            long seed
    ) {
        if (cachedSeed == seed) {
            return;
        }

        List<EntityType<?>> sources =
                RandomizerPools.mobs();

        if (sources.isEmpty()) {
            cachedMapping =
                    Map.of();

            cachedSeed =
                    seed;

            return;
        }

        ArrayList<EntityType<?>> donors =
                new ArrayList<>(
                        sources
                );

        /*
         * Sattolo shuffle:
         *
         * при N > 1 получается одна permutation,
         * в которой ни одна позиция
         * не остаётся сама на себе.
         */
        if (donors.size() > 1) {
            RandomSource random =
                    RandomSource.create(
                            seed
                    );

            for (int i =
                    donors.size() - 1;
                 i > 0;
                 i--) {

                int j =
                        random.nextInt(i);

                Collections.swap(
                        donors,
                        i,
                        j
                );
            }
        }

        HashMap<
                EntityType<?>,
                EntityType<?>
                > mapping =
                new HashMap<>();

        for (int i = 0;
             i < sources.size();
             i++) {

            mapping.put(
                    sources.get(i),
                    donors.get(i)
            );
        }

        cachedMapping =
                Map.copyOf(mapping);

        cachedSeed =
                seed;

        Randomizer.LOGGER.info(
                "Generated mob loot permutation for {} entity types.",
                cachedMapping.size()
        );
    }

    public static void invalidateCache() {
        cachedSeed =
                Long.MIN_VALUE;

        cachedMapping =
                Map.of();
    }
}