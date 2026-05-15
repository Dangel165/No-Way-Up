package com.nowayup.network;

import com.nowayup.client.ClientScareActionHandler;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record ScareActionPacket(int action) {
    public static final int DESKTOP_MESSAGE = 1;
    public static final int CLIENT_CRASH = 2;

    public static ScareActionPacket desktopMessage() {
        return new ScareActionPacket(DESKTOP_MESSAGE);
    }

    public static ScareActionPacket clientCrash() {
        return new ScareActionPacket(CLIENT_CRASH);
    }

    public static void encode(ScareActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.action);
    }

    public static ScareActionPacket decode(FriendlyByteBuf buffer) {
        return new ScareActionPacket(buffer.readVarInt());
    }

    public static void handle(ScareActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(packet.action)));
        context.setPacketHandled(true);
    }

    private static void handleClient(int action) {
        if (action == DESKTOP_MESSAGE) {
            ClientScareActionHandler.createDesktopMessage();
        } else if (action == CLIENT_CRASH) {
            ClientScareActionHandler.crashClient();
        }
    }
}
