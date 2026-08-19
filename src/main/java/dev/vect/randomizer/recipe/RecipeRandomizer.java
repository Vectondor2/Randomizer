package dev.vect.randomizer.recipe;

import dev.vect.randomizer.Randomizer;
import dev.vect.randomizer.config.RandomizerBlacklist;
import dev.vect.randomizer.core.RandomizerChannel;
import dev.vect.randomizer.core.RandomizerWorldData;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecipeRandomizer {

    private RecipeRandomizer() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(
                RecipeRandomizer::onDatapackSync
        );
    }

    private static void onDatapackSync(
            OnDatapackSyncEvent event
    ) {
        MinecraftServer server =
                event.getPlayerList()
                        .getServer();

        apply(server);
    }

    public static synchronized void apply(
            MinecraftServer server
    ) {
        RecipeManager manager =
                server.getRecipeManager();

        RandomizerWorldData data =
                RandomizerWorldData.get(
                        server.overworld()
                );

        long recipeSeed =
                data.getSeed(
                        RandomizerChannel.RECIPES
                );

        /*
         * Берём рецепты через публичный API.
         *
         * Если они уже были рандомизированы,
         * unwrap() вернёт исходную версию,
         * поэтому wrapper внутри wrapper
         * не появляется.
         */
        List<RecipeHolder<?>> originals =
                getOriginalRecipes(
                        manager
                );

        HolderLookup.Provider registries =
                server.registryAccess();

        List<Candidate> candidates =
                findCandidates(
                        originals,
                        registries
                );

        Map<ResourceLocation, ItemStack>
                replacements =
                createPermutation(
                        candidates,
                        recipeSeed
                );

        ArrayList<RecipeHolder<?>>
                finalRecipes =
                new ArrayList<>(
                        originals.size()
                );

        int randomized = 0;

        for (RecipeHolder<?> holder :
                originals) {

            ItemStack replacement =
                    replacements.get(
                            holder.id()
                    );

            RecipeHolder<?> finalHolder =
                    holder;

            if (replacement != null) {
                finalHolder =
                        wrapCraftingRecipe(
                                holder,
                                replacement
                        );

                randomized++;
            }

            finalRecipes.add(
                    finalHolder
            );
        }

        /*
         * NeoForge/Minecraft сам перестроит
         * внутренние byType и byName.
         *
         * Нам больше не нужен прямой доступ
         * к приватным полям RecipeManager.
         */
        manager.replaceRecipes(
                finalRecipes
        );

        Randomizer.LOGGER.info(
                "Recipe Randomizer: {} total recipes, {} randomized crafting recipes.",
                originals.size(),
                randomized
        );

        RecipeDiagnostics.write(
                server,
                originals,
                replacements
        );
    }

    public static List<RecipeHolder<?>>
    getOriginalRecipes(
            RecipeManager manager
    ) {
        LinkedHashMap<
                ResourceLocation,
                RecipeHolder<?>
                > originals =
                new LinkedHashMap<>();

        manager.getRecipes()
                .stream()
                .sorted(
                        (first, second) ->
                                first.id()
                                        .toString()
                                        .compareTo(
                                                second.id()
                                                        .toString()
                                        )
                )
                .forEach(holder -> {
                    RecipeHolder<?> original =
                            unwrap(holder);

                    originals.put(
                            original.id(),
                            original
                    );
                });

        return List.copyOf(
                originals.values()
        );
    }

    private static RecipeHolder<?>
    unwrap(
            RecipeHolder<?> holder
    ) {
        if (holder.value()
                instanceof RandomizedCraftingRecipe randomized) {

            return new RecipeHolder<>(
                    holder.id(),
                    randomized.original()
            );
        }

        return holder;
    }

    private static List<Candidate>
    findCandidates(
            List<RecipeHolder<?>> recipes,
            HolderLookup.Provider registries
    ) {
        ArrayList<Candidate> candidates =
                new ArrayList<>();

        for (RecipeHolder<?> holder :
                recipes) {

            /*
             * Blacklisted recipe остаётся
             * полностью оригинальным.
             */
            if (RandomizerBlacklist
                    .isRecipeBlocked(
                            holder.id()
                    )) {

                continue;
            }

            Recipe<?> recipe =
                    holder.value();

            /*
             * Пока безопасно рандомизируем
             * стандартный crafting type.
             *
             * Остальные типы потом увидим
             * в diagnostics.
             */
            if (recipe.getType()
                    != RecipeType.CRAFTING) {

                continue;
            }

            ItemStack result;

            try {
                result =
                        recipe.getResultItem(
                                registries
                        );

            } catch (RuntimeException exception) {
                Randomizer.LOGGER.warn(
                        "Recipe {} threw while reading result.",
                        holder.id(),
                        exception
                );

                continue;
            }

            /*
             * Special/dynamic crafting recipes
             * могут не иметь статического результата.
             */
            if (result == null
                    || result.isEmpty()) {

                continue;
            }

            ResourceLocation resultId =
                    BuiltInRegistries.ITEM
                            .getKey(
                                    result.getItem()
                            );

            if (resultId == null
                    || RandomizerBlacklist
                    .isItemBlocked(
                            resultId
                    )) {

                continue;
            }

            candidates.add(
                    new Candidate(
                            holder.id(),
                            result.copy()
                    )
            );
        }

        /*
         * Детерминированный порядок.
         */
        candidates.sort(
                (first, second) ->
                        first.id()
                                .toString()
                                .compareTo(
                                        second.id()
                                                .toString()
                                )
        );

        return List.copyOf(
                candidates
        );
    }

    private static Map<
            ResourceLocation,
            ItemStack
            > createPermutation(
            List<Candidate> candidates,
            long seed
    ) {
        if (candidates.isEmpty()) {
            return Map.of();
        }

        if (candidates.size() == 1) {
            Candidate only =
                    candidates.getFirst();

            return Map.of(
                    only.id(),
                    only.result().copy()
            );
        }

        ArrayList<ItemStack> donors =
                new ArrayList<>(
                        candidates.size()
                );

        for (Candidate candidate :
                candidates) {

            donors.add(
                    candidate.result()
                            .copy()
            );
        }

        RandomSource random =
                RandomSource.create(
                        seed
                );

        /*
         * Sattolo permutation.
         *
         * При N > 1 ни один рецепт
         * не остаётся со своим результатом.
         */
        for (int i =
                donors.size() - 1;
             i > 0;
             i--) {

            int j =
                    random.nextInt(i);

            Collections.swap(
                    donors,
                    i,
                    j
            );
        }

        HashMap<
                ResourceLocation,
                ItemStack
                > mapping =
                new HashMap<>();

        for (int i = 0;
             i < candidates.size();
             i++) {

            mapping.put(
                    candidates.get(i)
                            .id(),

                    donors.get(i)
                            .copy()
            );
        }

        return Map.copyOf(
                mapping
        );
    }

    @SuppressWarnings("unchecked")
    private static RecipeHolder<?>
    wrapCraftingRecipe(
            RecipeHolder<?> holder,
            ItemStack replacement
    ) {
        Recipe<CraftingInput> recipe =
                (Recipe<CraftingInput>)
                        holder.value();

        RandomizedCraftingRecipe randomized =
                new RandomizedCraftingRecipe(
                        recipe,
                        replacement
                );

        return new RecipeHolder<>(
                holder.id(),
                randomized
        );
    }

    private record Candidate(
            ResourceLocation id,
            ItemStack result
    ) {
    }
}