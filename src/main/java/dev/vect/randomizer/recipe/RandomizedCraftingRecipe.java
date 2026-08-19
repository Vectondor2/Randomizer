package dev.vect.randomizer.recipe;

import dev.vect.randomizer.recipe.RandomizedCraftingRecipeSerializer;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class RandomizedCraftingRecipe
        implements Recipe<CraftingInput> {

    private final Recipe<CraftingInput> original;
    private final ItemStack replacement;

    public RandomizedCraftingRecipe(
            Recipe<CraftingInput> original,
            ItemStack replacement
    ) {
        this.original = original;
        this.replacement = replacement.copy();
    }

    public Recipe<CraftingInput> original() {
        return original;
    }

    public ItemStack replacement() {
        return replacement.copy();
    }

    @Override
    public boolean matches(
            CraftingInput input,
            Level level
    ) {
        return original.matches(
                input,
                level
        );
    }

    @Override
    public ItemStack assemble(
            CraftingInput input,
            HolderLookup.Provider registries
    ) {
        /*
         * Самое главное место:
         * входы и matching остаются оригинальными,
         * но результат полностью заменяется.
         */
        return replacement.copy();
    }

    @Override
    public boolean canCraftInDimensions(
            int width,
            int height
    ) {
        return original.canCraftInDimensions(
                width,
                height
        );
    }

    @Override
    public ItemStack getResultItem(
            HolderLookup.Provider registries
    ) {
        /*
         * Рецептбук, JEI-подобные системы и клиент
         * также увидят рандомизированный результат.
         */
        return replacement.copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return original.getIngredients();
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(
            CraftingInput input
    ) {
        return original.getRemainingItems(
                input
        );
    }

    @Override
    public String getGroup() {
        return original.getGroup();
    }

    @Override
    public boolean isSpecial() {
        return original.isSpecial();
    }

    @Override
    public boolean showNotification() {
        return original.showNotification();
    }

    @Override
    public ItemStack getToastSymbol() {
        return original.getToastSymbol();
    }

    @Override
    public boolean isIncomplete() {
        return original.isIncomplete();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeRandomizerRegistry
                .RANDOMIZED_CRAFTING
                .get();
    }

    @Override
    public RecipeType<?> getType() {
        /*
         * Важно:
         * тип остаётся minecraft:crafting.
         *
         * Поэтому CraftingTable/Crafter продолжают
         * находить этот рецепт как обычный crafting.
         */
        return original.getType();
    }

    @SuppressWarnings("unchecked")
    public static RandomizedCraftingRecipe from(
            Recipe<?> original,
            ItemStack replacement
    ) {
        return new RandomizedCraftingRecipe(
                (Recipe<CraftingInput>) original,
                replacement
        );
    }
}