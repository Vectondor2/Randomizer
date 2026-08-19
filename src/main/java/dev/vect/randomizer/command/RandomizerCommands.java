package dev.vect.randomizer.command;

import com.mojang.brigadier.arguments.LongArgumentType;

import dev.vect.randomizer.block.BlockJournal;
import dev.vect.randomizer.config.RandomizerBlacklist;
import dev.vect.randomizer.core.RandomizerChannel;
import dev.vect.randomizer.core.RandomizerPools;
import dev.vect.randomizer.core.RandomizerWorldData;
import dev.vect.randomizer.diagnostic.RandomizerDiagnostics;
import dev.vect.randomizer.mob.Bestiary;
import dev.vect.randomizer.mob.MobLootRandomizer;
import dev.vect.randomizer.recipe.RecipeRandomizer;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class RandomizerCommands {

    private RandomizerCommands() {
    }

    public static void register(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher().register(
                Commands.literal(
                                "randomizer"
                        )

                        .then(
                                Commands.literal(
                                                "bestiary"
                                        )
                                        .executes(
                                                context -> {
                                                    ServerPlayer player =
                                                            context
                                                                    .getSource()
                                                                    .getPlayerOrException();

                                                    Bestiary.sendTo(
                                                            player
                                                    );

                                                    return 1;
                                                }
                                        )
                        )

                        .then(
                                Commands.literal(
                                                "blocks"
                                        )
                                        .executes(
                                                context -> {
                                                    ServerPlayer player =
                                                            context
                                                                    .getSource()
                                                                    .getPlayerOrException();

                                                    BlockJournal
                                                            .sendTo(
                                                                    player
                                                            );

                                                    return 1;
                                                }
                                        )
                        )

                        .then(
                                Commands.literal(
                                                "seed"
                                        )
                                        .executes(
                                                context ->
                                                        showSeed(
                                                                context
                                                                        .getSource()
                                                        )
                                        )
                        )

                        .then(
                                Commands.literal(
                                                "setseed"
                                        )
                                        .requires(
                                                source ->
                                                        source.hasPermission(
                                                                2
                                                        )
                                        )
                                        .then(
                                                Commands.argument(
                                                                "seed",
                                                                LongArgumentType
                                                                        .longArg()
                                                        )
                                                        .executes(
                                                                context ->
                                                                        setSeed(
                                                                                context
                                                                                        .getSource(),

                                                                                LongArgumentType
                                                                                        .getLong(
                                                                                                context,
                                                                                                "seed"
                                                                                        )
                                                                        )
                                                        )
                                        )
                        )

                        .then(
                                Commands.literal(
                                                "reroll"
                                        )
                                        .requires(
                                                source ->
                                                        source.hasPermission(
                                                                2
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "blocks"
                                                        )
                                                        .executes(
                                                                context ->
                                                                        reroll(
                                                                                context.getSource(),
                                                                                RandomizerChannel.BLOCKS
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "mobs"
                                                        )
                                                        .executes(
                                                                context ->
                                                                        reroll(
                                                                                context.getSource(),
                                                                                RandomizerChannel.MOBS
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "recipes"
                                                        )
                                                        .executes(
                                                                context ->
                                                                        reroll(
                                                                                context.getSource(),
                                                                                RandomizerChannel.RECIPES
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "fishing"
                                                        )
                                                        .executes(
                                                                context ->
                                                                        reroll(
                                                                                context.getSource(),
                                                                                RandomizerChannel.FISHING
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "trades"
                                                        )
                                                        .executes(
                                                                context ->
                                                                        reroll(
                                                                                context.getSource(),
                                                                                RandomizerChannel.TRADES
                                                                        )
                                                        )
                                        )

                                        .then(
                                                Commands.literal(
                                                                "all"
                                                        )
                                                        .executes(
                                                                context ->
                                                                        rerollAll(
                                                                                context.getSource()
                                                                        )
                                                        )
                                        )
                        )

                        .then(
                                Commands.literal(
                                                "reloadblacklist"
                                        )
                                        .requires(
                                                source ->
                                                        source.hasPermission(
                                                                2
                                                        )
                                        )
                                        .executes(
                                                context ->
                                                        reloadBlacklist(
                                                                context
                                                                        .getSource()
                                                        )
                                        )
                        )

                        .then(
                                Commands.literal(
                                                "diagnostics"
                                        )
                                        .requires(
                                                source ->
                                                        source.hasPermission(
                                                                2
                                                        )
                                        )
                                        .executes(
                                                context ->
                                                        diagnostics(
                                                                context
                                                                        .getSource()
                                                        )
                                        )
                        )
        );
    }

    private static int showSeed(
            CommandSourceStack source
    ) {
        RandomizerWorldData data =
                RandomizerWorldData.get(
                        source
                                .getServer()
                                .overworld()
                );

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Randomizer Seed: "
                                        + data.getMasterSeed()
                        ),
                false
        );

        return 1;
    }

    private static int setSeed(
            CommandSourceStack source,
            long seed
    ) {
        MinecraftServer server =
                source.getServer();

        RandomizerWorldData data =
                RandomizerWorldData.get(
                        server.overworld()
                );

        data.setMasterSeed(seed);

        MobLootRandomizer
                .invalidateCache();

        RecipeRandomizer.apply(
                server
        );

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Randomizer Seed установлен: "
                                        + seed
                        ),
                true
        );

        recipeSyncHint(
                source
        );

        return 1;
    }

    private static int reroll(
            CommandSourceStack source,
            RandomizerChannel channel
    ) {
        MinecraftServer server =
                source.getServer();

        RandomizerWorldData data =
                RandomizerWorldData.get(
                        server.overworld()
                );

        data.reroll(
                channel
        );

        if (channel
                == RandomizerChannel.MOBS) {

            MobLootRandomizer
                    .invalidateCache();
        }

        if (channel
                == RandomizerChannel.RECIPES) {

            RecipeRandomizer.apply(
                    server
            );

            recipeSyncHint(
                    source
            );
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Перегенерировано: "
                                        + channel.key()
                        ),
                true
        );

        /*
         * MerchantOffer уже существует
         * у старого жителя.
         *
         * Новый trade seed применяется
         * к предложениям, которые будут
         * созданы после reroll.
         */
        if (channel
                == RandomizerChannel.TRADES) {

            source.sendSuccess(
                    () ->
                            Component.literal(
                                    "Уже созданные предложения жителей могут сохраниться до обновления их торгов."
                            ),
                    false
            );
        }

        return 1;
    }

    private static int rerollAll(
            CommandSourceStack source
    ) {
        MinecraftServer server =
                source.getServer();

        RandomizerWorldData data =
                RandomizerWorldData.get(
                        server.overworld()
                );

        data.rerollAll();

        MobLootRandomizer
                .invalidateCache();

        RecipeRandomizer.apply(
                server
        );

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Все таблицы Randomizer перегенерированы."
                        ),
                true
        );

        recipeSyncHint(
                source
        );

        return 1;
    }

    private static int reloadBlacklist(
            CommandSourceStack source
    ) {
        boolean success =
                RandomizerBlacklist.reload();

        if (!success) {
            source.sendFailure(
                    Component.literal(
                            "Не удалось загрузить blacklist. Смотри latest.log."
                    )
            );

            return 0;
        }

        RandomizerPools
                .clearCaches();

        MobLootRandomizer
                .invalidateCache();

        RecipeRandomizer.apply(
                source.getServer()
        );

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Blacklist перезагружен: "
                                        + RandomizerBlacklist
                                        .summary()
                        ),
                true
        );

        recipeSyncHint(
                source
        );

        return 1;
    }

    private static int diagnostics(
            CommandSourceStack source
    ) {
        boolean success =
                RandomizerDiagnostics
                        .write(
                                source.getServer()
                        );

        if (!success) {
            source.sendFailure(
                    Component.literal(
                            "Не удалось создать диагностический отчёт."
                    )
            );

            return 0;
        }

        source.sendSuccess(
                () ->
                        Component.literal(
                                "Диагностика сохранена: "
                                        + RandomizerDiagnostics
                                        .file()
                        ),
                false
        );

        return 1;
    }

    private static void recipeSyncHint(
            CommandSourceStack source
    ) {
        source.sendSuccess(
                () ->
                        Component.literal(
                                "Для полной синхронизации рецептов клиента используй /reload."
                        ),
                false
        );
    }
}