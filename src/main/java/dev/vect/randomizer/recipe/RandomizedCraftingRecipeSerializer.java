package dev.vect.randomizer.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class RandomizedCraftingRecipeSerializer
        implements RecipeSerializer<RandomizedCraftingRecipe> {

    /*
     * JSON codec нам практически не понадобится,
     * потому что RandomizedCraftingRecipe создаётся
     * программно.
     *
     * Но RecipeSerializer обязан иметь codec.
     */
    public static final MapCodec<
            RandomizedCraftingRecipe
            > CODEC =
            RecordCodecBuilder.mapCodec(
                    instance ->
                            instance.group(
                                    Recipe.CODEC
                                            .fieldOf("original")
                                            .forGetter(
                                                    RandomizedCraftingRecipe::original
                                            ),

                                    ItemStack.CODEC
                                            .fieldOf("replacement")
                                            .forGetter(
                                                    RandomizedCraftingRecipe::replacement
                                            )
                            )
                            .apply(
                                    instance,
                                    RandomizedCraftingRecipe::from
                            )
            );

    /*
     * Этот codec действительно важен:
     * сервер через него передаёт изменённый
     * рецепт клиенту.
     */
    public static final StreamCodec<
            RegistryFriendlyByteBuf,
            RandomizedCraftingRecipe
            > STREAM_CODEC =
            StreamCodec.composite(
                    Recipe.STREAM_CODEC,
                    RandomizedCraftingRecipe::original,

                    ItemStack.STREAM_CODEC,
                    RandomizedCraftingRecipe::replacement,

                    RandomizedCraftingRecipe::from
            );

    @Override
    public MapCodec<RandomizedCraftingRecipe>
    codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<
            RegistryFriendlyByteBuf,
            RandomizedCraftingRecipe
            > streamCodec() {

        return STREAM_CODEC;
    }
}