package dev.vect.randomizer.fishing;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.vect.randomizer.core.RandomizerChannel;
import dev.vect.randomizer.core.RandomizerHash;
import dev.vect.randomizer.core.RandomizerPools;
import dev.vect.randomizer.core.RandomizerWorldData;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.List;

public final class FishingRandomizer
        extends LootModifier {

    /*
     * Только основная fishing loot table.
     *
     * Сундуки, мобы и любые другие loot tables
     * сюда не попадут.
     */
    private static final ResourceLocation
            FISHING_TABLE =
            ResourceLocation.withDefaultNamespace(
                    "gameplay/fishing"
            );

    public static final MapCodec<
            FishingRandomizer
            > CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            LootModifier
                                    .codecStart(instance)
                                    .apply(
                                            instance,
                                            FishingRandomizer::new
                                    )
            );

    public FishingRandomizer(
            LootItemCondition[] conditions
    ) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack>
    doApply(
            ObjectArrayList<ItemStack> generatedLoot,
            LootContext context
    ) {
        if (!FISHING_TABLE.equals(
                context.getQueriedLootTableId()
        )) {
            return generatedLoot;
        }

        List<Item> pool =
                RandomizerPools.items();

        if (pool.isEmpty()) {
            return generatedLoot;
        }

        RandomizerWorldData data =
                RandomizerWorldData.get(
                        context.getLevel()
                );

        long seed =
                data.getSeed(
                        RandomizerChannel.FISHING
                );

        ObjectArrayList<ItemStack>
                randomized =
                new ObjectArrayList<>();

        for (ItemStack original :
                generatedLoot) {

            if (original.isEmpty()) {
                continue;
            }

            ResourceLocation sourceId =
                    BuiltInRegistries.ITEM
                            .getKey(
                                    original.getItem()
                            );

            if (sourceId == null) {
                randomized.add(
                        original
                );

                continue;
            }

            int index =
                    RandomizerHash.index(
                            seed,
                            sourceId,
                            pool.size()
                    );

            Item replacement =
                    pool.get(index);

            ItemStack result =
                    new ItemStack(
                            replacement
                    );

            /*
             * Сохраняем количество улова,
             * но не позволяем создавать
             * illegal overstack.
             */
            result.setCount(
                    Math.min(
                            Math.max(
                                    original.getCount(),
                                    1
                            ),
                            result.getMaxStackSize()
                    )
            );

            randomized.add(result);
        }

        return randomized;
    }

    @Override
    public MapCodec<
            ? extends IGlobalLootModifier
            > codec() {

        return CODEC;
    }
}