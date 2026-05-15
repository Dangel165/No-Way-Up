package com.nowayup.system;

import com.nowayup.NoWayUpMod;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class NoWayUpAdvancementSystem {
    public static final String LOOP = "endings/loop";
    public static final String DESCENT = "endings/descent";
    public static final String REPLACEMENT = "endings/replacement";
    public static final String WITNESS = "endings/witness";
    public static final String SEAL = "endings/seal";
    public static final String ELIAS = "endings/elias";
    public static final String DAWN = "endings/dawn";

    private static final String ROOT = "root";
    private static final String CRITERION = "ending";

    private NoWayUpAdvancementSystem() {
    }

    public static void awardEnding(ServerPlayer player, String path) {
        award(player, ROOT);
        award(player, path);
    }

    private static void award(ServerPlayer player, String path) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(new ResourceLocation(NoWayUpMod.MOD_ID, path));
        if (advancement != null) {
            player.getAdvancements().award(advancement, CRITERION);
        }
    }
}
