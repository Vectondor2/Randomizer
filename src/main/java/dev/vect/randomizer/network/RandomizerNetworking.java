package dev.vect.randomizer.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class RandomizerNetworking {

    private static final String
            NETWORK_VERSION =
            "1";

    private RandomizerNetworking() {
    }

    public static void register(
            RegisterPayloadHandlersEvent event
    ) {
        PayloadRegistrar registrar =
                event.registrar(
                        NETWORK_VERSION
                );

        registrar.playToClient(
                BestiaryPayload.TYPE,
                BestiaryPayload.STREAM_CODEC,
                RandomizerNetworking
                        ::handleBestiary
        );

        registrar.playToClient(
                BlockJournalPayload.TYPE,
                BlockJournalPayload.STREAM_CODEC,
                RandomizerNetworking
                        ::handleBlockJournal
        );
    }

    private static void handleBestiary(
            BestiaryPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () ->
                        ClientBridge
                                .openBestiary(
                                        payload
                                )
        );
    }

    private static void handleBlockJournal(
            BlockJournalPayload payload,
            IPayloadContext context
    ) {
        context.enqueueWork(
                () ->
                        ClientBridge
                                .openBlockJournal(
                                        payload
                                )
        );
    }
}