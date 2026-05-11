package com.nowayup.system;

import com.nowayup.data.PlayerFearState;

public final class ForcedCrashSystem {
    private ForcedCrashSystem() {
    }

    public static boolean shouldTrigger(PlayerFearState state) {
        return !state.forcedCrashTriggered()
            && state.fearProgress() >= 145
            && state.reconnectCount() >= 1
            && state.fakeExitCount() >= 2;
    }

    public static void crash(PlayerFearState state) {
        state.setForcedCrashTriggered();
        state.addFear(20);
        throw new RuntimeException("No Way Up forced crash: There is no way up. Come back inside.");
    }
}
