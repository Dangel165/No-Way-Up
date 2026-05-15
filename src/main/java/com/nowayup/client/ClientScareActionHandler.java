package com.nowayup.client;

import com.nowayup.system.ExternalTextScareSystem;
import net.minecraft.client.Minecraft;

public final class ClientScareActionHandler {
    private ClientScareActionHandler() {
    }

    public static void createDesktopMessage() {
        ExternalTextScareSystem.createAndOpenDesktopMessage();
    }

    public static void crashClient() {
        Minecraft.getInstance().execute(() -> {
            throw new RuntimeException("No Way Up forced client crash: There is no way up. Come back inside.");
        });
    }
}
