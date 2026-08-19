package dev.vect.randomizer.core;

import dev.vect.randomizer.config.RandomizerBlacklist;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;

public final class RandomizerPools {

    private static List<Item> itemPool;
    private static List<EntityType<?>> mobPool;

    private RandomizerPools() {
    }

    public static List<Item> items() {
        if (itemPool == null) {
            rebuildItems();
        }

        return itemPool;
    }

    public static List<EntityType<?>> mobs() {
        if (mobPool == null) {
            rebuildMobs();
        }

        return mobPool;
    }

    public static void rebuildItems() {
        itemPool =
                BuiltInRegistries.ITEM
                        .stream()
                        .filter(
                                item ->
                                        item != Items.AIR
                        )
                        .filter(item -> {
                            ResourceLocation id =
                                    BuiltInRegistries.ITEM
                                            .getKey(item);

                            return id != null
                                    && !RandomizerBlacklist
                                    .isItemBlocked(id);
                        })
                        .sorted(
                                Comparator.comparing(
                                        item ->
                                                BuiltInRegistries.ITEM
                                                        .getKey(item)
                                                        .toString()
                                )
                        )
                        .toList();
    }

    public static void rebuildMobs() {
        mobPool =
                BuiltInRegistries.ENTITY_TYPE
                        .stream()
                        .filter(
                                type ->
                                        type.getCategory()
                                                != MobCategory.MISC
                        )
                        .filter(type -> {
                            ResourceLocation id =
                                    BuiltInRegistries
                                            .ENTITY_TYPE
                                            .getKey(type);

                            return id != null
                                    && !RandomizerBlacklist
                                    .isEntityBlocked(id);
                        })
                        .sorted(
                                Comparator.comparing(
                                        type ->
                                                BuiltInRegistries
                                                        .ENTITY_TYPE
                                                        .getKey(type)
                                                        .toString()
                                )
                        )
                        .toList();
    }

    public static void clearCaches() {
        itemPool = null;
        mobPool = null;
    }
}