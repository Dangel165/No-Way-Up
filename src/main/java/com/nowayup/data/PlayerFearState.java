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
    private boolean witnessEndingComplete;
    private boolean sealEndingComplete;
    private boolean eliasEndingComplete;
    private boolean eliasAscentComplete;
    private boolean happyEndingComplete;
    private long nextAudioTick;
    private long nextWhisperTick;
    private long nextWatcherTick;
    private long minuteProgressTick;
    private long mirrorStartTick;
    private long nextMirrorFootstepTick;
    private long wakeSequenceStartTick;
    private int wakeSequenceStage;

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
        state.witnessEndingComplete = tag.getBoolean("WitnessEndingComplete");
        state.sealEndingComplete = tag.getBoolean("SealEndingComplete");
        state.eliasEndingComplete = tag.getBoolean("EliasEndingComplete");
        state.eliasAscentComplete = tag.getBoolean("EliasAscentComplete");
        state.happyEndingComplete = tag.getBoolean("HappyEndingComplete");
        state.nextAudioTick = tag.getLong("NextAudioTick");
        state.nextWhisperTick = tag.getLong("NextWhisperTick");
        state.nextWatcherTick = tag.getLong("NextWatcherTick");
        state.minuteProgressTick = tag.getLong("MinuteProgressTick");
        state.mirrorStartTick = tag.getLong("MirrorStartTick");
        state.nextMirrorFootstepTick = tag.getLong("NextMirrorFootstepTick");
        state.wakeSequenceStartTick = tag.getLong("WakeSequenceStartTick");
        state.wakeSequenceStage = tag.getInt("WakeSequenceStage");
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
        tag.putBoolean("WitnessEndingComplete", witnessEndingComplete);
        tag.putBoolean("SealEndingComplete", sealEndingComplete);
        tag.putBoolean("EliasEndingComplete", eliasEndingComplete);
        tag.putBoolean("EliasAscentComplete", eliasAscentComplete);
        tag.putBoolean("HappyEndingComplete", happyEndingComplete);
        tag.putLong("NextAudioTick", nextAudioTick);
        tag.putLong("NextWhisperTick", nextWhisperTick);
        tag.putLong("NextWatcherTick", nextWatcherTick);
        tag.putLong("MinuteProgressTick", minuteProgressTick);
        tag.putLong("MirrorStartTick", mirrorStartTick);
        tag.putLong("NextMirrorFootstepTick", nextMirrorFootstepTick);
        tag.putLong("WakeSequenceStartTick", wakeSequenceStartTick);
        tag.putInt("WakeSequenceStage", wakeSequenceStage);
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

    public void resetFakeExitCount() {
        fakeExitCount = 0;
    }

    public int watcherSightings() {
        return watcherSightings;
    }

    public void incrementWatcherSightings() {
        watcherSightings++;
    }

    public void resetWatcherSightings() {
        watcherSightings = 0;
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

    public void resetForcedCrashTriggered() {
        forcedCrashTriggered = false;
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

    public void resetLoopEndingComplete() {
        loopEndingComplete = false;
    }

    public boolean descentEndingComplete() {
        return descentEndingComplete;
    }

    public void setDescentEndingComplete() {
        descentEndingComplete = true;
    }

    public void resetDescentEndingComplete() {
        descentEndingComplete = false;
    }

    public boolean replacementEndingComplete() {
        return replacementEndingComplete;
    }

    public void setReplacementEndingComplete() {
        replacementEndingComplete = true;
    }

    public void resetReplacementEndingComplete() {
        replacementEndingComplete = false;
    }

    public boolean witnessEndingComplete() {
        return witnessEndingComplete;
    }

    public void setWitnessEndingComplete() {
        witnessEndingComplete = true;
    }

    public void resetWitnessEndingComplete() {
        witnessEndingComplete = false;
    }

    public boolean sealEndingComplete() {
        return sealEndingComplete;
    }

    public void setSealEndingComplete() {
        sealEndingComplete = true;
    }

    public void resetSealEndingComplete() {
        sealEndingComplete = false;
    }

    public boolean eliasEndingComplete() {
        return eliasEndingComplete;
    }

    public void setEliasEndingComplete() {
        eliasEndingComplete = true;
    }

    public void resetEliasEndingComplete() {
        eliasEndingComplete = false;
        eliasAscentComplete = false;
    }

    public boolean eliasAscentComplete() {
        return eliasAscentComplete;
    }

    public void setEliasAscentComplete() {
        eliasAscentComplete = true;
    }

    public boolean happyEndingComplete() {
        return happyEndingComplete;
    }

    public void setHappyEndingComplete() {
        happyEndingComplete = true;
    }

    public void resetHappyEndingComplete() {
        happyEndingComplete = false;
    }

    public void resetAllEndings() {
        setMirrorEntered(false);
        setCollapseStage(0);
        setMirrorEventStage(0);
        resetLoopEndingComplete();
        resetDescentEndingComplete();
        resetReplacementEndingComplete();
        resetWitnessEndingComplete();
        resetSealEndingComplete();
        resetEliasEndingComplete();
        resetHappyEndingComplete();
    }

    public void resetRunAfterDeath(long gameTime) {
        resetFakeExitCount();
        resetWatcherSightings();
        resetForcedCrashTriggered();
        setMirrorEntered(false);
        setCollapseStage(0);
        setMirrorEventStage(0);
        resetLoopEndingComplete();
        resetDescentEndingComplete();
        resetReplacementEndingComplete();
        resetWitnessEndingComplete();
        resetSealEndingComplete();
        resetEliasEndingComplete();
        resetEventTimers(gameTime);
        startWakeSequence(gameTime);
    }

    public void startWakeSequence(long gameTime) {
        wakeSequenceStartTick = gameTime;
        wakeSequenceStage = 0;
    }

    public long wakeSequenceStartTick() {
        return wakeSequenceStartTick;
    }

    public int wakeSequenceStage() {
        return wakeSequenceStage;
    }

    public void setWakeSequenceStage(int wakeSequenceStage) {
        this.wakeSequenceStage = Math.max(0, wakeSequenceStage);
    }

    public void stopWakeSequence() {
        wakeSequenceStartTick = 0L;
        wakeSequenceStage = 99;
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

    public void resetEventTimers(long gameTime) {
        nextAudioTick = gameTime + 20L;
        nextWhisperTick = gameTime + 60L;
        nextWatcherTick = gameTime + 120L;
        minuteProgressTick = gameTime + 1200L;
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
