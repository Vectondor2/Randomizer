package dev.vect.randomizer.recipe;

import dev.vect.randomizer.Randomizer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class RecipeRandomizerRegistry {

    private RecipeRandomizerRegistry() {
    }

    public static final DeferredRegister<
            RecipeSerializer<?>
            > RECIPE_SERIALIZERS =
            DeferredRegister.create(
                    BuiltInRegistries.RECIPE_SERIALIZER,
                    Randomizer.MOD_ID
            );

    public static final Supplier<
            RecipeSerializer<RandomizedCraftingRecipe>
            > RANDOMIZED_CRAFTING =
            RECIPE_SERIALIZERS.register(
                    "randomized_crafting",
                    RandomizedCraftingRecipeSerializer::new
            );

    public static void register(
            IEventBus modEventBus
    ) {
        RECIPE_SERIALIZERS.register(
                modEventBus
        );
    }
}