package com.nowayup.client;

import com.nowayup.NoWayUpMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = NoWayUpMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientFearHudOverlay {
    private static final int PANEL_WIDTH = 104;
    private static final int LINE_HEIGHT = 10;

    private ClientFearHudOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || !ClientFearHudState.visible()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int lines = ClientFearHudState.ending().isEmpty() ? 5 : 6;
        int panelHeight = 10 + lines * LINE_HEIGHT;
        int x = minecraft.getWindow().getGuiScaledWidth() - PANEL_WIDTH - 8;
        int y = 36;

        graphics.fill(x - 4, y - 4, x + PANEL_WIDTH, y + panelHeight, 0x88000000);
        graphics.fill(x - 4, y - 4, x + PANEL_WIDTH, y - 2, 0xAA5F1212);

        int textY = y;
        graphics.drawString(font, I18n.get("nowayup.hud.title"), x, textY, 0xD8D8D8, true);
        textY += LINE_HEIGHT + 1;
        graphics.drawString(font, I18n.get("nowayup.hud.fear", ClientFearHudState.fear()), x, textY, fearColor(ClientFearHudState.fear()), true);
        textY += LINE_HEIGHT;
        graphics.drawString(font, I18n.get("nowayup.hud.phase", translatePhase(ClientFearHudState.phase())), x, textY, 0xB8B8B8, true);
        textY += LINE_HEIGHT;
        graphics.drawString(font, I18n.get("nowayup.hud.layer", I18n.get(ClientFearHudState.mirror() ? "nowayup.layer.mirror" : "nowayup.layer.mine")), x, textY, 0xA7C7D9, true);
        textY += LINE_HEIGHT;
        graphics.drawString(font, I18n.get("nowayup.hud.false_exits", ClientFearHudState.fakeExits()), x, textY, 0xB8B8B8, true);
        if (!ClientFearHudState.ending().isEmpty()) {
            textY += LINE_HEIGHT;
            graphics.drawString(font, translateEnding(ClientFearHudState.ending()), x, textY, 0xFFE08A, true);
        }
    }

    private static String translatePhase(String phase) {
        return switch (phase) {
            case "Dawn" -> I18n.get("nowayup.phase.dawn");
            case "Mirror" -> I18n.get("nowayup.phase.mirror");
            case "Late" -> I18n.get("nowayup.phase.late");
            case "Middle" -> I18n.get("nowayup.phase.middle");
            default -> I18n.get("nowayup.phase.early");
        };
    }

    private static String translateEnding(String ending) {
        return switch (ending) {
            case "Ending: Dawn" -> I18n.get("nowayup.ending.dawn");
            case "Ending: Elias" -> I18n.get("nowayup.ending.elias");
            case "Ending: Seal" -> I18n.get("nowayup.ending.seal");
            case "Ending: Witness" -> I18n.get("nowayup.ending.witness");
            case "Ending: Replacement" -> I18n.get("nowayup.ending.replacement");
            case "Ending: Descent" -> I18n.get("nowayup.ending.descent");
            case "Ending: Loop" -> I18n.get("nowayup.ending.loop");
            default -> ending;
        };
    }

    private static int fearColor(int fear) {
        if (fear >= 220) {
            return 0xFF5555;
        }
        if (fear >= 120) {
            return 0xFFAA55;
        }
        if (fear >= 60) {
            return 0xFFFFAA;
        }
        return 0xAAAAAA;
    }
}
