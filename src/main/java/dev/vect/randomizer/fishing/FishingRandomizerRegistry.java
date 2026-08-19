package dev.vect.randomizer.fishing;

import com.mojang.serialization.MapCodec;

import dev.vect.randomizer.Randomizer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class FishingRandomizerRegistry {

    private FishingRandomizerRegistry() {
    }

    public static final DeferredRegister<
            MapCodec<? extends IGlobalLootModifier>
            > GLOBAL_LOOT_MODIFIERS =
            DeferredRegister.create(
                    NeoForgeRegistries.Keys
                            .GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    Randomizer.MOD_ID
            );

    public static final Supplier<
            MapCodec<FishingRandomizer>
            > FISHING_RANDOMIZER =
            GLOBAL_LOOT_MODIFIERS.register(
                    "fishing_randomizer",
                    () -> FishingRandomizer.CODEC
            );

    public static void register(
            IEventBus modEventBus
    ) {
        GLOBAL_LOOT_MODIFIERS.register(
                modEventBus
        );
    }
}