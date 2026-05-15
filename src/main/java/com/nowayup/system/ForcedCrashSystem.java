package com.nowayup.system;

import com.nowayup.data.PlayerFearState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ForcedCrashSystem {
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
        if (player.server.getPlayerCount() > 1) {
            player.connection.disconnect(Component.literal("""
                Internal Exception: java.lang.IllegalStateException: There is no way up.

                The mine kept the others.
                Come back inside.
                """));
            return;
        }
        throw new RuntimeException("No Way Up forced crash: There is no way up. Come back inside.");
    }
}
