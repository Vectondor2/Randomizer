package dev.vect.randomizer.network;

import dev.vect.randomizer.Randomizer;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record BestiaryPayload(
        long masterSeed,
        List<Entry> entries
) implements CustomPacketPayload {

    public BestiaryPayload {
        entries = List.copyOf(entries);
    }

    public static final Type<BestiaryPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            Randomizer.MOD_ID,
                            "bestiary"
                    )
            );

    public static final StreamCodec<
            ByteBuf,
            BestiaryPayload
            > STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    BestiaryPayload::masterSeed,

                    Entry.LIST_CODEC,
                    BestiaryPayload::entries,

                    BestiaryPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(
            String sourceId,
            String targetId,
            int kills,
            boolean discovered
    ) {

        public static final StreamCodec<
                ByteBuf,
                Entry
                > STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8,
                        Entry::sourceId,

                        ByteBufCodecs.STRING_UTF8,
                        Entry::targetId,

                        ByteBufCodecs.VAR_INT,
                        Entry::kills,

                        ByteBufCodecs.BOOL,
                        Entry::discovered,

                        Entry::new
                );

        public static final StreamCodec<
                ByteBuf,
                List<Entry>
                > LIST_CODEC =
                STREAM_CODEC.apply(
                        ByteBufCodecs.list(8192)
                );
    }
}