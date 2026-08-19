package dev.vect.randomizer.mob;

import dev.vect.randomizer.core.RandomizerPools;
import dev.vect.randomizer.core.RandomizerWorldData;
import dev.vect.randomizer.network.BestiaryPayload;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Bestiary {

    private Bestiary() {
    }

    public static void sendTo(
            ServerPlayer player
    ) {
        RandomizerWorldData data =
                RandomizerWorldData.get(
                        player.serverLevel()
                );

        List<BestiaryPayload.Entry> entries =
                createEntries(data);

        PacketDistributor.sendToPlayer(
                player,
                new BestiaryPayload(
                        data.getMasterSeed(),
                        entries
                )
        );
    }

    private static List<BestiaryPayload.Entry>
    createEntries(
            RandomizerWorldData data
    ) {
        /*
         * Основной mob pool +
         * MISC-мобы, которые игрок уже реально встретил.
         */
        Set<ResourceLocation> sourceIds =
                new LinkedHashSet<>();

        for (EntityType<?> type :
                RandomizerPools.mobs()) {

            ResourceLocation id =
                    BuiltInRegistries.ENTITY_TYPE
                            .getKey(type);

            if (id != null) {
                sourceIds.add(id);
            }
        }

        sourceIds.addAll(
                data.getDiscoveredMobs()
        );

        ArrayList<BestiaryPayload.Entry>
                result = new ArrayList<>();

        sourceIds.stream()
                .sorted()
                .forEach(sourceId -> {
                    EntityType<?> sourceType =
                            BuiltInRegistries.ENTITY_TYPE
                                    .get(sourceId);

                    if (sourceType == null) {
                        return;
                    }

                    boolean discovered =
                            data.isMobDiscovered(
                                    sourceId
                            );

                    String targetId = "";

                    if (discovered) {
                        EntityType<?> targetType =
                                MobLootRandomizer
                                        .getMappedType(
                                                data,
                                                sourceType
                                        );

                        if (targetType != null) {
                            ResourceLocation target =
                                    BuiltInRegistries
                                            .ENTITY_TYPE
                                            .getKey(
                                                    targetType
                                            );

                            if (target != null) {
                                targetId =
                                        target.toString();
                            }
                        }
                    }

                    result.add(
                            new BestiaryPayload.Entry(
                                    sourceId.toString(),
                                    targetId,
                                    data.getMobKills(
                                            sourceId
                                    ),
                                    discovered
                            )
                    );
                });

        return List.copyOf(result);
    }
}