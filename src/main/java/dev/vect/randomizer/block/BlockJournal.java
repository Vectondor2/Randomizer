package dev.vect.randomizer.block;

import dev.vect.randomizer.config.RandomizerBlacklist;
import dev.vect.randomizer.core.RandomizerWorldData;
import dev.vect.randomizer.network.BlockJournalPayload;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class BlockJournal {

    private BlockJournal() {
    }

    public static void sendTo(
            ServerPlayer player
    ) {
        RandomizerWorldData data =
                RandomizerWorldData.get(
                        player.serverLevel()
                );

        ArrayList<BlockJournalPayload.Entry>
                entries =
                new ArrayList<>();

        data.getDiscoveredBlocks()
                .stream()
                .filter(
                        id ->
                                !RandomizerBlacklist
                                        .isBlockBlocked(id)
                )
                .sorted(
                        Comparator.comparing(
                                ResourceLocation::toString
                        )
                )
                .forEach(blockId -> {
                    Item result =
                            BlockRandomizer
                                    .getMappedItem(
                                            data,
                                            blockId
                                    );

                    if (result == null) {
                        return;
                    }

                    ResourceLocation itemId =
                            BuiltInRegistries.ITEM
                                    .getKey(result);

                    if (itemId == null) {
                        return;
                    }

                    entries.add(
                            new BlockJournalPayload.Entry(
                                    blockId.toString(),
                                    itemId.toString()
                            )
                    );
                });

        PacketDistributor.sendToPlayer(
                player,
                new BlockJournalPayload(
                        data.getMasterSeed(),
                        List.copyOf(entries)
                )
        );
    }
}