package dev.vect.randomizer.config;

import dev.vect.randomizer.Randomizer;

import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

public final class RandomizerBlacklist {

    private static final Path FILE =
            Path.of(
                    "config",
                    "randomizer-blacklist.txt"
            );

    private static volatile Snapshot snapshot =
            Snapshot.empty();

    private static final String DEFAULT_FILE = """
            # Universal Randomizer blacklist
            #
            # Форматы:
            #
            # namespace:modid
            # item:modid:item_id
            # block:modid:block_id
            # entity:modid:entity_id
            # recipe:modid:recipe_id
            #
            # Строки с # являются комментариями.
            #
            # Примеры:
            #
            # namespace:ftbquests
            # item:examplemod:debug_item
            # block:examplemod:technical_block
            # entity:examplemod:dummy
            # recipe:examplemod:internal_recipe
            #
            # По умолчанию blacklist пуст.
            """;

    private RandomizerBlacklist() {
    }

    public static synchronized boolean reload() {
        try {
            Files.createDirectories(
                    FILE.getParent()
            );

            if (Files.notExists(FILE)) {
                Files.writeString(
                        FILE,
                        DEFAULT_FILE,
                        StandardCharsets.UTF_8
                );
            }

            Set<String> namespaces =
                    new HashSet<>();

            Set<ResourceLocation> items =
                    new HashSet<>();

            Set<ResourceLocation> blocks =
                    new HashSet<>();

            Set<ResourceLocation> entities =
                    new HashSet<>();

            Set<ResourceLocation> recipes =
                    new HashSet<>();

            int lineNumber = 0;

            for (String rawLine :
                    Files.readAllLines(
                            FILE,
                            StandardCharsets.UTF_8
                    )) {

                lineNumber++;

                String line =
                        rawLine.strip();

                if (line.isEmpty()
                        || line.startsWith("#")) {
                    continue;
                }

                int separator =
                        line.indexOf(':');

                if (separator <= 0
                        || separator
                        >= line.length() - 1) {

                    warnInvalid(
                            lineNumber,
                            rawLine
                    );

                    continue;
                }

                String type =
                        line.substring(
                                        0,
                                        separator
                                )
                                .strip()
                                .toLowerCase(
                                        Locale.ROOT
                                );

                String value =
                        line.substring(
                                        separator + 1
                                )
                                .strip();

                switch (type) {
                    case "namespace" -> {
                        if (!value.isBlank()) {
                            namespaces.add(
                                    value.toLowerCase(
                                            Locale.ROOT
                                    )
                            );
                        }
                    }

                    case "item" ->
                            parseId(
                                    value,
                                    items::add,
                                    lineNumber,
                                    rawLine
                            );

                    case "block" ->
                            parseId(
                                    value,
                                    blocks::add,
                                    lineNumber,
                                    rawLine
                            );

                    case "entity" ->
                            parseId(
                                    value,
                                    entities::add,
                                    lineNumber,
                                    rawLine
                            );

                    case "recipe" ->
                            parseId(
                                    value,
                                    recipes::add,
                                    lineNumber,
                                    rawLine
                            );

                    default ->
                            warnInvalid(
                                    lineNumber,
                                    rawLine
                            );
                }
            }

            snapshot =
                    new Snapshot(
                            Set.copyOf(namespaces),
                            Set.copyOf(items),
                            Set.copyOf(blocks),
                            Set.copyOf(entities),
                            Set.copyOf(recipes)
                    );

            Randomizer.LOGGER.info(
                    "Randomizer blacklist loaded: {}",
                    summary()
            );

            return true;

        } catch (IOException exception) {
            Randomizer.LOGGER.error(
                    "Failed to load Randomizer blacklist from {}",
                    FILE.toAbsolutePath(),
                    exception
            );

            return false;
        }
    }

    private static void parseId(
            String value,
            Consumer<ResourceLocation> consumer,
            int lineNumber,
            String rawLine
    ) {
        ResourceLocation id =
                ResourceLocation.tryParse(value);

        if (id == null) {
            warnInvalid(
                    lineNumber,
                    rawLine
            );

            return;
        }

        consumer.accept(id);
    }

    private static void warnInvalid(
            int lineNumber,
            String line
    ) {
        Randomizer.LOGGER.warn(
                "Invalid blacklist entry at line {}: {}",
                lineNumber,
                line
        );
    }

    public static boolean isItemBlocked(
            ResourceLocation id
    ) {
        if (id == null) {
            return true;
        }

        Snapshot current =
                snapshot;

        return current.namespaces()
                .contains(id.getNamespace())
                || current.items()
                .contains(id);
    }

    public static boolean isBlockBlocked(
            ResourceLocation id
    ) {
        if (id == null) {
            return true;
        }

        Snapshot current =
                snapshot;

        return current.namespaces()
                .contains(id.getNamespace())
                || current.blocks()
                .contains(id);
    }

    public static boolean isEntityBlocked(
            ResourceLocation id
    ) {
        if (id == null) {
            return true;
        }

        Snapshot current =
                snapshot;

        return current.namespaces()
                .contains(id.getNamespace())
                || current.entities()
                .contains(id);
    }

    public static boolean isRecipeBlocked(
            ResourceLocation id
    ) {
        if (id == null) {
            return true;
        }

        Snapshot current =
                snapshot;

        return current.namespaces()
                .contains(id.getNamespace())
                || current.recipes()
                .contains(id);
    }

    public static String summary() {
        Snapshot current =
                snapshot;

        return "namespaces="
                + current.namespaces().size()
                + ", items="
                + current.items().size()
                + ", blocks="
                + current.blocks().size()
                + ", entities="
                + current.entities().size()
                + ", recipes="
                + current.recipes().size();
    }

    public static Path file() {
        return FILE;
    }

    private record Snapshot(
            Set<String> namespaces,
            Set<ResourceLocation> items,
            Set<ResourceLocation> blocks,
            Set<ResourceLocation> entities,
            Set<ResourceLocation> recipes
    ) {

        private static Snapshot empty() {
            return new Snapshot(
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of()
            );
        }
    }
}