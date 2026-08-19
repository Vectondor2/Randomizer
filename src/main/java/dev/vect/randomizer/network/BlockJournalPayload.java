package dev.vect.randomizer.network;

import dev.vect.randomizer.Randomizer;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record BlockJournalPayload(
        long masterSeed,
        List<Entry> entries
) implements CustomPacketPayload {

    public BlockJournalPayload {
        entries = List.copyOf(entries);
    }

    public static final Type<
            BlockJournalPayload
            > TYPE =
            new Type<>(
                    ResourceLocation
                            .fromNamespaceAndPath(
                                    Randomizer.MOD_ID,
                                    "block_journal"
                            )
            );

    public static final StreamCodec<
            ByteBuf,
            BlockJournalPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    BlockJournalPayload::masterSeed,

                    Entry.LIST_CODEC,
                    BlockJournalPayload::entries,

                    BlockJournalPayload::new
            );

    @Override
    public Type<
            ? extends CustomPacketPayload
            > type() {

        return TYPE;
    }

    public record Entry(
            String blockId,
            String itemId
    ) {

        public static final StreamCodec<
                ByteBuf,
                Entry
                > STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8,
                        Entry::blockId,

                        ByteBufCodecs.STRING_UTF8,
                        Entry::itemId,

                        Entry::new
                );

        public static final StreamCodec<
                ByteBuf,
                List<Entry>
                > LIST_CODEC =
                STREAM_CODEC.apply(
                        ByteBufCodecs.list(
                                16384
                        )
                );
    }
}