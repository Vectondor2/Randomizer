package dev.vect.randomizer.block;

import dev.vect.randomizer.Randomizer;
import dev.vect.randomizer.config.RandomizerBlacklist;
import dev.vect.randomizer.core.RandomizerChannel;
import dev.vect.randomizer.core.RandomizerHash;
import dev.vect.randomizer.core.RandomizerPools;
import dev.vect.randomizer.core.RandomizerWorldData;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class BlockRandomizer {

    private BlockRandomizer() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(
                BlockRandomizer::onBlockDrops
        );
    }

    private static void onBlockDrops(
            BlockDropsEvent event
    ) {
        Block block =
                event.getState().getBlock();

        ResourceLocation blockId =
                BuiltInRegistries.BLOCK
                        .getKey(block);

        if (blockId == null) {
            return;
        }

        /*
         * Blacklisted block вообще не участвует
         * в рандомайзере.
         */
        if (RandomizerBlacklist
                .isBlockBlocked(blockId)) {
            return;
        }

        RandomizerWorldData data =
                RandomizerWorldData.get(
                        event.getLevel()
                );

        Entity breaker =
                event.getBreaker();

        /*
         * SHIFT BYPASS
         *
         * Работает только после того,
         * как этот тип блока хотя бы один раз
         * прошёл через Randomizer.
         */
        if (breaker instanceof Player player
                && player.isShiftKeyDown()
                && data.isBlockDiscovered(blockId)) {

            /*
             * Ничего не меняем.
             * Minecraft/мод оставляет свой
             * оригинальный дроп.
             */
            return;
        }

        Item replacement =
                getMappedItem(
                        data,
                        blockId
                );

        if (replacement == null) {
            Randomizer.LOGGER.error(
                    "Could not find randomized drop for block {}",
                    blockId
            );

            return;
        }

        /*
         * Удаляем исходный рассчитанный loot.
         */
        event.getDrops().clear();

        ItemEntity randomizedDrop =
                new ItemEntity(
                        event.getLevel(),
                        event.getPos().getX()
                                + 0.5D,
                        event.getPos().getY()
                                + 0.5D,
                        event.getPos().getZ()
                                + 0.5D,
                        new ItemStack(
                                replacement
                        )
                );

        event.getDrops().add(
                randomizedDrop
        );

        /*
         * После первого успешного
         * рандомизированного разрушения
         * блок считается открытым.
         */
        if (data.discoverBlock(blockId)) {
            Randomizer.LOGGER.info(
                    "Discovered block: {} -> {}",
                    blockId,
                    BuiltInRegistries.ITEM
                            .getKey(replacement)
            );
        }
    }

    /**
     * Возвращает стабильный результат
     * для конкретного Block ID.
     *
     * Этот же метод используется журналом блоков,
     * чтобы GUI показывал ровно тот же mapping,
     * который реально используется игровым событием.
     */
    @Nullable
    public static Item getMappedItem(
            RandomizerWorldData data,
            ResourceLocation blockId
    ) {
        if (RandomizerBlacklist
                .isBlockBlocked(blockId)) {
            return null;
        }

        List<Item> pool =
                RandomizerPools.items();

        if (pool.isEmpty()) {
            return null;
        }

        long seed =
                data.getSeed(
                        RandomizerChannel.BLOCKS
                );

        int index =
                RandomizerHash.index(
                        seed,
                        blockId,
                        pool.size()
                );

        return pool.get(index);
    }
}