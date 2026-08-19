package dev.vect.randomizer;

import dev.vect.randomizer.client.BestiaryScreen;
import dev.vect.randomizer.client.BlockJournalScreen;
import dev.vect.randomizer.network.ClientBridge;

import net.minecraft.client.Minecraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(
        value = Randomizer.MOD_ID,
        dist = Dist.CLIENT
)
public final class RandomizerClient {

    public RandomizerClient(
            IEventBus modEventBus
    ) {
        ClientBridge.setBestiaryOpener(
                payload ->
                        Minecraft
                                .getInstance()
                                .setScreen(
                                        new BestiaryScreen(
                                                payload
                                        )
                                )
        );

        ClientBridge.setBlockJournalOpener(
                payload ->
                        Minecraft
                                .getInstance()
                                .setScreen(
                                        new BlockJournalScreen(
                                                payload
                                        )
                                )
        );
    }
}