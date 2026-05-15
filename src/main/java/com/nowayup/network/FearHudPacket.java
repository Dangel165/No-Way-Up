package com.nowayup.network;

import com.nowayup.client.ClientFearHudState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record FearHudPacket(int fear, int fakeExits, int watcherSightings, boolean mirror, String phase, String ending) {
    public static void encode(FearHudPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.fear);
        buffer.writeVarInt(packet.fakeExits);
        buffer.writeVarInt(packet.watcherSightings);
        buffer.writeBoolean(packet.mirror);
        buffer.writeUtf(packet.phase);
        buffer.writeUtf(packet.ending);
    }

    public static FearHudPacket decode(FriendlyByteBuf buffer) {
        return new FearHudPacket(
            buffer.readVarInt(),
            buffer.readVarInt(),
            buffer.readVarInt(),
            buffer.readBoolean(),
            buffer.readUtf(),
            buffer.readUtf()
        );
    }

    public static void handle(FearHudPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFearHudState.update(packet)));
        context.setPacketHandled(true);
    }
}
