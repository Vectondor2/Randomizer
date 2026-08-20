package dev.vect.randomizer.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class RandomizedCraftingRecipe
        implements CraftingRecipe {

    private final CraftingRecipe original;
    private final ItemStack replacement;

    public RandomizedCraftingRecipe(
            CraftingRecipe original,
            ItemStack replacement
    ) {
        this.original = original;
        this.replacement = replacement.copy();
    }

    public CraftingRecipe original() {
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
    public CraftingBookCategory category() {
        return original.category();
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
        return original.getType();
    }

    public static RandomizedCraftingRecipe from(
            Recipe<?> original,
            ItemStack replacement
    ) {
        if (!(original instanceof CraftingRecipe craftingRecipe)) {
            throw new IllegalArgumentException(
                    "Randomized crafting wrapper requires CraftingRecipe, got: "
                            + original.getClass().getName()
            );
        }

        return new RandomizedCraftingRecipe(
                craftingRecipe,
                replacement
        );
    }
}
