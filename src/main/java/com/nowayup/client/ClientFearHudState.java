package com.nowayup.client;

import com.nowayup.network.FearHudPacket;

public final class ClientFearHudState {
    private static int fear;
    private static int fakeExits;
    private static int watcherSightings;
    private static boolean mirror;
    private static String phase = "";
    private static String ending = "";
    private static long lastUpdateMs;

    private ClientFearHudState() {
    }

    public static void update(FearHudPacket packet) {
        fear = packet.fear();
        fakeExits = packet.fakeExits();
        watcherSightings = packet.watcherSightings();
        mirror = packet.mirror();
        phase = packet.phase();
        ending = packet.ending();
        lastUpdateMs = System.currentTimeMillis();
    }

    public static boolean visible() {
        return lastUpdateMs > 0L && System.currentTimeMillis() - lastUpdateMs < 5000L;
    }

    public static int fear() {
        return fear;
    }

    public static int fakeExits() {
        return fakeExits;
    }

    public static int watcherSightings() {
        return watcherSightings;
    }

    public static boolean mirror() {
        return mirror;
    }

    public static String phase() {
        return phase;
    }

    public static String ending() {
        return ending;
    }
}
