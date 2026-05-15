package com.nowayup.network;

import com.nowayup.NoWayUpMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NoWayUpNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static int packetId;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(NoWayUpMod.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private NoWayUpNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(
            packetId++,
            FearHudPacket.class,
            FearHudPacket::encode,
            FearHudPacket::decode,
            FearHudPacket::handle
        );
    }

    public static void sendFearHud(ServerPlayer player, FearHudPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
