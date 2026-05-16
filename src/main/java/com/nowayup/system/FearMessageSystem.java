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
        int lineIndex = Math.floorMod(index, WHISPERS.size());
        player.displayClientMessage(Component.translatable("nowayup.message.whisper", Component.translatable("nowayup.whisper." + lineIndex)).withStyle(ChatFormatting.DARK_GRAY), true);
    }

    public static void nameCall(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("nowayup.message.whisper", player.getGameProfile().getName()).withStyle(ChatFormatting.DARK_RED), true);
    }

    public static void watcherSeen(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("nowayup.message.watcher_seen").withStyle(ChatFormatting.GRAY), true);
    }

    public static void fakeExit(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("nowayup.message.no_way_up").withStyle(ChatFormatting.DARK_RED), true);
    }
}
