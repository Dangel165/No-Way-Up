package com.nowayup.system;

import com.nowayup.data.PlayerFearState;
import com.nowayup.network.NoWayUpNetwork;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ForcedCrashSystem {
    private ForcedCrashSystem() {
    }

    public static boolean triggerEventCrash(ServerPlayer player, PlayerFearState state) {
        if (state.forcedCrashTriggered()) {
            return false;
        }
        state.setForcedCrashTriggered();
        state.addFear(20);
        NoWayUpNetwork.sendClientCrash(player);
        return true;
    }

    public static int disconnectEveryone(MinecraftServer server) {
        List<ServerPlayer> players = List.copyOf(server.getPlayerList().getPlayers());
        for (ServerPlayer player : players) {
            NoWayUpNetwork.sendClientCrash(player);
        }
        return players.size();
    }
}
