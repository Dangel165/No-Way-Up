package com.nowayup.event;

import com.nowayup.data.FearProgressSavedData;
import com.nowayup.data.PlayerFearState;
import com.nowayup.system.EnvironmentMutationSystem;
import com.nowayup.system.FearMessageSystem;
import com.nowayup.system.ForcedCrashSystem;
import com.nowayup.system.LoreBookSystem;
import com.nowayup.system.MirrorMineSystem;
import com.nowayup.system.MineshaftPrisonSystem;
import com.nowayup.system.NoWayUpAdvancementSystem;
import com.nowayup.system.WatcherIllusionSystem;
import com.nowayup.network.FearHudPacket;
import com.nowayup.network.NoWayUpNetwork;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        NoWayUpNetwork.sendDesktopScare(player);
                        context.getSource().sendSuccess(() -> Component.literal("Desktop message sent to client."), false);
                        return 1;
                    }))
                .then(Commands.literal("crash")
                    .executes(context -> {
                        int kicked = ForcedCrashSystem.disconnectEveryone(context.getSource().getServer());
                        context.getSource().sendSuccess(() -> Component.literal("No Way Up crash packet sent to " + kicked + " player(s)."), true);
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
                .then(Commands.literal("collapse")
                    .then(Commands.literal("get")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            PlayerFearState state = FearProgressSavedData.get(dataLevel(player)).stateFor(player.getUUID());
                            context.getSource().sendSuccess(() -> Component.literal("Mirror collapse stage: " + state.collapseStage()), false);
                            return state.collapseStage();
                        }))
                    .then(Commands.literal("set")
                        .then(Commands.argument("stage", IntegerArgumentType.integer(0, 5))
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();
                                FearProgressSavedData data = FearProgressSavedData.get(dataLevel(player));
                                PlayerFearState state = data.stateFor(player.getUUID());
                                int stage = IntegerArgumentType.getInteger(context, "stage");
                                MirrorMineSystem.forceCollapseStage(player, state, stage);
                                data.setDirty();
                                context.getSource().sendSuccess(() -> Component.literal("Mirror collapse stage set to " + stage + "."), false);
                                return stage;
                            }))))
                .then(Commands.literal("ending")
                    .then(Commands.literal("loop")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            FearProgressSavedData data = FearProgressSavedData.get(dataLevel(player));
                            PlayerFearState state = data.stateFor(player.getUUID());
                            MirrorMineSystem.triggerLoopEnding(player, state);
                            data.setDirty();
                            context.getSource().sendSuccess(() -> Component.literal("Loop Ending triggered."), false);
                            return 1;
                        }))
                    .then(Commands.literal("witness")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            FearProgressSavedData data = FearProgressSavedData.get(dataLevel(player));
                            PlayerFearState state = data.stateFor(player.getUUID());
                            if (!state.mirrorEntered()) {
                                MirrorMineSystem.enterMirror(player, state);
                            }
                            MirrorMineSystem.triggerWitnessEnding(player, state);
                            data.setDirty();
                            context.getSource().sendSuccess(() -> Component.literal("Witness Ending triggered."), false);
                            return 1;
                        }))
                    .then(Commands.literal("seal")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            FearProgressSavedData data = FearProgressSavedData.get(dataLevel(player));
                            PlayerFearState state = data.stateFor(player.getUUID());
                            if (!state.mirrorEntered()) {
                                MirrorMineSystem.enterMirror(player, state);
                            }
                            MirrorMineSystem.triggerSealEnding(player, state);
                            data.setDirty();
                            context.getSource().sendSuccess(() -> Component.literal("Seal Ending triggered."), false);
                            return 1;
                        }))
                    .then(Commands.literal("elias")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            FearProgressSavedData data = FearProgressSavedData.get(dataLevel(player));
                            PlayerFearState state = data.stateFor(player.getUUID());
                            MirrorMineSystem.triggerEliasEnding(player, state);
                            data.setDirty();
                            context.getSource().sendSuccess(() -> Component.literal("Elias Ending triggered."), false);
                            return 1;
                        }))
                    .then(Commands.literal("happy")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            FearProgressSavedData data = FearProgressSavedData.get(dataLevel(player));
                            PlayerFearState state = data.stateFor(player.getUUID());
                            MirrorMineSystem.triggerHappyEnding(player, state);
                            data.setDirty();
                            context.getSource().sendSuccess(() -> Component.literal("Happy Ending triggered."), false);
                            return 1;
                        }))
                    .then(Commands.literal("reset")
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(context -> {
                                ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                ServerLevel level = dataLevel(target);
                                FearProgressSavedData data = FearProgressSavedData.get(level);
                                PlayerFearState state = data.stateFor(target.getUUID());
                                state.resetAllEndings();
                                state.resetEventTimers(level.getGameTime());
                                NoWayUpAdvancementSystem.revokeEndings(target);
                                ensureMineReady(level, data);
                                target.setGameMode(GameType.SURVIVAL);
                                MineshaftPrisonSystem.sendToStart(target);
                                target.displayClientMessage(Component.literal("No Way Up endings reset."), true);
                                data.setDirty();
                                context.getSource().sendSuccess(() -> Component.literal("Reset No Way Up endings for " + target.getGameProfile().getName() + "."), true);
                                return 1;
                            }))))
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

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        BlockPos pos = event.getPos();
        boolean noWayUpRegion = player.serverLevel().dimension().equals(MirrorMineSystem.MIRROR_LEVEL)
            || player.serverLevel().dimension().equals(MirrorMineSystem.DESCENT_VOID_LEVEL)
            || player.serverLevel().dimension().equals(MirrorMineSystem.DAWN_LEVEL)
            || MirrorMineSystem.isInsideMirrorRegion(pos)
            || MineshaftPrisonSystem.isInsideMineRegion(pos);
        if (noWayUpRegion) {
            event.setCanceled(false);
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.serverLevel().dimension().equals(MirrorMineSystem.MIRROR_LEVEL)) {
            return;
        }

        BlockPos pos = event.getPos();
        FearProgressSavedData data = FearProgressSavedData.get(dataLevel(player));
        PlayerFearState state = data.stateFor(player.getUUID());
        boolean handled = false;
        if (MirrorMineSystem.isDescentDoorBlock(pos) || MirrorMineSystem.isNearDescentExit(pos)) {
            handled = MirrorMineSystem.triggerDescentChoice(player, state);
        } else if (MirrorMineSystem.isFalseDescentDoorBlock(pos)) {
            handled = MirrorMineSystem.triggerFalseDoorFromInteraction(player, state);
        }

        if (handled) {
            data.setDirty();
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Monster monster
            && event.getLevel() instanceof ServerLevel level
            && (level.dimension().equals(MirrorMineSystem.MIRROR_LEVEL)
                || level.dimension().equals(MirrorMineSystem.DESCENT_VOID_LEVEL)
                || level.dimension().equals(MirrorMineSystem.DAWN_LEVEL)
                || MirrorMineSystem.isInsideMirrorRegion(monster.blockPosition()))) {
            event.setCanceled(true);
            monster.discard();
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.literal("You will never get out."), false);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel level = dataLevel(player);
        FearProgressSavedData data = FearProgressSavedData.get(level);
        PlayerFearState state = data.stateFor(player.getUUID());
        if (state.descentEndingComplete()) {
            player.setGameMode(GameType.SURVIVAL);
            player.server.execute(() -> {
                MirrorMineSystem.sendToDescentVoid(player, state);
                player.displayClientMessage(Component.literal("That was never allowed."), true);
            });
            return;
        }
        if (state.happyEndingComplete()) {
            player.setGameMode(GameType.SURVIVAL);
            player.server.execute(() -> {
                MirrorMineSystem.sendToDawn(player);
                player.displayClientMessage(Component.literal("The way out stayed open."), true);
            });
            return;
        }
        ensureMineReady(level, data);
        player.setGameMode(GameType.SURVIVAL);
        state.setFirstSpawnComplete();
        state.addFear(10);
        state.resetRunAfterDeath(level.getGameTime());
        player.server.execute(() -> {
            player.teleportTo(level, MineshaftPrisonSystem.START_POS.getX() + 0.5, MineshaftPrisonSystem.START_POS.getY(), MineshaftPrisonSystem.START_POS.getZ() + 0.5, player.getYRot(), player.getXRot());
            player.displayClientMessage(Component.literal("You will never get out."), false);
            player.displayClientMessage(Component.literal("Death was not a way out."), true);
            level.playSound(null, MineshaftPrisonSystem.START_POS, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.45F);
            level.playSound(null, MineshaftPrisonSystem.START_POS.offset(0, 0, -4), SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.AMBIENT, 0.9F, 0.5F);
        });
        data.setDirty();
    }

    @SubscribeEvent
    public void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer && !event.isForced()) {
            event.setCanceled(true);
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

        if (state.descentEndingComplete()) {
            player.setGameMode(GameType.SURVIVAL);
            MirrorMineSystem.sendToDescentVoid(player, state);
            player.displayClientMessage(Component.literal("That was never allowed."), true);
            return;
        }

        if (state.happyEndingComplete()) {
            player.setGameMode(GameType.SURVIVAL);
            MirrorMineSystem.sendToDawn(player);
            player.displayClientMessage(Component.literal("The way out stayed open."), true);
            return;
        }

        if (!state.firstSpawnComplete()) {
            ensureMineReady(level, data);
            player.setGameMode(GameType.SURVIVAL);
            player.teleportTo(level, MineshaftPrisonSystem.START_POS.getX() + 0.5, MineshaftPrisonSystem.START_POS.getY(), MineshaftPrisonSystem.START_POS.getZ() + 0.5, player.getYRot(), player.getXRot());
            state.setFirstSpawnComplete();
            state.resetEventTimers(level.getGameTime());
            state.startWakeSequence(level.getGameTime());
        } else {
            ensureMineReady(level, data);
            player.setGameMode(GameType.SURVIVAL);
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

        boolean inMirrorLevel = player.serverLevel().dimension().equals(MirrorMineSystem.MIRROR_LEVEL);

        if (player.serverLevel().dimension().equals(MirrorMineSystem.DAWN_LEVEL)) {
            MirrorMineSystem.tickDawn(player, state, gameTime);
            sendFearHud(player, state);
            data.setDirty();
            return;
        }

        if (player.serverLevel().dimension().equals(MirrorMineSystem.DESCENT_VOID_LEVEL)) {
            MirrorMineSystem.tickDescentVoid(player, state, gameTime);
            sendFearHud(player, state);
            data.setDirty();
            return;
        }

        if (state.mirrorEntered() && !inMirrorLevel) {
            state.setMirrorEntered(false);
            state.setCollapseStage(0);
            state.setMirrorEventStage(0);
            data.setDirty();
        }

        if (inMirrorLevel && state.eliasEndingComplete()) {
            MirrorMineSystem.tickEliasChamber(player, state);
            sendFearHud(player, state);
            data.setDirty();
            return;
        }

        if (redirectFalseEscape(player, state)) {
            data.setDirty();
            return;
        }

        if (recoverEscapedPlayer(player, state, data)) {
            data.setDirty();
            return;
        }

        if (state.mirrorEntered() && inMirrorLevel) {
            MirrorMineSystem.tickMirror(player, state);
            sendFearHud(player, state);
            cleanupWatchers(player);
            data.setDirty();
            if (gameTime % 20L != 0L) {
                return;
            }
        }

        if (gameTime % 20L != 0L) {
            cleanupWatchers(player);
            return;
        }

        tickProgress(level, state);
        tickWakeSequence(player, state, gameTime);
        EnvironmentMutationSystem.applyLateMutation(level, state);
        tickMirrorGate(player, state, gameTime);
        tickAudio(player, state, gameTime);
        tickWhispers(player, state, gameTime);
        tickDesktopMessage(player, state);
        tickForcedCrash(player, state);
        tickWatcher(player, state, gameTime);
        if (inMirrorLevel) {
            MirrorMineSystem.tickMirror(player, state);
        }
        sendFearHud(player, state);
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

    private static void sendFearHud(ServerPlayer player, PlayerFearState state) {
        if (!state.firstSpawnComplete()) {
            return;
        }
        NoWayUpNetwork.sendFearHud(player, new FearHudPacket(
            state.fearProgress(),
            state.fakeExitCount(),
            state.watcherSightings(),
            state.mirrorEntered(),
            hudPhase(state),
            hudEnding(state)
        ));
    }

    private static String hudPhase(PlayerFearState state) {
        if (state.happyEndingComplete()) {
            return "Dawn";
        }
        if (state.mirrorEntered()) {
            return "Mirror";
        }
        if (state.fearProgress() >= 220) {
            return "Late";
        }
        if (state.fearProgress() >= 100) {
            return "Middle";
        }
        return "Early";
    }

    private static String hudEnding(PlayerFearState state) {
        if (state.happyEndingComplete()) {
            return "Ending: Dawn";
        }
        if (state.eliasEndingComplete()) {
            return "Ending: Elias";
        }
        if (state.sealEndingComplete()) {
            return "Ending: Seal";
        }
        if (state.witnessEndingComplete()) {
            return "Ending: Witness";
        }
        if (state.replacementEndingComplete()) {
            return "Ending: Replacement";
        }
        if (state.descentEndingComplete()) {
            return "Ending: Descent";
        }
        if (state.loopEndingComplete()) {
            return "Ending: Loop";
        }
        return "";
    }

    private static void tickWakeSequence(ServerPlayer player, PlayerFearState state, long gameTime) {
        if (state.wakeSequenceStartTick() <= 0L || state.wakeSequenceStage() >= 99) {
            return;
        }

        long elapsed = gameTime - state.wakeSequenceStartTick();
        if (state.wakeSequenceStage() < 1 && elapsed >= 20L) {
            player.displayClientMessage(Component.literal("You will never get out."), false);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.45F);
            state.addFear(10);
            state.setWakeSequenceStage(1);
        }
        if (state.wakeSequenceStage() < 2 && elapsed >= 60L) {
            FearMessageSystem.nameCall(player);
            player.serverLevel().playSound(null, player.blockPosition().offset(0, 0, -4), SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.AMBIENT, 0.9F, 0.5F);
            state.setWakeSequenceStage(2);
        }
        if (state.wakeSequenceStage() < 3 && elapsed >= 120L) {
            WatcherIllusionSystem.spawnWatcher(player);
            player.displayClientMessage(Component.literal("It found the new start."), true);
            state.setWakeSequenceStage(3);
        }
        if (state.wakeSequenceStage() < 4 && elapsed >= 200L) {
            EnvironmentMutationSystem.applyLateMutation(player.serverLevel(), state);
            player.displayClientMessage(Component.literal("The mine starts again."), true);
            state.setWakeSequenceStage(4);
        }
        if (elapsed >= 300L) {
            state.stopWakeSequence();
        }
    }

    private static boolean redirectFalseEscape(ServerPlayer player, PlayerFearState state) {
        if (!state.mirrorEntered()
            && state.firstSpawnComplete()
            && player.serverLevel().dimension().equals(Level.OVERWORLD)
            && player.getY() > MineshaftPrisonSystem.SURFACE_ESCAPE_Y) {
            state.incrementFakeExitCount();
            state.addFear(10);
            if (shouldForceMirrorFromEscape(player, state)) {
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
            if (shouldForceMirrorFromEscape(player, state)) {
                MirrorMineSystem.enterMirror(player, state);
                return true;
            }
            FearMessageSystem.fakeExit(player);
            MineshaftPrisonSystem.sendDeeper(player, state.fakeExitCount());
            return true;
        }
        return false;
    }

    private static boolean shouldForceMirrorFromEscape(ServerPlayer player, PlayerFearState state) {
        return player.getY() > MineshaftPrisonSystem.MIRROR_ESCAPE_Y
            || state.fakeExitCount() >= 3
            || MirrorMineSystem.shouldEnterMirror(state);
    }

    private static void tickMirrorGate(ServerPlayer player, PlayerFearState state, long gameTime) {
        if (state.mirrorEntered()
            || !state.firstSpawnComplete()
            || !player.serverLevel().dimension().equals(Level.OVERWORLD)
            || !MineshaftPrisonSystem.isInsideMineRegion(player)) {
            return;
        }

        int depth = Math.max(0, state.fakeExitCount());
        if (!MineshaftPrisonSystem.isAtMirrorGate(player, depth)) {
            return;
        }

        if (player.isShiftKeyDown()) {
            if (shouldTriggerEliasEnding(player)) {
                MirrorMineSystem.triggerEliasEnding(player, state);
                return;
            }
            if (MirrorMineSystem.shouldEnterMirrorGate(state)) {
                player.displayClientMessage(Component.literal("The wall opens the wrong way."), false);
                MirrorMineSystem.enterMirror(player, state);
            } else {
                player.displayClientMessage(Component.literal("The mirror is still asleep."), true);
                state.addFear(1);
            }
        } else if (gameTime % 80L == 0L) {
            player.displayClientMessage(Component.literal("The wall looks back."), true);
        }
    }

    private static boolean shouldTriggerEliasEnding(ServerPlayer player) {
        boolean hasEliasJournal = false;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.TORCH) || stack.is(Items.SOUL_TORCH) || stack.is(Items.REDSTONE_TORCH) || stack.is(Items.COMPASS) || stack.is(Items.RECOVERY_COMPASS) || stack.is(Items.FILLED_MAP) || stack.is(Items.MAP)) {
                return false;
            }
            if (stack.is(Items.WRITTEN_BOOK) || stack.is(Items.WRITABLE_BOOK)) {
                String title = stack.getOrCreateTag().getString("title");
                if ("Elias Ward's Journal".equals(title)) {
                    hasEliasJournal = true;
                } else {
                    return false;
                }
            }
        }
        return hasEliasJournal;
    }

    private static boolean recoverEscapedPlayer(ServerPlayer player, PlayerFearState state, FearProgressSavedData data) {
        if (!state.firstSpawnComplete()) {
            return false;
        }
        if (state.happyEndingComplete()) {
            return false;
        }
        if (state.mirrorEntered() || !player.serverLevel().dimension().equals(Level.OVERWORLD)) {
            return false;
        }
        if (MineshaftPrisonSystem.isInsideMineRegion(player) && player.getY() <= MineshaftPrisonSystem.SURFACE_ESCAPE_Y) {
            return false;
        }

        ServerLevel level = dataLevel(player);
        if (!data.prisonShellBuilt()) {
            MineshaftPrisonSystem.buildNoSurfaceColumn(level);
            MineshaftPrisonSystem.refreshMirrorGate(level, 0);
            data.setPrisonShellBuilt();
        }
        MineshaftPrisonSystem.updateSupplyChest(level);
        state.addFear(5);
        state.resetEventTimers(level.getGameTime());
        player.teleportTo(level, MineshaftPrisonSystem.START_POS.getX() + 0.5, MineshaftPrisonSystem.START_POS.getY(), MineshaftPrisonSystem.START_POS.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.displayClientMessage(Component.literal("You will never get out."), true);
        return true;
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

    private static void tickDesktopMessage(ServerPlayer player, PlayerFearState state) {
        if (!state.desktopMessageCreated() && state.fearProgress() >= 160 && state.reconnectCount() >= 2) {
            NoWayUpNetwork.sendDesktopScare(player);
            state.setDesktopMessageCreated();
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
        }

        state.setNextWatcherTick(gameTime + 6000L + player.getRandom().nextInt(8400));
    }

    private static void cleanupWatchers(ServerPlayer player) {
        if (player.serverLevel().dimension().equals(MirrorMineSystem.MIRROR_LEVEL)) {
            MirrorMineSystem.cleanupWitnessFigures(player);
        }
        for (ArmorStand watcher : player.serverLevel().getEntitiesOfClass(ArmorStand.class, player.getBoundingBox().inflate(64.0), armorStand -> armorStand.getTags().contains(WatcherIllusionSystem.WATCHER_TAG))) {
            boolean mirror = player.serverLevel().dimension().equals(MirrorMineSystem.MIRROR_LEVEL);
            int maxAge = mirror ? 80 : 220;
            float vanishDistance = mirror ? 16.0F : 5.0F;
            if (watcher.tickCount > maxAge || watcher.distanceTo(player) < vanishDistance || isLookingAtWatcher(player, watcher)) {
                watcher.discard();
            }
        }
    }

    private static boolean isLookingAtWatcher(ServerPlayer player, ArmorStand watcher) {
        if (watcher.distanceTo(player) > 48.0F) {
            return false;
        }
        Vec3 look = player.getLookAngle().normalize();
        Vec3 toWatcher = watcher.position().add(0.0, 1.0, 0.0).subtract(player.getEyePosition()).normalize();
        return look.dot(toWatcher) > 0.72D;
    }

    private static ServerLevel dataLevel(ServerPlayer player) {
        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        return overworld == null ? player.serverLevel() : overworld;
    }

    private static void ensureMineReady(ServerLevel level, FearProgressSavedData data) {
        if (!data.worldInitialized()) {
            MineshaftPrisonSystem.buildStartingChamber(level);
            data.setWorldInitialized();
            data.setPrisonShellBuilt();
        } else if (!data.prisonShellBuilt()) {
            MineshaftPrisonSystem.buildNoSurfaceColumn(level);
            MineshaftPrisonSystem.refreshMirrorGate(level, 0);
            data.setPrisonShellBuilt();
        }
        MineshaftPrisonSystem.updateSupplyChest(level);
    }
}
