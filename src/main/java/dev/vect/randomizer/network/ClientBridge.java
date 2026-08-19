package dev.vect.randomizer.network;

import java.util.Objects;
import java.util.function.Consumer;

public final class ClientBridge {

    private static Consumer<
            BestiaryPayload
            > bestiaryOpener =
            payload -> {
            };

    private static Consumer<
            BlockJournalPayload
            > blockJournalOpener =
            payload -> {
            };

    private ClientBridge() {
    }

    public static void setBestiaryOpener(
            Consumer<BestiaryPayload> opener
    ) {
        bestiaryOpener =
                Objects.requireNonNull(
                        opener
                );
    }

    public static void setBlockJournalOpener(
            Consumer<BlockJournalPayload> opener
    ) {
        blockJournalOpener =
                Objects.requireNonNull(
                        opener
                );
    }

    public static void openBestiary(
            BestiaryPayload payload
    ) {
        bestiaryOpener.accept(
                payload
        );
    }

    public static void openBlockJournal(
            BlockJournalPayload payload
    ) {
        blockJournalOpener.accept(
                payload
        );
    }
}