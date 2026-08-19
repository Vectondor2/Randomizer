package dev.vect.randomizer.diagnostic;

import dev.vect.randomizer.Randomizer;
import dev.vect.randomizer.config.RandomizerBlacklist;
import dev.vect.randomizer.core.RandomizerChannel;
import dev.vect.randomizer.core.RandomizerPools;
import dev.vect.randomizer.core.RandomizerWorldData;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;

public final class RandomizerDiagnostics {

    private static final Path DIRECTORY =
            Path.of(
                    "logs",
                    "randomizer"
            );

    private static final Path FILE =
            DIRECTORY.resolve(
                    "status.txt"
            );

    private RandomizerDiagnostics() {
    }

    public static boolean write(
            MinecraftServer server
    ) {
        RandomizerWorldData data =
                RandomizerWorldData.get(
                        server.overworld()
                );

        ArrayList<String> lines =
                new ArrayList<>();

        lines.add(
                "=============================================="
        );

        lines.add(
                "      UNIVERSAL RANDOMIZER - STATUS"
        );

        lines.add(
                "=============================================="
        );

        lines.add("");

        lines.add(
                "Master Seed: "
                        + data.getMasterSeed()
        );

        lines.add("");

        lines.add(
                "Blocks Seed:  "
                        + data.getSeed(
                        RandomizerChannel.BLOCKS
                )
        );

        lines.add(
                "Mobs Seed:    "
                        + data.getSeed(
                        RandomizerChannel.MOBS
                )
        );

        lines.add(
                "Recipes Seed: "
                        + data.getSeed(
                        RandomizerChannel.RECIPES
                )
        );

        lines.add(
                "Fishing Seed: "
                        + data.getSeed(
                        RandomizerChannel.FISHING
                )
        );

        lines.add(
                "Trades Seed:  "
                        + data.getSeed(
                        RandomizerChannel.TRADES
                )
        );

        lines.add("");

        lines.add(
                "Registered blocks: "
                        + BuiltInRegistries.BLOCK
                        .size()
        );

        lines.add(
                "Randomizer item pool: "
                        + RandomizerPools
                        .items()
                        .size()
        );

        lines.add(
                "Randomizer mob pool: "
                        + RandomizerPools
                        .mobs()
                        .size()
        );

        lines.add("");

        lines.add(
                "Discovered blocks: "
                        + data.getDiscoveredBlocks()
                        .size()
        );

        lines.add(
                "Discovered mobs: "
                        + data.getDiscoveredMobs()
                        .size()
        );

        int totalKills =
                data.getMobKills()
                        .values()
                        .stream()
                        .mapToInt(
                                Integer::intValue
                        )
                        .sum();

        lines.add(
                "Tracked mob kills: "
                        + totalKills
        );

        lines.add("");

        lines.add(
                "Blacklist: "
                        + RandomizerBlacklist
                        .summary()
        );

        lines.add("");

        lines.add(
                "Recipe details:"
        );

        lines.add(
                "See logs/randomizer/recipes.txt"
        );

        try {
            Files.createDirectories(
                    DIRECTORY
            );

            Files.write(
                    FILE,
                    List.copyOf(lines),
                    StandardCharsets.UTF_8
            );

            Randomizer.LOGGER.info(
                    "Randomizer diagnostics written to {}",
                    FILE.toAbsolutePath()
            );

            return true;

        } catch (IOException exception) {
            Randomizer.LOGGER.error(
                    "Could not write Randomizer diagnostics.",
                    exception
            );

            return false;
        }
    }

    public static Path file() {
        return FILE;
    }
}