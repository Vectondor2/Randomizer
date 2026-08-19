package dev.vect.randomizer.trade;

import dev.vect.randomizer.Randomizer;
import dev.vect.randomizer.core.RandomizerChannel;
import dev.vect.randomizer.core.RandomizerHash;
import dev.vect.randomizer.core.RandomizerPools;
import dev.vect.randomizer.core.RandomizerWorldData;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class TradeRandomizer {

    private TradeRandomizer() {
    }

    public static void register() {
        /*
         * LOWEST нужен, чтобы сначала дать
         * остальным модам ATM10 добавить
         * и изменить свои сделки.
         *
         * После этого Randomizer получает
         * максимально полный список.
         */
        NeoForge.EVENT_BUS.addListener(
                EventPriority.LOWEST,
                TradeRandomizer::onVillagerTrades
        );

        NeoForge.EVENT_BUS.addListener(
                EventPriority.LOWEST,
                TradeRandomizer::onWandererTrades
        );
    }

    private static void onVillagerTrades(
            VillagerTradesEvent event
    ) {
        ResourceLocation professionId =
                BuiltInRegistries
                        .VILLAGER_PROFESSION
                        .getKey(
                                event.getType()
                        );

        if (professionId == null) {
            return;
        }

        for (int level = 1;
             level <= 5;
             level++) {

            List<ItemListing> originalList =
                    event.getTrades()
                            .get(level);

            if (originalList == null
                    || originalList.isEmpty()) {

                continue;
            }

            /*
             * ВАЖНО:
             *
             * Не пытаемся делать originalList.set().
             *
             * Некоторые моды ATM10 могут оставить
             * здесь immutable List.
             *
             * Вместо этого создаём собственный
             * ArrayList и заменяем значение
             * целиком в mutable map NeoForge.
             */
            List<ItemListing> wrappedList =
                    createWrappedCopy(
                            originalList,
                            "villager:"
                                    + professionId
                                    + ":level_"
                                    + level
                    );

            event.getTrades()
                    .put(
                            level,
                            wrappedList
                    );
        }
    }

    private static void onWandererTrades(
            WandererTradesEvent event
    ) {
        /*
         * В WandererTradesEvent нет setter'а
         * для самих списков.
         *
         * NeoForge обычно передаёт сюда
         * mutable NonNullList.
         *
         * Но для безопасности не позволяем
         * immutable-list уронить загрузку мира.
         */
        wrapWandererListSafely(
                event.getGenericTrades(),
                "wanderer:generic"
        );

        wrapWandererListSafely(
                event.getRareTrades(),
                "wanderer:rare"
        );
    }

    private static List<ItemListing>
    createWrappedCopy(
            List<ItemListing> originalList,
            String prefix
    ) {
        ArrayList<ItemListing> result =
                new ArrayList<>(
                        originalList.size()
                );

        for (int index = 0;
             index < originalList.size();
             index++) {

            ItemListing original =
                    originalList.get(index);

            /*
             * Защита от двойной обёртки
             * при каком-нибудь нестандартном reload.
             */
            if (original
                    instanceof RandomizedListing) {

                result.add(original);

                continue;
            }

            result.add(
                    new RandomizedListing(
                            original,
                            prefix
                                    + ":"
                                    + index
                    )
            );
        }

        return result;
    }

    private static void wrapWandererListSafely(
            List<ItemListing> listings,
            String prefix
    ) {
        for (int index = 0;
             index < listings.size();
             index++) {

            ItemListing original =
                    listings.get(index);

            if (original
                    instanceof RandomizedListing) {

                continue;
            }

            try {
                listings.set(
                        index,
                        new RandomizedListing(
                                original,
                                prefix
                                        + ":"
                                        + index
                        )
                );

            } catch (UnsupportedOperationException exception) {
                /*
                 * Лучше оставить Wandering Trader
                 * нерандомизированным, чем уничтожить
                 * загрузку всего мира.
                 */
                Randomizer.LOGGER.warn(
                        "Wandering trader list '{}' is immutable. "
                                + "Skipping its trade randomization.",
                        prefix
                );

                return;
            }
        }
    }

    private record RandomizedListing(
            ItemListing original,
            String mappingKey
    ) implements ItemListing {

        @Nullable
        @Override
        public MerchantOffer getOffer(
                Entity entity,
                RandomSource random
        ) {
            MerchantOffer offer =
                    original.getOffer(
                            entity,
                            random
                    );

            if (offer == null) {
                return null;
            }

            if (!(entity.level()
                    instanceof ServerLevel level)) {

                return offer;
            }

            List<Item> pool =
                    RandomizerPools.items();

            if (pool.isEmpty()) {
                return offer;
            }

            RandomizerWorldData data =
                    RandomizerWorldData.get(
                            level
                    );

            long seed =
                    data.getSeed(
                            RandomizerChannel.TRADES
                    );

            int index =
                    RandomizerHash.index(
                            seed,
                            mappingKey,
                            pool.size()
                    );

            Item replacement =
                    pool.get(index);

            int originalCount =
                    Math.max(
                            1,
                            offer.result
                                    .getCount()
                    );

            ItemStack randomizedResult =
                    new ItemStack(
                            replacement
                    );

            /*
             * Если оригинальная торговля
             * продавала, например, 16 предметов,
             * стараемся сохранить количество.
             *
             * Но не превышаем max stack size
             * нового предмета.
             */
            randomizedResult.setCount(
                    Math.min(
                            originalCount,
                            randomizedResult
                                    .getMaxStackSize()
                    )
            );

            /*
             * Меняем только результат.
             *
             * Цена,
             * второй ингредиент,
             * maxUses,
             * XP,
             * demand,
             * price multiplier
             *
             * остаются оригинальными.
             */
            offer.result =
                    randomizedResult;

            return offer;
        }
    }
}