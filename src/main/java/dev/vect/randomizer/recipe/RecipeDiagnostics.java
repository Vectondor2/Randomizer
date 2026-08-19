package dev.vect.randomizer.recipe;

import dev.vect.randomizer.Randomizer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class RecipeDiagnostics {

    private RecipeDiagnostics() {
    }

    public static void write(
            MinecraftServer server,
            List<RecipeHolder<?>> recipes,
            Map<ResourceLocation, ItemStack>
                    replacements
    ) {
        Path directory =
                Path.of(
                        "logs",
                        "randomizer"
                );

        Path file =
                directory.resolve(
                        "recipes.txt"
                );

        try {
            Files.createDirectories(
                    directory
            );

            List<String> lines =
                    buildReport(
                            server,
                            recipes,
                            replacements
                    );

            Files.write(
                    file,
                    lines,
                    StandardCharsets.UTF_8
            );

            Randomizer.LOGGER.info(
                    "Recipe diagnostics written to {}",
                    file.toAbsolutePath()
            );

        } catch (IOException exception) {
            Randomizer.LOGGER.error(
                    "Failed to write recipe diagnostics.",
                    exception
            );
        }
    }

    private static List<String>
    buildReport(
            MinecraftServer server,
            List<RecipeHolder<?>> recipes,
            Map<ResourceLocation, ItemStack>
                    replacements
    ) {
        ArrayList<String> lines =
                new ArrayList<>();

        Map<String, Integer>
                typeCounts =
                new TreeMap<>();

        Map<String, Integer>
                serializerCounts =
                new TreeMap<>();

        Map<String, Integer>
                unsupportedClasses =
                new TreeMap<>();

        int craftingTotal = 0;
        int craftingStatic = 0;
        int craftingDynamic = 0;

        for (RecipeHolder<?> holder :
                recipes) {

            Recipe<?> recipe =
                    holder.value();

            ResourceLocation typeId =
                    BuiltInRegistries
                            .RECIPE_TYPE
                            .getKey(
                                    recipe.getType()
                            );

            ResourceLocation serializerId =
                    BuiltInRegistries
                            .RECIPE_SERIALIZER
                            .getKey(
                                    recipe.getSerializer()
                            );

            String type =
                    typeId == null
                            ? "<unknown>"
                            : typeId.toString();

            String serializer =
                    serializerId == null
                            ? "<unknown>"
                            : serializerId.toString();

            typeCounts.merge(
                    type,
                    1,
                    Integer::sum
            );

            serializerCounts.merge(
                    serializer,
                    1,
                    Integer::sum
            );

            if (recipe.getType()
                    == RecipeType.CRAFTING) {

                craftingTotal++;

                if (replacements.containsKey(
                        holder.id()
                )) {
                    craftingStatic++;
                } else {
                    craftingDynamic++;
                }

            } else {
                String className =
                        recipe.getClass()
                                .getName();

                unsupportedClasses.merge(
                        type
                                + " | "
                                + serializer
                                + " | "
                                + className,
                        1,
                        Integer::sum
                );
            }
        }

        lines.add(
                "=============================================="
        );
        lines.add(
                "        UNIVERSAL RANDOMIZER - RECIPES"
        );
        lines.add(
                "=============================================="
        );
        lines.add("");

        lines.add(
                "Total recipes: "
                        + recipes.size()
        );

        lines.add(
                "Crafting recipes: "
                        + craftingTotal
        );

        lines.add(
                "Randomized static crafting recipes: "
                        + craftingStatic
        );

        lines.add(
                "Dynamic/special crafting recipes skipped: "
                        + craftingDynamic
        );

        lines.add(
                "Other recipe types awaiting adapters: "
                        + (
                        recipes.size()
                                - craftingTotal
                )
        );

        lines.add("");
        lines.add(
                "----------------------------------------------"
        );
        lines.add(
                "RECIPE TYPES"
        );
        lines.add(
                "----------------------------------------------"
        );

        typeCounts.forEach(
                (type, count) ->
                        lines.add(
                                String.format(
                                        "%7d  %s",
                                        count,
                                        type
                                )
                        )
        );

        lines.add("");
        lines.add(
                "----------------------------------------------"
        );
        lines.add(
                "SERIALIZERS"
        );
        lines.add(
                "----------------------------------------------"
        );

        serializerCounts.forEach(
                (serializer, count) ->
                        lines.add(
                                String.format(
                                        "%7d  %s",
                                        count,
                                        serializer
                                )
                        )
        );

        lines.add("");
        lines.add(
                "----------------------------------------------"
        );
        lines.add(
                "NON-CRAFTING IMPLEMENTATIONS"
        );
        lines.add(
                "----------------------------------------------"
        );

        unsupportedClasses
                .entrySet()
                .stream()
                .sorted(
                        Map.Entry
                                .<String, Integer>
                                        comparingByValue()
                                .reversed()
                                .thenComparing(
                                        Map.Entry::getKey
                                )
                )
                .forEach(entry ->
                        lines.add(
                                String.format(
                                        "%7d  %s",
                                        entry.getValue(),
                                        entry.getKey()
                                )
                        )
                );

        lines.add("");
        lines.add(
                "----------------------------------------------"
        );
        lines.add(
                "CURRENT RANDOMIZED MAPPINGS"
        );
        lines.add(
                "----------------------------------------------"
        );

        replacements
                .entrySet()
                .stream()
                .sorted(
                        Comparator.comparing(
                                entry ->
                                        entry.getKey()
                                                .toString()
                        )
                )
                .forEach(entry -> {
                    ResourceLocation itemId =
                            BuiltInRegistries.ITEM
                                    .getKey(
                                            entry
                                                    .getValue()
                                                    .getItem()
                                    );

                    lines.add(
                            entry.getKey()
                                    + " -> "
                                    + (
                                    itemId == null
                                            ? "<unknown>"
                                            : itemId
                            )
                                    + " x"
                                    + entry
                                    .getValue()
                                    .getCount()
                    );
                });

        return List.copyOf(lines);
    }
}