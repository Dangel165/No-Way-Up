package com.nowayup.system;

import com.nowayup.data.PlayerFearState;
import com.nowayup.network.NoWayUpNetwork;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ForcedCrashSystem {
    private static final Component CRASH_MESSAGE = Component.literal("""
        Internal Exception: java.lang.IllegalStateException: There is no way up.

        The mine kept the others.
        Come back inside.
        """);

    private ForcedCrashSystem() {
    }

    public static boolean shouldTrigger(PlayerFearState state) {
        return !state.forcedCrashTriggered()
            && state.fearProgress() >= 145
            && state.reconnectCount() >= 1
            && state.fakeExitCount() >= 2;
    }

    public static void crashOrDisconnect(ServerPlayer player, PlayerFearState state) {
        state.setForcedCrashTriggered();
        state.addFear(20);
        NoWayUpNetwork.sendClientCrash(player);
    }

    public static int disconnectEveryone(MinecraftServer server) {
        List<ServerPlayer> players = List.copyOf(server.getPlayerList().getPlayers());
        for (ServerPlayer player : players) {
            NoWayUpNetwork.sendClientCrash(player);
        }
        return players.size();
    }
}
