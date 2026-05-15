package com.nowayup.data;

import net.minecraft.nbt.CompoundTag;

public class PlayerFearState {
    private int fearProgress;
    private int reconnectCount;
    private int fakeExitCount;
    private int watcherSightings;
    private int environmentMutationCount;
    private int collapseStage;
    private int mirrorEventStage;
    private boolean firstSpawnComplete;
    private boolean desktopMessageCreated;
    private boolean forcedCrashTriggered;
    private boolean mirrorEntered;
    private boolean loopEndingComplete;
    private boolean descentEndingComplete;
    private boolean replacementEndingComplete;
    private long nextAudioTick;
    private long nextWhisperTick;
    private long nextWatcherTick;
    private long minuteProgressTick;
    private long mirrorStartTick;
    private long nextMirrorFootstepTick;

    public static PlayerFearState load(CompoundTag tag) {
        PlayerFearState state = new PlayerFearState();
        state.fearProgress = tag.getInt("FearProgress");
        state.reconnectCount = tag.getInt("ReconnectCount");
        state.fakeExitCount = tag.getInt("FakeExitCount");
        state.watcherSightings = tag.getInt("WatcherSightings");
        state.environmentMutationCount = tag.getInt("EnvironmentMutationCount");
        state.collapseStage = tag.getInt("CollapseStage");
        state.mirrorEventStage = tag.getInt("MirrorEventStage");
        state.firstSpawnComplete = tag.getBoolean("FirstSpawnComplete");
        state.desktopMessageCreated = tag.getBoolean("DesktopMessageCreated");
        state.forcedCrashTriggered = tag.getBoolean("ForcedCrashTriggered");
        state.mirrorEntered = tag.getBoolean("MirrorEntered");
        state.loopEndingComplete = tag.getBoolean("LoopEndingComplete");
        state.descentEndingComplete = tag.getBoolean("DescentEndingComplete");
        state.replacementEndingComplete = tag.getBoolean("ReplacementEndingComplete");
        state.nextAudioTick = tag.getLong("NextAudioTick");
        state.nextWhisperTick = tag.getLong("NextWhisperTick");
        state.nextWatcherTick = tag.getLong("NextWatcherTick");
        state.minuteProgressTick = tag.getLong("MinuteProgressTick");
        state.mirrorStartTick = tag.getLong("MirrorStartTick");
        state.nextMirrorFootstepTick = tag.getLong("NextMirrorFootstepTick");
        return state;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("FearProgress", fearProgress);
        tag.putInt("ReconnectCount", reconnectCount);
        tag.putInt("FakeExitCount", fakeExitCount);
        tag.putInt("WatcherSightings", watcherSightings);
        tag.putInt("EnvironmentMutationCount", environmentMutationCount);
        tag.putInt("CollapseStage", collapseStage);
        tag.putInt("MirrorEventStage", mirrorEventStage);
        tag.putBoolean("FirstSpawnComplete", firstSpawnComplete);
        tag.putBoolean("DesktopMessageCreated", desktopMessageCreated);
        tag.putBoolean("ForcedCrashTriggered", forcedCrashTriggered);
        tag.putBoolean("MirrorEntered", mirrorEntered);
        tag.putBoolean("LoopEndingComplete", loopEndingComplete);
        tag.putBoolean("DescentEndingComplete", descentEndingComplete);
        tag.putBoolean("ReplacementEndingComplete", replacementEndingComplete);
        tag.putLong("NextAudioTick", nextAudioTick);
        tag.putLong("NextWhisperTick", nextWhisperTick);
        tag.putLong("NextWatcherTick", nextWatcherTick);
        tag.putLong("MinuteProgressTick", minuteProgressTick);
        tag.putLong("MirrorStartTick", mirrorStartTick);
        tag.putLong("NextMirrorFootstepTick", nextMirrorFootstepTick);
        return tag;
    }

    public void addFear(int amount) {
        fearProgress = Math.max(0, fearProgress + amount);
    }

    public void setFearProgress(int fearProgress) {
        this.fearProgress = Math.max(0, fearProgress);
    }

    public int fearProgress() {
        return fearProgress;
    }

    public int reconnectCount() {
        return reconnectCount;
    }

    public void incrementReconnectCount() {
        reconnectCount++;
    }

    public int fakeExitCount() {
        return fakeExitCount;
    }

    public void incrementFakeExitCount() {
        fakeExitCount++;
    }

    public int watcherSightings() {
        return watcherSightings;
    }

    public void incrementWatcherSightings() {
        watcherSightings++;
    }

    public int environmentMutationCount() {
        return environmentMutationCount;
    }

    public void incrementEnvironmentMutationCount() {
        environmentMutationCount++;
    }

    public int collapseStage() {
        return collapseStage;
    }

    public void setCollapseStage(int collapseStage) {
        this.collapseStage = Math.max(0, collapseStage);
    }

    public int mirrorEventStage() {
        return mirrorEventStage;
    }

    public void setMirrorEventStage(int mirrorEventStage) {
        this.mirrorEventStage = Math.max(0, mirrorEventStage);
    }

    public boolean firstSpawnComplete() {
        return firstSpawnComplete;
    }

    public void setFirstSpawnComplete() {
        firstSpawnComplete = true;
    }

    public boolean desktopMessageCreated() {
        return desktopMessageCreated;
    }

    public void setDesktopMessageCreated() {
        desktopMessageCreated = true;
    }

    public boolean forcedCrashTriggered() {
        return forcedCrashTriggered;
    }

    public void setForcedCrashTriggered() {
        forcedCrashTriggered = true;
    }

    public boolean mirrorEntered() {
        return mirrorEntered;
    }

    public void setMirrorEntered(boolean mirrorEntered) {
        this.mirrorEntered = mirrorEntered;
    }

    public boolean loopEndingComplete() {
        return loopEndingComplete;
    }

    public void setLoopEndingComplete() {
        loopEndingComplete = true;
    }

    public boolean descentEndingComplete() {
        return descentEndingComplete;
    }

    public void setDescentEndingComplete() {
        descentEndingComplete = true;
    }

    public boolean replacementEndingComplete() {
        return replacementEndingComplete;
    }

    public void setReplacementEndingComplete() {
        replacementEndingComplete = true;
    }

    public long nextAudioTick() {
        return nextAudioTick;
    }

    public void setNextAudioTick(long nextAudioTick) {
        this.nextAudioTick = nextAudioTick;
    }

    public long nextWhisperTick() {
        return nextWhisperTick;
    }

    public void setNextWhisperTick(long nextWhisperTick) {
        this.nextWhisperTick = nextWhisperTick;
    }

    public long nextWatcherTick() {
        return nextWatcherTick;
    }

    public void setNextWatcherTick(long nextWatcherTick) {
        this.nextWatcherTick = nextWatcherTick;
    }

    public long minuteProgressTick() {
        return minuteProgressTick;
    }

    public void setMinuteProgressTick(long minuteProgressTick) {
        this.minuteProgressTick = minuteProgressTick;
    }

    public long mirrorStartTick() {
        return mirrorStartTick;
    }

    public void setMirrorStartTick(long mirrorStartTick) {
        this.mirrorStartTick = mirrorStartTick;
    }

    public long nextMirrorFootstepTick() {
        return nextMirrorFootstepTick;
    }

    public void setNextMirrorFootstepTick(long nextMirrorFootstepTick) {
        this.nextMirrorFootstepTick = nextMirrorFootstepTick;
    }
}
