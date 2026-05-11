package com.nowayup.system;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class FearMessageSystem {
    private static final List<String> WHISPERS = List.of(
        "Go back.",
        "Up is down.",
        "You already came below.",
        "Why did you stop?",
        "I can hear you.",
        "Do not open the door. It is already open.",
        "That was not you passing by."
    );

    private FearMessageSystem() {
    }

    public static void whisper(ServerPlayer player, int index) {
        String line = WHISPERS.get(Math.floorMod(index, WHISPERS.size()));
        player.displayClientMessage(Component.literal("??? whispers: " + line).withStyle(ChatFormatting.DARK_GRAY), true);
    }

    public static void nameCall(ServerPlayer player) {
        player.displayClientMessage(Component.literal("??? whispers: " + player.getGameProfile().getName()).withStyle(ChatFormatting.DARK_RED), true);
    }

    public static void watcherSeen(ServerPlayer player) {
        player.displayClientMessage(Component.literal("You saw it too, didn't you?").withStyle(ChatFormatting.GRAY), true);
    }

    public static void fakeExit(ServerPlayer player) {
        player.displayClientMessage(Component.literal("There is no way up.").withStyle(ChatFormatting.DARK_RED), true);
    }
}
