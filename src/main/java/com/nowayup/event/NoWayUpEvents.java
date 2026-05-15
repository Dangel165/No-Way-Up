package com.nowayup.event;

import com.nowayup.data.FearProgressSavedData;
import com.nowayup.data.PlayerFearState;
import com.nowayup.system.EnvironmentMutationSystem;
import com.nowayup.system.ExternalTextScareSystem;
import com.nowayup.system.FearMessageSystem;
import com.nowayup.system.ForcedCrashSystem;
import com.nowayup.system.LoreBookSystem;
import com.nowayup.system.MirrorMineSystem;
import com.nowayup.system.MineshaftPrisonSystem;
import com.nowayup.system.WatcherIllusionSystem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class NoWayUpEvents {
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("nowayup")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("fear")
                    .then(Commands.literal("get")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            PlayerFearState state = FearProgressSavedData.get(dataLevel(player)).stateFor(player.getUUID());
                            context.getSource().sendSuccess(() -> Component.literal("No Way Up fear: " + state.fearProgress()), false);
                            return state.fearProgress();
                        }))
                    .then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                FearProgressSavedData data = FearProgressSavedData.get(dataLevel(player));
                                PlayerFearState state = data.stateFor(player.getUUID());
                                state.addFear(IntegerArgumentType.getInteger(context, "amount"));
                                applyDebugFearEffects(player, state);
                                data.setDirty();
                                context.getSource().sendSuccess(() -> Component.literal("No Way Up fear: " + state.fearProgress()), false);
                                return state.fearProgress();
                            })))
                    .then(Commands.literal("set")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                FearProgressSavedData data = FearProgressSavedData.get(dataLevel(player));
                                PlayerFearState state = data.stateFor(player.getUUID());
                                state.setFearProgress(IntegerArgumentType.getInteger(context, "amount"));
                                applyDebugFearEffects(player, state);
                                data.setDirty();
                                context.getSource().sendSuccess(() -> Component.literal("No Way Up fear: " + state.fearProgress()), false);
                                return state.fearProgress();
                            }))))
                .then(Commands.literal("desktop")
                    .executes(context -> {
                        boolean created = ExternalTextScareSystem.createDesktopMessage();
                        context.getSource().sendSuccess(() -> Component.literal(created ? "Desktop message created." : "Desktop message failed."), false);
                        return created ? 1 : 0;
                    }))
                .then(Commands.literal("crash")
                    .executes(context -> {
                        int kicked = ForcedCrashSystem.disconnectEveryone(context.getSource().getServer());
                        context.getSource().sendSuccess(() -> Component.literal("No Way Up disconnected " + kicked + " player(s)."), true);
                        return kicked;
                    }))
                .then(Commands.literal("watcher")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        boolean spawned = WatcherIllusionSystem.spawnWatcher(player);
                        if (spawned) {
                            FearProgressSavedData data = FearProgressSavedData.get(dataLevel(player));
                            PlayerFearState state = data.stateFor(player.getUUID());
                            state.incrementWatcherSightings();
                            state.addFear(20);
                            FearMessageSystem.watcherSeen(player);
                            MirrorMineSystem.triggerReplacementEnding(player, state);
                            data.setDirty();
                        }
                        context.getSource().sendSuccess(() -> Component.literal(spawned ? "Watcher spawned." : "Watcher failed."), false);
                        return spawned ? 1 : 0;
                    }))
                .then(Commands.literal("lore")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        LoreBookSystem.giveLoreBooks(player);
                        context.getSource().sendSuccess(() -> Component.literal("Lore books added."), false);
                        return 1;
                    }))
                .then(Commands.literal("mutate")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        FearProgressSavedData data = FearProgressSavedData.get(dataLevel(player));
                        PlayerFearState state = data.stateFor(player.getUUID());
                        state.incrementReconnectCount();
                        EnvironmentMutationSystem.mutateOnReconnect(player.serverLevel(), state);
                        data.setDirty();
                        context.getSource().sendSuccess(() -> Component.literal("Environment mutated."), false);
                        return 1;
                    }))
                .then(Commands.literal("mirror")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        FearProgressSavedData data = FearProgressSavedData.get(dataLevel(player));
                        PlayerFearState state = data.stateFor(player.getUUID());
                        MirrorMineSystem.enterMirror(player, state);
                        data.setDirty();
                        context.getSource().sendSuccess(() -> Component.literal("Entered the mirror mine."), false);
                        return 1;
                    }))
                .then(Commands.literal("start")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        MineshaftPrisonSystem.buildStartingChamber(player.serverLevel());
                        MineshaftPrisonSystem.sendToStart(player);
                        context.getSource().sendSuccess(() -> Component.literal("Sent to the mine."), false);
                        return 1;
                    }))
        );
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Monster monster
            && event.getLevel() instanceof ServerLevel level
            && (level.dimension().equals(MirrorMineSystem.MIRROR_LEVEL) || MirrorMineSystem.isInsideMirrorRegion(monster.blockPosition()))) {
            event.setCanceled(true);
            monster.discard();
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = dataLevel(player);
        FearProgressSavedData data = FearProgressSavedData.get(level);
        PlayerFearState state = data.stateFor(player.getUUID());

        if (!state.firstSpawnComplete()) {
            if (!data.worldInitialized()) {
                MineshaftPrisonSystem.buildStartingChamber(level);
                data.setWorldInitialized();
            }
            MineshaftPrisonSystem.buildNoSurfaceColumn(level);
            MineshaftPrisonSystem.updateSupplyChest(level);
            player.teleportTo(level, MineshaftPrisonSystem.START_POS.getX() + 0.5, MineshaftPrisonSystem.START_POS.getY(), MineshaftPrisonSystem.START_POS.getZ() + 0.5, player.getYRot(), player.getXRot());
            state.setFirstSpawnComplete();
            state.setMinuteProgressTick(level.getGameTime() + 1200L);
        } else {
            MineshaftPrisonSystem.buildNoSurfaceColumn(level);
            MineshaftPrisonSystem.updateSupplyChest(level);
            state.incrementReconnectCount();
            state.addFear(15);
            if (state.fearProgress() >= 100 || state.reconnectCount() >= 1) {
                EnvironmentMutationSystem.mutateOnReconnect(level, state);
            }
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Why did you come back?"), true);
        }

        data.setDirty();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        long gameTime = level.getGameTime();
        FearProgressSavedData data = FearProgressSavedData.get(dataLevel(player));
        PlayerFearState state = data.stateFor(player.getUUID());

        if (redirectFalseEscape(player, state)) {
            data.setDirty();
            return;
        }

        if (gameTime % 20L != 0L) {
            cleanupWatchers(player);
            return;
        }

        tickProgress(level, state);
        EnvironmentMutationSystem.applyLateMutation(level, state);
        tickAudio(player, state, gameTime);
        tickWhispers(player, state, gameTime);
        tickDesktopMessage(state);
        tickForcedCrash(player, state);
        tickWatcher(player, state, gameTime);
        MirrorMineSystem.triggerReplacementEnding(player, state);
        MirrorMineSystem.tickMirror(player, state);
        cleanupWatchers(player);

        data.setDirty();
    }

    private static void tickProgress(ServerLevel level, PlayerFearState state) {
        long gameTime = level.getGameTime();
        if (state.minuteProgressTick() <= 0L || gameTime >= state.minuteProgressTick()) {
            state.addFear(1);
            state.setMinuteProgressTick(gameTime + 1200L);
        }
    }

    private static boolean redirectFalseEscape(ServerPlayer player, PlayerFearState state) {
        if (!state.mirrorEntered()
            && state.firstSpawnComplete()
            && player.serverLevel().dimension().equals(Level.OVERWORLD)
            && player.getY() > MineshaftPrisonSystem.SURFACE_ESCAPE_Y) {
            state.incrementFakeExitCount();
            state.addFear(10);
            if (player.getY() > MineshaftPrisonSystem.MIRROR_ESCAPE_Y || state.fakeExitCount() >= 3 || MirrorMineSystem.shouldEnterMirror(state)) {
                MirrorMineSystem.enterMirror(player, state);
                return true;
            }
            FearMessageSystem.fakeExit(player);
            MineshaftPrisonSystem.sendDeeper(player, state.fakeExitCount());
            return true;
        }

        if (!state.mirrorEntered() && MineshaftPrisonSystem.isInsideMineRegion(player) && MineshaftPrisonSystem.reachedFalseExit(player, state.fakeExitCount())) {
            state.incrementFakeExitCount();
            state.addFear(10);
            if (MirrorMineSystem.shouldEnterMirror(state)) {
                MirrorMineSystem.enterMirror(player, state);
                return true;
            }
            FearMessageSystem.fakeExit(player);
            MineshaftPrisonSystem.sendDeeper(player, state.fakeExitCount());
            return true;
        }
        return false;
    }

    private static void applyDebugFearEffects(ServerPlayer player, PlayerFearState state) {
        if (state.fearProgress() >= 100) {
            EnvironmentMutationSystem.applyLateMutation(player.serverLevel(), state);
            player.displayClientMessage(Component.literal("The mine noticed the change."), true);
        }
        if (state.fearProgress() >= 220) {
            if (WatcherIllusionSystem.spawnWatcher(player)) {
                state.incrementWatcherSightings();
                FearMessageSystem.watcherSeen(player);
            }
        }
        if (state.fearProgress() >= 260 && !state.mirrorEntered()) {
            MirrorMineSystem.enterMirror(player, state);
        }
    }

    private static void tickAudio(ServerPlayer player, PlayerFearState state, long gameTime) {
        if (state.fearProgress() < 10 || gameTime < state.nextAudioTick()) {
            return;
        }

        int roll = player.getRandom().nextInt(4);
        BlockPos pos = player.blockPosition().offset(player.getRandom().nextInt(25) - 12, 0, player.getRandom().nextInt(25) - 12);
        if (roll == 0) {
            player.serverLevel().playSound(null, pos, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.AMBIENT, 0.9F, 0.55F);
        } else if (roll == 1) {
            player.serverLevel().playSound(null, pos, SoundEvents.WOOD_STEP, SoundSource.AMBIENT, 0.7F, 0.65F);
        } else if (roll == 2) {
            player.serverLevel().playSound(null, pos, SoundEvents.MINECART_RIDING, SoundSource.AMBIENT, 0.55F, 0.4F);
        } else {
            player.serverLevel().playSound(null, pos, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 0.8F, 0.5F);
        }

        state.setNextAudioTick(gameTime + 600L + player.getRandom().nextInt(1200));
    }

    private static void tickWhispers(ServerPlayer player, PlayerFearState state, long gameTime) {
        if (state.fearProgress() < 40 || gameTime < state.nextWhisperTick()) {
            return;
        }

        if (player.getRandom().nextInt(4) == 0) {
            FearMessageSystem.nameCall(player);
        } else {
            FearMessageSystem.whisper(player, state.fearProgress() + state.reconnectCount());
        }

        state.setNextWhisperTick(gameTime + 1200L + player.getRandom().nextInt(3600));
    }

    private static void tickDesktopMessage(PlayerFearState state) {
        if (!state.desktopMessageCreated() && state.fearProgress() >= 160 && state.reconnectCount() >= 2) {
            if (ExternalTextScareSystem.createDesktopMessage()) {
                state.setDesktopMessageCreated();
            }
        }
    }

    private static void tickForcedCrash(ServerPlayer player, PlayerFearState state) {
        if (ForcedCrashSystem.shouldTrigger(state)) {
            ForcedCrashSystem.crashOrDisconnect(player, state);
        }
    }

    private static void tickWatcher(ServerPlayer player, PlayerFearState state, long gameTime) {
        if (state.fearProgress() < 220 || gameTime < state.nextWatcherTick()) {
            return;
        }

        if (WatcherIllusionSystem.spawnWatcher(player)) {
            state.incrementWatcherSightings();
            state.addFear(20);
            FearMessageSystem.watcherSeen(player);
            MirrorMineSystem.triggerReplacementEnding(player, state);
        }

        state.setNextWatcherTick(gameTime + 6000L + player.getRandom().nextInt(8400));
    }

    private static void cleanupWatchers(ServerPlayer player) {
        for (ArmorStand watcher : player.serverLevel().getEntitiesOfClass(ArmorStand.class, player.getBoundingBox().inflate(64.0), armorStand -> armorStand.getTags().contains(WatcherIllusionSystem.WATCHER_TAG))) {
            if (watcher.tickCount > 420 || watcher.distanceTo(player) < 3.0F) {
                watcher.discard();
            }
        }
    }

    private static ServerLevel dataLevel(ServerPlayer player) {
        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        return overworld == null ? player.serverLevel() : overworld;
    }
}
