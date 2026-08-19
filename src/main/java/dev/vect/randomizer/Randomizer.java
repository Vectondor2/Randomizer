package dev.vect.randomizer;

import com.mojang.logging.LogUtils;

import dev.vect.randomizer.block.BlockRandomizer;
import dev.vect.randomizer.command.RandomizerCommands;
import dev.vect.randomizer.config.RandomizerBlacklist;
import dev.vect.randomizer.core.RandomizerWorldData;
import dev.vect.randomizer.diagnostic.RandomizerDiagnostics;
import dev.vect.randomizer.fishing.FishingRandomizerRegistry;
import dev.vect.randomizer.mob.MobLootRandomizer;
import dev.vect.randomizer.network.RandomizerNetworking;
import dev.vect.randomizer.recipe.RecipeRandomizer;
import dev.vect.randomizer.recipe.RecipeRandomizerRegistry;
import dev.vect.randomizer.trade.TradeRandomizer;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import org.slf4j.Logger;

@Mod(Randomizer.MOD_ID)
public class Randomizer {

    public static final String MOD_ID =
            "randomizer";

    public static final Logger LOGGER =
            LogUtils.getLogger();

    public Randomizer(
            IEventBus modEventBus
    ) {
        /*
         * Config.
         */
        RandomizerBlacklist.reload();

        /*
         * Registries.
         */
        RecipeRandomizerRegistry.register(
                modEventBus
        );

        FishingRandomizerRegistry.register(
                modEventBus
        );

        /*
         * Gameplay.
         */
        BlockRandomizer.register();
        MobLootRandomizer.register();
        RecipeRandomizer.register();
        TradeRandomizer.register();

        /*
         * Networking.
         */
        modEventBus.addListener(
                RandomizerNetworking::register
        );

        /*
         * Commands.
         */
        NeoForge.EVENT_BUS.addListener(
                RandomizerCommands::register
        );

        /*
         * World.
         */
        NeoForge.EVENT_BUS.addListener(
                Randomizer::onServerStarted
        );
    }

    private static void onServerStarted(
            ServerStartedEvent event
    ) {
        RandomizerWorldData data =
                RandomizerWorldData.get(
                        event.getServer()
                                .overworld()
                );

        LOGGER.info(
                "Universal Randomizer world seed: {}",
                data.getMasterSeed()
        );

        /*
         * Маленький общий отчёт создаётся
         * автоматически при запуске мира.
         */
        RandomizerDiagnostics.write(
                event.getServer()
        );
    }
}