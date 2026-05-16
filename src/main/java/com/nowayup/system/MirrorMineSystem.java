package com.nowayup.system;

import com.nowayup.data.PlayerFearState;
import com.nowayup.NoWayUpMod;
import com.nowayup.network.NoWayUpNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

public final class MirrorMineSystem {
    public static final ResourceKey<net.minecraft.world.level.Level> MIRROR_LEVEL = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(NoWayUpMod.MOD_ID, "mirror_mine"));
    public static final ResourceKey<net.minecraft.world.level.Level> DESCENT_VOID_LEVEL = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(NoWayUpMod.MOD_ID, "descent_void"));
    public static final ResourceKey<net.minecraft.world.level.Level> DAWN_LEVEL = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(NoWayUpMod.MOD_ID, "dawn"));
    public static final BlockPos MIRROR_POS = new BlockPos(128, -32, 0);
    private static final BlockPos DESCENT_VOID_POS = new BlockPos(0, 80, 0);
    private static final BlockPos DAWN_POS = new BlockPos(0, 80, 0);
    private static final BlockPos DESCENT_EXIT_POS = MIRROR_POS.offset(0, -20, -26);
    private static final BlockPos LEGACY_DESCENT_EXIT_POS = MIRROR_POS.offset(0, -20, -18);
    private static final BlockPos FALSE_DESCENT_DOOR_POS = MIRROR_POS.offset(0, -8, -13);
    private static final BlockPos WITNESS_EXIT_POS = MIRROR_POS.offset(0, 0, 24);
    private static final BlockPos WITNESS_GATE_POS = MIRROR_POS.offset(0, 0, 12);
    private static final BlockPos WITNESS_VOID_POS = MIRROR_POS.offset(0, 28, 0);
    private static final BlockPos SEAL_GATE_POS = MIRROR_POS.offset(0, 0, 5);
    private static final BlockPos SEAL_CHAMBER_POS = MIRROR_POS.offset(24, 0, 0);
    private static final BlockPos ELIAS_CHAMBER_POS = MIRROR_POS.offset(48, 24, 0);
    private static final BlockPos ELIAS_SPAWN_POS = ELIAS_CHAMBER_POS.offset(0, 0, 2);
    private static final BlockPos ELIAS_ROPE_POS = ELIAS_CHAMBER_POS.offset(0, 0, -3);
    private static final String WITNESS_TAG = "nowayup_witness";
    private static final String WITNESS_ENDING_TAG = "nowayup_witness_ending";
    private static final long COLLAPSE_STAGE_TICKS = 600L;
    private static final long COLLAPSE_RUMBLE_TICKS = 100L;
    private static final long REPLACEMENT_END_TICKS = 4800L;
    private static final long ENDING_RETURN_TICKS = 3600L;

    private MirrorMineSystem() {
    }

    public static boolean shouldEnterMirror(PlayerFearState state) {
        return !state.mirrorEntered()
            && state.fearProgress() >= 250
            && state.fakeExitCount() >= 5
            && state.watcherSightings() >= 3;
    }

    public static boolean shouldEnterMirrorGate(PlayerFearState state) {
        return !state.mirrorEntered()
            && (state.fearProgress() >= 80
                || state.fakeExitCount() >= 1
                || state.reconnectCount() >= 1
                || state.watcherSightings() >= 1);
    }

    public static boolean isInsideMirrorRegion(BlockPos pos) {
        return Math.abs(pos.getX() - MIRROR_POS.getX()) <= 80
            && Math.abs(pos.getZ() - MIRROR_POS.getZ()) <= 80
            && pos.getY() <= MIRROR_POS.getY() + 32
            && pos.getY() >= MIRROR_POS.getY() - 80;
    }

    public static void enterMirror(ServerPlayer player, PlayerFearState state) {
        ServerLevel level = mirrorLevelOrFallback(player);
        buildMirrorMine(level);
        state.setMirrorEntered(true);
        state.setMirrorStartTick(level.getGameTime());
        state.setCollapseStage(0);
        state.setMirrorEventStage(0);
        state.clearEndingReturn();
        state.setNextMirrorFootstepTick(level.getGameTime() + 100L);
        state.addFear(50);
        triggerMirrorEntryDesktopMessage(player, state);
        player.teleportTo(level, MIRROR_POS.getX() + 0.5, MIRROR_POS.getY(), MIRROR_POS.getZ() + 0.5, player.getYRot() + 180.0F, 0.0F);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 45, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0, false, false));
        player.displayClientMessage(NoWayUpText.tr("nowayup.message.do_not_climb_this_time"), true);
        level.playSound(null, player.blockPosition(), SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), SoundSource.AMBIENT, 1.0F, 0.5F);
    }

    private static void triggerMirrorEntryDesktopMessage(ServerPlayer player, PlayerFearState state) {
        if (state.desktopMessageCreated()) {
            return;
        }
        NoWayUpNetwork.sendDesktopScare(player);
        state.setDesktopMessageCreated();
    }

    public static void tickMirror(ServerPlayer player, PlayerFearState state) {
        if (!state.mirrorEntered()) {
            return;
        }

        ServerLevel level = player.serverLevel();
        if (!level.dimension().equals(MIRROR_LEVEL)) {
            return;
        }
        if (state.mirrorStartTick() <= 0L || state.mirrorStartTick() > level.getGameTime()) {
            state.setMirrorStartTick(level.getGameTime());
        }
        long mirrorTicks = level.getGameTime() - state.mirrorStartTick();
        tickMirrorFootsteps(player, state, level.getGameTime());
        tickMirrorEvents(player, state, mirrorTicks);
        cleanupWitnessFigures(player);
        int targetStage = (int) Math.min(5, mirrorTicks / COLLAPSE_STAGE_TICKS);
        if (targetStage > state.collapseStage()) {
            applyCollapseStage(level, player, targetStage);
            state.setCollapseStage(targetStage);
            player.displayClientMessage(NoWayUpText.tr(collapseMessageKey(targetStage)), true);
            level.playSound(null, player.blockPosition(), SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 1.0F, 0.55F);
        }
        tickActiveCollapse(level, player, state, mirrorTicks);

        if (isNearDescentExit(player.blockPosition())) {
            triggerDescentChoice(player, state);
        } else if (!hasTerminalEnding(state) && state.collapseStage() >= 2 && player.isShiftKeyDown() && player.blockPosition().distSqr(SEAL_GATE_POS) < 9.0) {
            triggerSealEnding(player, state);
        } else if (!hasTerminalEnding(state) && isAtWitnessGate(player.blockPosition())) {
            triggerWitnessEnding(player, state);
        } else if (!hasTerminalEnding(state) && player.blockPosition().distSqr(FALSE_DESCENT_DOOR_POS) < 6.0) {
            triggerFalseDoorFromInteraction(player, state);
        } else if (!hasTerminalEnding(state) && player.getY() > MIRROR_POS.getY() + 6 && state.collapseStage() >= 1) {
            triggerLoopEnding(player, state);
        } else if (!hasTerminalEnding(state) && mirrorTicks >= REPLACEMENT_END_TICKS) {
            triggerReplacementEnding(player, state);
        }
    }

    public static boolean isNearDescentExit(BlockPos pos) {
        return pos.distSqr(DESCENT_EXIT_POS) < 25.0
            || pos.distSqr(DESCENT_EXIT_POS.above()) < 25.0
            || pos.distSqr(LEGACY_DESCENT_EXIT_POS) < 25.0
            || pos.distSqr(LEGACY_DESCENT_EXIT_POS.above()) < 25.0;
    }

    public static boolean isDescentDoorBlock(BlockPos pos) {
        return pos.equals(DESCENT_EXIT_POS)
            || pos.equals(DESCENT_EXIT_POS.above())
            || pos.equals(LEGACY_DESCENT_EXIT_POS)
            || pos.equals(LEGACY_DESCENT_EXIT_POS.above());
    }

    public static boolean isFalseDescentDoorBlock(BlockPos pos) {
        return pos.equals(FALSE_DESCENT_DOOR_POS) || pos.equals(FALSE_DESCENT_DOOR_POS.above());
    }

    private static boolean isAtWitnessGate(BlockPos pos) {
        return Math.abs(pos.getX() - WITNESS_GATE_POS.getX()) <= 2
            && pos.getY() >= WITNESS_GATE_POS.getY()
            && pos.getY() <= WITNESS_GATE_POS.getY() + 3
            && Math.abs(pos.getZ() - WITNESS_GATE_POS.getZ()) <= 2;
    }

    public static boolean triggerDescentChoice(ServerPlayer player, PlayerFearState state) {
        if (!state.mirrorEntered() || hasTerminalEnding(state)) {
            return false;
        }
        if (hasAllStartingLoreBooks(player)) {
            triggerHappyEnding(player, state);
        } else {
            triggerDescentEnding(player, state);
        }
        return true;
    }

    public static boolean triggerFalseDoorFromInteraction(ServerPlayer player, PlayerFearState state) {
        if (!state.mirrorEntered() || state.mirrorEventStage() >= 4) {
            return false;
        }
        state.setMirrorEventStage(4);
        triggerFalseDoor(player, state);
        return true;
    }

    public static void forceCollapseStage(ServerPlayer player, PlayerFearState state, int stage) {
        if (!state.mirrorEntered()) {
            enterMirror(player, state);
        }
        int clamped = Math.max(0, Math.min(5, stage));
        ServerLevel level = player.serverLevel();
        long adjustedStart = level.getGameTime() - (clamped * COLLAPSE_STAGE_TICKS);
        state.setMirrorStartTick(Math.max(1L, adjustedStart));
        state.setCollapseStage(Math.max(0, clamped - 1));
        if (clamped > 0) {
            applyCollapseStage(level, player, clamped);
        }
        state.setCollapseStage(clamped);
        player.displayClientMessage(NoWayUpText.tr(collapseMessageKey(clamped)), true);
    }

    private static boolean hasTerminalEnding(PlayerFearState state) {
        return state.endingReturnPending();
    }

    public static boolean tickEndingReturn(ServerPlayer player, PlayerFearState state, long gameTime) {
        if (!state.endingReturnPending()) {
            return false;
        }
        if (state.endingReturnTick() > gameTime) {
            return false;
        }
        returnToMineAfterEnding(player, state);
        return true;
    }

    public static void returnToMineAfterEnding(ServerPlayer player, PlayerFearState state) {
        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        ServerLevel target = overworld == null ? player.serverLevel() : overworld;
        MineshaftPrisonSystem.buildStartingChamber(target);
        MineshaftPrisonSystem.updateSupplyChest(target);
        state.setMirrorEntered(false);
        state.setCollapseStage(0);
        state.setMirrorEventStage(0);
        state.clearEndingReturn();
        state.resetProgressForNewLoop(target.getGameTime());
        player.setGameMode(GameType.SURVIVAL);
        player.removeAllEffects();
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.SLOW_FALLING);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.teleportTo(target, MineshaftPrisonSystem.START_POS.getX() + 0.5, MineshaftPrisonSystem.START_POS.getY(), MineshaftPrisonSystem.START_POS.getZ() + 0.5, player.getYRot(), 0.0F);
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
        player.displayClientMessage(NoWayUpText.tr("nowayup.message.back_to_mine"), true);
        target.playSound(null, MineshaftPrisonSystem.START_POS, SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.4F);
        target.playSound(null, MineshaftPrisonSystem.START_POS.offset(0, 0, -4), SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.AMBIENT, 0.9F, 0.5F);
    }

    public static boolean allEndingsComplete(PlayerFearState state) {
        return state.loopEndingComplete()
            && state.descentEndingComplete()
            && state.replacementEndingComplete()
            && state.witnessEndingComplete()
            && state.sealEndingComplete()
            && state.eliasEndingComplete()
            && state.happyEndingComplete();
    }

    public static void scheduleEndingReturnToMine(PlayerFearState state, long gameTime, long delayTicks) {
        state.scheduleEndingReturn(gameTime, delayTicks, false);
    }

    private static void tickMirrorFootsteps(ServerPlayer player, PlayerFearState state, long gameTime) {
        if (gameTime < state.nextMirrorFootstepTick()) {
            return;
        }
        BlockPos behind = player.blockPosition().relative(player.getDirection().getOpposite(), 3);
        player.serverLevel().playSound(null, behind, SoundEvents.DEEPSLATE_STEP, SoundSource.AMBIENT, 0.9F, 0.55F);
        if (player.getRandom().nextInt(3) == 0) {
            player.displayClientMessage(NoWayUpText.tr("nowayup.message.steps_late"), true);
        }
        state.setNextMirrorFootstepTick(gameTime + 80L + player.getRandom().nextInt(180));
    }

    private static void tickMirrorEvents(ServerPlayer player, PlayerFearState state, long mirrorTicks) {
        if (state.mirrorEventStage() < 1 && mirrorTicks >= 600L) {
            placeReversedSigns(player.serverLevel());
            player.displayClientMessage(NoWayUpText.tr("nowayup.message.do_not_wake_up"), true);
            state.setMirrorEventStage(1);
        }
        if (state.mirrorEventStage() < 2 && mirrorTicks >= 1200L) {
            String reversedName = new StringBuilder(player.getGameProfile().getName()).reverse().toString();
            SignTextSystem.placeStandingSign(player.serverLevel(), MIRROR_POS.offset(3, 0, -5), 10, reversedName, "was here", "first.", "");
            player.displayClientMessage(NoWayUpText.tr("nowayup.message.was_here_first", reversedName), true);
            state.setMirrorEventStage(2);
        }
        if (state.mirrorEventStage() < 3 && mirrorTicks >= 1800L) {
            placeFalseDescentDoor(player.serverLevel());
            player.displayClientMessage(NoWayUpText.tr("nowayup.message.not_the_exit"), true);
            state.setMirrorEventStage(3);
        }
    }

    private static ServerLevel mirrorLevelOrFallback(ServerPlayer player) {
        ServerLevel mirror = player.server.getLevel(MIRROR_LEVEL);
        return mirror == null ? player.serverLevel() : mirror;
    }

    private static ServerLevel descentVoidLevelOrFallback(ServerPlayer player) {
        ServerLevel descentVoid = player.server.getLevel(DESCENT_VOID_LEVEL);
        return descentVoid == null ? player.serverLevel() : descentVoid;
    }

    private static ServerLevel dawnLevelOrFallback(ServerPlayer player) {
        ServerLevel dawn = player.server.getLevel(DAWN_LEVEL);
        return dawn == null ? player.serverLevel() : dawn;
    }

    public static void triggerReplacementEnding(ServerPlayer player, PlayerFearState state) {
        if (state.replacementEndingComplete() || !state.mirrorEntered()) {
            return;
        }
        state.setReplacementEndingComplete();
        NoWayUpAdvancementSystem.awardEnding(player, NoWayUpAdvancementSystem.REPLACEMENT);
        player.displayClientMessage(NoWayUpText.tr("nowayup.message.replacement_1"), false);
        player.displayClientMessage(NoWayUpText.tr("nowayup.message.replacement_2"), true);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 240, 8, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 240, 0, false, false));
        player.setDeltaMovement(0.0, 0.0, 0.0);
        state.scheduleEndingReturn(player.serverLevel().getGameTime(), ENDING_RETURN_TICKS);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 1.0F, 0.4F);
    }

    public static void triggerWitnessEnding(ServerPlayer player, PlayerFearState state) {
        boolean firstCompletion = !state.witnessEndingComplete();
        if (firstCompletion) {
            NoWayUpAdvancementSystem.awardEnding(player, NoWayUpAdvancementSystem.WITNESS);
        }
        state.setWitnessEndingComplete();
        state.setMirrorEntered(false);
        ServerLevel level = player.serverLevel();
        state.setWitnessEndingStartTick(level.getGameTime());
        state.setWitnessEndingStage(0);
        state.scheduleEndingReturn(level.getGameTime(), ENDING_RETURN_TICKS);
        sendToWitnessVoid(player, state);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 180, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 180, 3, false, false));
        player.displayClientMessage(NoWayUpText.tr("nowayup.message.witness_early"), false);
        player.displayClientMessage(NoWayUpText.tr("nowayup.message.witness_warned"), true);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.AMBIENT, 0.9F, 0.55F);
    }

    public static void sendToWitnessVoid(ServerPlayer player, PlayerFearState state) {
        ServerLevel level = mirrorLevelOrFallback(player);
        buildWitnessVoid(level);
        state.setWitnessEndingComplete();
        state.setMirrorEntered(false);
        player.setGameMode(GameType.SPECTATOR);
        player.teleportTo(level, WITNESS_VOID_POS.getX() + 0.5, WITNESS_VOID_POS.getY() + 1.2, WITNESS_VOID_POS.getZ() + 0.5, 180.0F, 35.0F);
        if (state.witnessEndingStage() < 4 && level.getEntitiesOfClass(ArmorStand.class, new net.minecraft.world.phys.AABB(WITNESS_VOID_POS.offset(0, 0, -4)).inflate(4.0), armorStand -> armorStand.getTags().contains(WITNESS_ENDING_TAG)).isEmpty()) {
            spawnWitnessEndingFigure(level, WITNESS_VOID_POS.offset(0, 0, -4), player.getGameProfile().getName());
        }
    }

    public static void tickWitnessEnding(ServerPlayer player, PlayerFearState state, long gameTime) {
        if (!isWitnessEndingActive(state) || !player.serverLevel().dimension().equals(MIRROR_LEVEL)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        if (state.witnessEndingStartTick() > gameTime) {
            state.setWitnessEndingStartTick(gameTime);
            state.setWitnessEndingStage(0);
        }

        player.setGameMode(GameType.SPECTATOR);
        player.teleportTo(level, WITNESS_VOID_POS.getX() + 0.5, WITNESS_VOID_POS.getY() + 1.2, WITNESS_VOID_POS.getZ() + 0.5, player.getYRot(), player.getXRot());

        BlockPos figurePos = WITNESS_VOID_POS.offset(0, 0, -4);
        if (state.witnessEndingStage() < 4 && gameTime % 100L == 0L && level.getEntitiesOfClass(ArmorStand.class, new net.minecraft.world.phys.AABB(figurePos).inflate(4.0), armorStand -> armorStand.getTags().contains(WITNESS_ENDING_TAG)).isEmpty()) {
            spawnWitnessEndingFigure(level, figurePos, player.getGameProfile().getName());
        }

        long elapsed = gameTime - state.witnessEndingStartTick();
        if (state.witnessEndingStage() < 1 && elapsed >= 60L) {
            state.setWitnessEndingStage(1);
            player.displayClientMessage(NoWayUpText.tr("nowayup.message.witness_not_late"), false);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 0.8F, 0.55F);
        }
        if (state.witnessEndingStage() < 2 && elapsed >= 160L) {
            state.setWitnessEndingStage(2);
            player.displayClientMessage(NoWayUpText.tr("nowayup.message.witness_do_not_warn"), false);
            level.playSound(null, WITNESS_VOID_POS, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 0.9F, 0.45F);
        }
        if (state.witnessEndingStage() < 3 && elapsed >= 280L) {
            state.setWitnessEndingStage(3);
            player.displayClientMessage(NoWayUpText.tr("nowayup.message.witness_they_warned"), false);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 4, false, false));
        }

        if (state.witnessEndingStage() < 4 && elapsed >= 420L) {
            state.setWitnessEndingStage(4);
            player.displayClientMessage(NoWayUpText.tr("nowayup.message.now_watch"), false);
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 260, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 260, 7, false, false));
            level.playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.8F, 0.35F);
            for (ArmorStand witness : level.getEntitiesOfClass(ArmorStand.class, new net.minecraft.world.phys.AABB(figurePos).inflate(8.0), armorStand -> armorStand.getTags().contains(WITNESS_ENDING_TAG))) {
                level.sendParticles(ParticleTypes.ASH, witness.getX(), witness.getY() + 1.0, witness.getZ(), 80, 0.45, 0.9, 0.45, 0.02);
                witness.discard();
            }
        }

        if (state.witnessEndingStage() < 4 && gameTime % 40L == 0L) {
            level.sendParticles(ParticleTypes.ASH, figurePos.getX() + 0.5, figurePos.getY() + 1.0, figurePos.getZ() + 0.5, 25, 0.4, 0.8, 0.4, 0.01);
        }
        if (gameTime % 120L == 0L) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 90, 0, false, false));
            level.playSound(null, WITNESS_VOID_POS, SoundEvents.DEEPSLATE_STEP, SoundSource.AMBIENT, 0.7F, 0.5F);
        }
    }

    public static boolean isWitnessEndingActive(PlayerFearState state) {
        return state.witnessEndingComplete()
            && state.endingReturnPending()
            && state.witnessEndingStartTick() > 0L;
    }

    public static void triggerSealEnding(ServerPlayer player, PlayerFearState state) {
        if (state.sealEndingComplete()) {
            return;
        }
        state.setSealEndingComplete();
        NoWayUpAdvancementSystem.awardEnding(player, NoWayUpAdvancementSystem.SEAL);
        state.setMirrorEntered(false);
        ServerLevel level = player.serverLevel();
        buildSealChamber(level, player.getGameProfile().getName());
        ensureSafeArrival(level, SEAL_CHAMBER_POS, Blocks.REINFORCED_DEEPSLATE);
        applyArrivalSafety(player);
        player.teleportTo(level, SEAL_CHAMBER_POS.getX() + 0.5, SEAL_CHAMBER_POS.getY(), SEAL_CHAMBER_POS.getZ() + 0.5, 90.0F, 0.0F);
        applyArrivalSafety(player);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 260, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 260, 4, false, false));
        player.displayClientMessage(NoWayUpText.tr("nowayup.message.seal_1"), false);
        player.displayClientMessage(NoWayUpText.tr("nowayup.message.seal_2"), true);
        state.scheduleEndingReturn(level.getGameTime(), ENDING_RETURN_TICKS);
        level.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.AMBIENT, 1.0F, 0.45F);
    }

    public static void triggerEliasEnding(ServerPlayer player, PlayerFearState state) {
        boolean firstCompletion = !state.eliasEndingComplete();
        if (firstCompletion) {
            NoWayUpAdvancementSystem.awardEnding(player, NoWayUpAdvancementSystem.ELIAS);
        }
        ServerLevel level = mirrorLevelOrFallback(player);
        state.setEliasEndingComplete();
        state.setEliasEndingActive(true);
        state.resetEliasAscentComplete();
        state.setMirrorEntered(false);
        state.setCollapseStage(0);
        state.setMirrorEventStage(0);
        state.clearEndingReturn();
        buildEliasChamber(level);
        ensureSafeArrival(level, ELIAS_SPAWN_POS, Blocks.STONE);
        placeEliasRope(level);
        player.setGameMode(GameType.SURVIVAL);
        applyArrivalSafety(player);
        player.teleportTo(level, ELIAS_SPAWN_POS.getX() + 0.5, ELIAS_SPAWN_POS.getY(), ELIAS_SPAWN_POS.getZ() + 0.5, 180.0F, 0.0F);
        applyArrivalSafety(player);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 240, 0, false, false));
        player.displayClientMessage(NoWayUpText.tr("nowayup.message.elias_1"), false);
        player.displayClientMessage(NoWayUpText.tr("nowayup.message.elias_2"), true);
        level.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.AMBIENT, 0.6F, 1.2F);
    }

    public static void tickEliasChamber(ServerPlayer player, PlayerFearState state) {
        if (!isEliasEndingActive(state)
            || state.eliasAscentComplete()
            || !player.serverLevel().dimension().equals(MIRROR_LEVEL)) {
            return;
        }

        BlockPos top = ELIAS_ROPE_POS.offset(0, 11, 0);
        boolean reachedRopeTop = player.getY() >= top.getY() - 0.2D
            && Math.abs(player.getX() - (top.getX() + 0.5D)) <= 1.5D
            && Math.abs(player.getZ() - (top.getZ() + 0.5D)) <= 1.5D;
        if (!reachedRopeTop) {
            return;
        }

        state.setEliasAscentComplete();
        state.setEliasEndingActive(false);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 260, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 260, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 180, 8, false, false));
        player.displayClientMessage(NoWayUpText.tr("nowayup.message.elias_final"), false);
        state.scheduleEndingReturn(player.serverLevel().getGameTime(), ENDING_RETURN_TICKS);
        player.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 80, 1.5, 1.0, 1.5, 0.01);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.25F);
    }

    public static boolean isEliasEndingActive(PlayerFearState state) {
        return state.eliasEndingComplete() && state.eliasEndingActive();
    }

    public static void triggerHappyEnding(ServerPlayer player, PlayerFearState state) {
        ServerLevel target = dawnLevelOrFallback(player);
        if (state.happyEndingComplete()) {
            state.setMirrorEntered(false);
            state.setCollapseStage(0);
            state.setMirrorEventStage(0);
            buildDawnClearing(target);
            ensureSafeArrival(target, DAWN_POS.above(), Blocks.GRASS_BLOCK);
            applyArrivalSafety(player);
            player.teleportTo(target, DAWN_POS.getX() + 0.5, DAWN_POS.getY() + 1.0, DAWN_POS.getZ() + 0.5, 0.0F, 0.0F);
            applyArrivalSafety(player);
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0, false, false));
            scheduleEndingReturnToMine(state, target.getGameTime(), ENDING_RETURN_TICKS);
            player.displayClientMessage(NoWayUpText.tr("nowayup.message.way_out_open"), true);
            return;
        }
        state.setHappyEndingComplete();
        NoWayUpAdvancementSystem.awardEnding(player, NoWayUpAdvancementSystem.DAWN);
        state.setMirrorEntered(false);
        state.setCollapseStage(0);
        state.setMirrorEventStage(0);
        buildDawnClearing(target);
        ensureSafeArrival(target, DAWN_POS.above(), Blocks.GRASS_BLOCK);
        applyArrivalSafety(player);
        player.teleportTo(target, DAWN_POS.getX() + 0.5, DAWN_POS.getY() + 1.0, DAWN_POS.getZ() + 0.5, 0.0F, 0.0F);
        applyArrivalSafety(player);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 220, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 80, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0, false, false));
        player.displayClientMessage(NoWayUpText.tr("nowayup.message.dawn_1"), false);
        player.displayClientMessage(NoWayUpText.tr("nowayup.message.dawn_2"), true);
        scheduleEndingReturnToMine(state, target.getGameTime(), ENDING_RETURN_TICKS);
        target.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    public static void sendToDawn(ServerPlayer player) {
        ServerLevel target = dawnLevelOrFallback(player);
        buildDawnClearing(target);
        ensureSafeArrival(target, DAWN_POS.above(), Blocks.GRASS_BLOCK);
        applyArrivalSafety(player);
        player.teleportTo(target, DAWN_POS.getX() + 0.5, DAWN_POS.getY() + 1.0, DAWN_POS.getZ() + 0.5, player.getYRot(), player.getXRot());
        applyArrivalSafety(player);
    }

    public static void tickDawn(ServerPlayer player, PlayerFearState state, long gameTime) {
        if (!player.serverLevel().dimension().equals(DAWN_LEVEL)) {
            return;
        }
        state.setHappyEndingComplete();
        state.setMirrorEntered(false);
        state.setCollapseStage(0);
        state.setMirrorEventStage(0);
        if (!state.endingReturnPending()) {
            scheduleEndingReturnToMine(state, gameTime, ENDING_RETURN_TICKS);
        } else if (state.endingReturnToSurface()) {
            scheduleEndingReturnToMine(state, gameTime, 1L);
        }
        if (player.getY() < 32.0D || player.getY() > 220.0D) {
            sendToDawn(player);
            return;
        }
        if (gameTime % 200L == 0L) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, false, false));
            player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 8, 1.4, 0.7, 1.4, 0.01);
        }
    }

    public static void sendToDescentVoid(ServerPlayer player, PlayerFearState state) {
        ServerLevel target = descentVoidLevelOrFallback(player);
        buildDescentVoid(target);
        ensureSafeArrival(target, DESCENT_VOID_POS.above(), Blocks.BARRIER);
        state.setDescentEndingComplete();
        state.setMirrorEntered(false);
        applyArrivalSafety(player);
        player.teleportTo(target, DESCENT_VOID_POS.getX() + 0.5, DESCENT_VOID_POS.getY() + 1.0, DESCENT_VOID_POS.getZ() + 0.5, 180.0F, 0.0F);
        applyArrivalSafety(player);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 260, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 1200, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 4, false, false));
        state.scheduleEndingReturn(target.getGameTime(), ENDING_RETURN_TICKS);
    }

    public static void tickDescentVoid(ServerPlayer player, PlayerFearState state, long gameTime) {
        if (!player.serverLevel().dimension().equals(DESCENT_VOID_LEVEL)) {
            return;
        }
        state.setDescentEndingComplete();
        state.setMirrorEntered(false);
        if (player.getY() < -32.0D || player.getY() > 220.0D) {
            sendToDescentVoid(player, state);
            return;
        }
        if (gameTime % 80L == 0L) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 220, 0, false, false));
            player.displayClientMessage(NoWayUpText.tr("nowayup.message.no_up_here"), true);
            player.serverLevel().sendParticles(ParticleTypes.ASH, player.getX(), player.getY() + 1.0, player.getZ(), 45, 2.2, 1.2, 2.2, 0.01);
        }
    }

    private static void buildDescentVoid(ServerLevel level) {
        BlockPos center = DESCENT_VOID_POS;
        for (int x = -8; x <= 8; x++) {
            for (int y = -2; y <= 6; y++) {
                for (int z = -8; z <= 8; z++) {
                    level.setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(center.offset(x, 0, z), Blocks.BARRIER.defaultBlockState(), 3);
            }
        }
        SignTextSystem.placeStandingSign(level, center.offset(0, 1, -3), 8, "You found", "a way down.", "", "");
        SignTextSystem.placeStandingSign(level, center.offset(0, 1, 3), 0, "That was", "never", "allowed.", "");
    }

    private static boolean hasAllStartingLoreBooks(ServerPlayer player) {
        boolean shaft = false;
        boolean grave = false;
        boolean mirror = false;
        boolean elias = false;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.is(Items.WRITTEN_BOOK) || !stack.hasTag()) {
                continue;
            }
            String title = stack.getTag().getString("title");
            if ("Shaft 0 Survey".equals(title)) {
                shaft = true;
            } else if ("The Upward Grave".equals(title)) {
                grave = true;
            } else if ("Mirror Note".equals(title)) {
                mirror = true;
            } else if ("Elias Ward's Journal".equals(title)) {
                elias = true;
            }
        }
        return shaft && grave && mirror && elias;
    }

    private static void ensureSafeArrival(ServerLevel level, BlockPos feetPos, Block floorBlock) {
        level.getChunk(feetPos);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(feetPos.offset(x, -1, z), floorBlock.defaultBlockState(), 3);
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 3; y++) {
                for (int z = -1; z <= 1; z++) {
                    level.setBlock(feetPos.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static void applyArrivalSafety(ServerPlayer player) {
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.fallDistance = 0.0F;
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 160, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 4, false, false));
    }

    private static void buildDawnClearing(ServerLevel level) {
        BlockPos center = DAWN_POS;
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                BlockPos ground = center.offset(x, 0, z);
                level.setBlock(ground.below(), Blocks.DIRT.defaultBlockState(), 3);
                level.setBlock(ground, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                for (int y = 1; y <= 5; y++) {
                    level.setBlock(ground.above(y), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        level.setBlock(center.offset(-3, 1, -2), Blocks.POPPY.defaultBlockState(), 3);
        level.setBlock(center.offset(2, 1, -3), Blocks.DANDELION.defaultBlockState(), 3);
        level.setBlock(center.offset(4, 1, 2), Blocks.OXEYE_DAISY.defaultBlockState(), 3);
        level.setBlock(center.offset(-5, 1, 3), Blocks.CORNFLOWER.defaultBlockState(), 3);
        level.setBlock(center.offset(0, 1, 4), Blocks.CHEST.defaultBlockState(), 3);
        BlockEntity chest = level.getBlockEntity(center.offset(0, 1, 4));
        if (chest instanceof Container container && container.getItem(0).isEmpty()) {
            container.setItem(0, new ItemStack(Items.BREAD, 6));
            container.setItem(1, new ItemStack(Items.OAK_SAPLING, 1));
        }
        SignTextSystem.placeStandingSign(level, center.offset(0, 1, -4), 8, "You brought", "their names", "back.", "");
        SignTextSystem.placeStandingSign(level, center.offset(0, 1, 6), 0, "The mine", "cannot", "follow.", "");
    }

    private static void buildMirrorMine(ServerLevel level) {
        BlockPos center = MIRROR_POS;
        for (int x = -6; x <= 6; x++) {
            for (int y = -1; y <= 5; y++) {
                for (int z = -6; z <= 6; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    boolean wall = Math.abs(x) == 6 || Math.abs(z) == 6 || y == -1 || y == 5;
                    level.setBlock(pos, wall ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        for (int z = -5; z <= 5; z++) {
            level.setBlock(center.offset(-4, 0, z), Blocks.DARK_OAK_FENCE.defaultBlockState(), 3);
            level.setBlock(center.offset(4, 0, z), Blocks.DARK_OAK_FENCE.defaultBlockState(), 3);
            level.setBlock(center.offset(-4, 3, z), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 3);
            level.setBlock(center.offset(4, 3, z), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 3);
        }

        for (int z = -5; z <= 5; z += 2) {
            level.setBlock(center.offset(0, 0, -z), Blocks.RAIL.defaultBlockState(), 3);
        }

        level.setBlock(center.offset(-2, 1, 0), Blocks.SOUL_TORCH.defaultBlockState(), 3);
        level.setBlock(center.offset(2, 1, 0), Blocks.SOUL_TORCH.defaultBlockState(), 3);
        SignTextSystem.placeStandingSign(level, center.offset(0, 0, 5), 8, "Do not", "climb this", "time.", "");
        SignTextSystem.placeStandingSign(level, center.offset(2, 0, 5), 8, "Crouch here", "when it", "starts to", "fall.");
        placeMirrorChest(level, center.offset(-3, 0, 4));
        buildDescentTunnel(level);
        buildWitnessRoute(level);
    }

    private static void buildWitnessRoute(ServerLevel level) {
        for (int z = 6; z <= 12; z++) {
            BlockPos center = MIRROR_POS.offset(0, 0, z);
            for (int x = -2; x <= 2; x++) {
                for (int y = -1; y <= 3; y++) {
                    BlockPos pos = center.offset(x, y, 0);
                    boolean wall = Math.abs(x) == 2 || y == -1 || y == 3;
                    level.setBlock(pos, wall ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }
            if (z % 4 == 0) {
                level.setBlock(center.offset(-1, 1, 0), Blocks.SOUL_TORCH.defaultBlockState(), 3);
            }
        }

        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 3; y++) {
                level.setBlock(WITNESS_GATE_POS.offset(x, y, 1), Blocks.TINTED_GLASS.defaultBlockState(), 3);
            }
        }
        for (int z = 14; z <= 30; z++) {
            BlockPos center = MIRROR_POS.offset(0, 0, z);
            for (int x = -5; x <= 5; x++) {
                for (int y = -1; y <= 5; y++) {
                    level.setBlock(center.offset(x, y, 0), Blocks.POLISHED_DEEPSLATE.defaultBlockState(), 3);
                }
            }
        }
        SignTextSystem.placeStandingSign(level, MIRROR_POS.offset(0, 0, 8), 8, "Follow", "your steps", "backward.", "");
        SignTextSystem.placeStandingSign(level, WITNESS_GATE_POS.offset(0, 0, -1), 8, "You are", "early this", "time.", "");
    }

    private static void buildWitnessRoom(ServerLevel level) {
        BlockPos center = WITNESS_EXIT_POS;
        for (int x = -5; x <= 5; x++) {
            for (int y = -1; y <= 4; y++) {
                for (int z = -3; z <= 4; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    boolean wall = Math.abs(x) == 5 || z == 4 || y == -1 || y == 4;
                    level.setBlock(pos, wall ? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 2; y++) {
                level.setBlock(center.offset(x, y, -3), Blocks.TINTED_GLASS.defaultBlockState(), 3);
            }
        }
        SignTextSystem.placeStandingSign(level, center.offset(0, 0, 3), 8, "You are", "early this", "time.", "");
    }

    private static void buildWitnessVoid(ServerLevel level) {
        BlockPos center = WITNESS_VOID_POS;
        for (int x = -5; x <= 5; x++) {
            for (int y = -2; y <= 5; y++) {
                for (int z = -7; z <= 5; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    boolean shell = Math.abs(x) == 5 || y == -2 || y == 5 || z == -7 || z == 5;
                    level.setBlock(pos, shell ? Blocks.BLACK_CONCRETE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        for (int x = -3; x <= 3; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlock(center.offset(x, -1, z), Blocks.TINTED_GLASS.defaultBlockState(), 3);
            }
        }
        level.setBlock(center.offset(0, 0, 4), Blocks.TINTED_GLASS.defaultBlockState(), 3);
        level.setBlock(center.offset(0, 1, 4), Blocks.TINTED_GLASS.defaultBlockState(), 3);
        level.setBlock(center.offset(0, 2, 4), Blocks.TINTED_GLASS.defaultBlockState(), 3);
        SignTextSystem.placeStandingSign(level, center.offset(0, 0, 3), 0, "Now", "watch.", "", "");
    }

    private static void spawnWitnessEndingFigure(ServerLevel level, BlockPos pos, String playerName) {
        for (ArmorStand old : level.getEntitiesOfClass(ArmorStand.class, new net.minecraft.world.phys.AABB(pos).inflate(32.0), armorStand -> armorStand.getTags().contains(WITNESS_ENDING_TAG))) {
            old.discard();
        }
        ArmorStand figure = new ArmorStand(EntityType.ARMOR_STAND, level);
        figure.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180.0F, 0.0F);
        figure.setNoGravity(true);
        figure.setInvulnerable(true);
        figure.setSilent(true);
        figure.addTag(WITNESS_TAG);
        figure.addTag(WITNESS_ENDING_TAG);
        figure.setItemSlot(EquipmentSlot.HEAD, playerHead(playerName));
        figure.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        figure.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        figure.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        level.addFreshEntity(figure);
    }

    private static ItemStack playerHead(String playerName) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        CompoundTag tag = head.getOrCreateTag();
        tag.putString("SkullOwner", playerName);
        return head;
    }

    public static void cleanupWitnessFigures(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (ArmorStand witness : level.getEntitiesOfClass(ArmorStand.class, player.getBoundingBox().inflate(96.0), armorStand -> armorStand.getTags().contains(WITNESS_TAG) && !armorStand.getTags().contains(WITNESS_ENDING_TAG))) {
            Vec3 toWitness = witness.position().add(0.0, 1.0, 0.0).subtract(player.getEyePosition());
            double distance = toWitness.length();
            boolean lookingToward = distance < 64.0D && player.getLookAngle().normalize().dot(toWitness.normalize()) > 0.72D;
            boolean close = distance < 24.0D;
            if (lookingToward || close || witness.tickCount > 100) {
                level.sendParticles(ParticleTypes.ASH, witness.getX(), witness.getY() + 1.0, witness.getZ(), 35, 0.35, 0.8, 0.35, 0.02);
                level.playSound(null, witness.blockPosition(), SoundEvents.SCULK_BLOCK_BREAK, SoundSource.HOSTILE, 0.8F, 0.55F);
                witness.discard();
            }
        }
    }

    private static void buildSealChamber(ServerLevel level, String playerName) {
        BlockPos center = SEAL_CHAMBER_POS;
        for (int x = -4; x <= 4; x++) {
            for (int y = -1; y <= 4; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    boolean wall = Math.abs(x) == 4 || Math.abs(z) == 4 || y == -1 || y == 4;
                    level.setBlock(pos, wall ? Blocks.REINFORCED_DEEPSLATE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(center.offset(0, 0, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);
        level.setBlock(center.offset(0, 1, 0), Blocks.SCULK_CATALYST.defaultBlockState(), 3);
        SignTextSystem.placeStandingSign(level, center.offset(0, 0, -3), 8, "DO NOT", "CLIMB.", "THE MINE", "LEARNED SKY");
        SignTextSystem.placeStandingSign(level, center.offset(0, 0, 3), 0, playerName, "taught it", "to sleep.", "");
    }

    private static void buildEliasChamber(ServerLevel level) {
        BlockPos center = ELIAS_CHAMBER_POS;
        for (int x = -7; x <= 7; x++) {
            for (int y = -2; y <= 13; y++) {
                for (int z = -7; z <= 7; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    boolean outerShell = Math.abs(x) == 7 || Math.abs(z) == 7 || y == -2 || y == 13;
                    boolean roughWall = Math.abs(x) >= 5 || Math.abs(z) >= 5 || y == -1 || y == 12;
                    if (outerShell || roughWall) {
                        level.setBlock(pos, eliasStoneFor(x, y, z).defaultBlockState(), 3);
                    } else {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        for (int x = -2; x <= 2; x++) {
            for (int z = -1; z <= 3; z++) {
                level.setBlock(center.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }

        placeEliasRope(level);
        level.setBlock(ELIAS_ROPE_POS.offset(0, 12, 0), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(ELIAS_ROPE_POS.offset(0, 11, 1), Blocks.CHAIN.defaultBlockState(), 3);
        level.setBlock(ELIAS_ROPE_POS.offset(0, 11, 0), Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.SOUTH), 3);

        level.setBlock(center.offset(-2, 0, 2), Blocks.CANDLE.defaultBlockState(), 3);
        level.setBlock(center.offset(2, 0, 1), Blocks.CANDLE.defaultBlockState(), 3);
        SignTextSystem.placeStandingSign(level, center.offset(2, 0, 0), 12, "Maps are", "how it", "learned us.", "");
    }

    private static void placeEliasRope(ServerLevel level) {
        for (int y = 0; y <= 11; y++) {
            level.setBlock(ELIAS_ROPE_POS.offset(0, y, -1), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(ELIAS_ROPE_POS.offset(0, y, 0), Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.SOUTH), 3);
        }
    }

    private static Block eliasStoneFor(int x, int y, int z) {
        int value = Math.abs((x * 31) + (y * 17) - (z * 13));
        if (value % 11 == 0) {
            return Blocks.COBBLESTONE;
        }
        if (value % 7 == 0) {
            return Blocks.ANDESITE;
        }
        if (value % 5 == 0) {
            return Blocks.DEEPSLATE;
        }
        return Blocks.STONE;
    }

    private static void buildDescentTunnel(ServerLevel level) {
        BlockPos start = MIRROR_POS.offset(0, 0, -6);
        for (int i = 0; i <= 20; i++) {
            BlockPos center = start.offset(0, -i, -i);
            for (int x = -2; x <= 2; x++) {
                for (int y = -1; y <= 2; y++) {
                    for (int z = -1; z <= 1; z++) {
                        BlockPos pos = center.offset(x, y, z);
                        boolean wall = Math.abs(x) == 2 || y == -1 || y == 2 || Math.abs(z) == 1;
                        level.setBlock(pos, wall ? Blocks.DEEPSLATE_BRICKS.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        placeDescentExitDoor(level, DESCENT_EXIT_POS);
    }

    private static void placeDescentExitDoor(ServerLevel level, BlockPos bottom) {
        level.setBlock(bottom, Blocks.OAK_DOOR.defaultBlockState()
            .setValue(DoorBlock.FACING, Direction.SOUTH)
            .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 3);
        level.setBlock(bottom.above(), Blocks.OAK_DOOR.defaultBlockState()
            .setValue(DoorBlock.FACING, Direction.SOUTH)
            .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
        SignTextSystem.placeStandingSign(level, bottom.south(), 8, "Only down", "stays open.", "", "");
        level.getServer().execute(() -> SignTextSystem.placeStandingSign(level, bottom.south(), 8, "Only down", "stays open.", "", ""));
    }

    private static void placeReversedSigns(ServerLevel level) {
        SignTextSystem.placeStandingSign(level, MIRROR_POS.offset(-3, 0, -5), 6, "This time,", "do not", "wake up.", "");
        SignTextSystem.placeStandingSign(level, MIRROR_POS.offset(5, 0, 0), 12, "The lower", "path", "remembers", "less.");
        level.getServer().execute(() -> {
            SignTextSystem.placeStandingSign(level, MIRROR_POS.offset(-3, 0, -5), 6, "This time,", "do not", "wake up.", "");
            SignTextSystem.placeStandingSign(level, MIRROR_POS.offset(5, 0, 0), 12, "The lower", "path", "remembers", "less.");
        });
    }

    private static void placeFalseDescentDoor(ServerLevel level) {
        level.setBlock(FALSE_DESCENT_DOOR_POS, Blocks.DARK_OAK_DOOR.defaultBlockState()
            .setValue(DoorBlock.FACING, net.minecraft.core.Direction.NORTH)
            .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 3);
        level.setBlock(FALSE_DESCENT_DOOR_POS.above(), Blocks.DARK_OAK_DOOR.defaultBlockState()
            .setValue(DoorBlock.FACING, net.minecraft.core.Direction.NORTH)
            .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
        level.setBlock(FALSE_DESCENT_DOOR_POS.north(), Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 3);
        level.setBlock(FALSE_DESCENT_DOOR_POS.north().above(), Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 3);
        SignTextSystem.placeStandingSign(level, FALSE_DESCENT_DOOR_POS.south(), 8, "That was", "not the", "exit.", "");
        level.getServer().execute(() -> SignTextSystem.placeStandingSign(level, FALSE_DESCENT_DOOR_POS.south(), 8, "That was", "not the", "exit.", ""));
    }

    private static void triggerFalseDoor(ServerPlayer player, PlayerFearState state) {
        ServerLevel level = player.serverLevel();
        level.setBlock(FALSE_DESCENT_DOOR_POS.south(), Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 3);
        level.setBlock(FALSE_DESCENT_DOOR_POS.south().above(), Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 3);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false));
        player.displayClientMessage(NoWayUpText.tr("nowayup.message.not_the_exit"), true);
        level.playSound(null, player.blockPosition(), SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.4F);
        ForcedCrashSystem.triggerEventCrash(player, state);
    }

    private static void placeMirrorChest(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        BlockEntity chest = level.getBlockEntity(pos);
        if (chest instanceof Container container) {
            LoreBookSystem.addMessageBook(container, 1);
        }
    }

    private static void applyCollapseStage(ServerLevel level, ServerPlayer player, int stage) {
        BlockPos center = MIRROR_POS;
        collapseAroundPlayer(level, player, stage);
        if (stage >= 1) {
            level.setBlock(center.offset(0, 4, 5), Blocks.GRAVEL.defaultBlockState(), 3);
            level.setBlock(center.offset(1, 4, 5), Blocks.GRAVEL.defaultBlockState(), 3);
        }
        if (stage >= 2) {
            level.setBlock(center.offset(0, 0, 5), Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 3);
            level.setBlock(center.offset(0, 1, 5), Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 3);
        }
        if (stage >= 3) {
            level.setBlock(center.offset(-2, 1, 0), Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(center.offset(2, 1, 0), Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(center.offset(0, 1, 0), Blocks.AIR.defaultBlockState(), 3);
        }
        if (stage >= 4) {
            level.setBlock(center.offset(-1, 0, -3), Blocks.SCULK.defaultBlockState(), 3);
            level.setBlock(center.offset(1, 0, -3), Blocks.SCULK.defaultBlockState(), 3);
        }
        if (stage >= 5) {
            level.setBlock(center.offset(5, 0, 0), Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(center.offset(5, 1, 0), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static void collapseAroundPlayer(ServerLevel level, ServerPlayer player, int stage) {
        BlockPos base = player.blockPosition();
        for (int i = -2; i <= 2; i++) {
            BlockPos fallPos = base.offset(i, 5 + stage % 2, -2 - stage % 3);
            level.setBlock(fallPos, stage % 2 == 0 ? Blocks.COBBLED_DEEPSLATE.defaultBlockState() : Blocks.GRAVEL.defaultBlockState(), 3);
            FallingBlockEntity falling = FallingBlockEntity.fall(level, fallPos, level.getBlockState(fallPos));
            falling.time = 1;
        }

        for (int i = -1; i <= 1; i++) {
            BlockPos crack = base.offset(i, 0, 2 + stage);
            if (level.isEmptyBlock(crack)) {
                level.setBlock(crack, Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 3);
            }
        }

        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, base.getX() + 0.5, base.getY() + 1.5, base.getZ() + 0.5, 40, 2.0, 1.0, 2.0, 0.03);
        level.sendParticles(ParticleTypes.ASH, base.getX() + 0.5, base.getY() + 1.2, base.getZ() + 0.5, 60, 2.5, 1.0, 2.5, 0.02);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, false));
        level.playSound(null, base, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 0.7F, 0.45F);
        level.playSound(null, base, SoundEvents.GRAVEL_FALL, SoundSource.BLOCKS, 1.0F, 0.6F);
    }

    private static void tickActiveCollapse(ServerLevel level, ServerPlayer player, PlayerFearState state, long mirrorTicks) {
        if (state.collapseStage() <= 0 || mirrorTicks % COLLAPSE_RUMBLE_TICKS != 0L) {
            return;
        }

        BlockPos base = player.blockPosition();
        int stage = state.collapseStage();
        for (int i = 0; i < Math.min(5, stage + 1); i++) {
            int dx = player.getRandom().nextInt(9) - 4;
            int dz = player.getRandom().nextInt(9) - 4;
            BlockPos fallPos = base.offset(dx, 5 + player.getRandom().nextInt(3), dz);
            if (!level.isEmptyBlock(fallPos)) {
                continue;
            }
            level.setBlock(fallPos, player.getRandom().nextBoolean() ? Blocks.GRAVEL.defaultBlockState() : Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 3);
            FallingBlockEntity falling = FallingBlockEntity.fall(level, fallPos, level.getBlockState(fallPos));
            falling.time = 1;
        }

        if (stage >= 2) {
            for (int i = -1; i <= 1; i++) {
                BlockPos crack = base.offset(i, 0, 2 + player.getRandom().nextInt(3));
                if (level.isEmptyBlock(crack)) {
                    level.setBlock(crack, Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 3);
                }
            }
        }

        level.sendParticles(ParticleTypes.ASH, base.getX() + 0.5, base.getY() + 1.4, base.getZ() + 0.5, 90, 3.0, 1.2, 3.0, 0.025);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, base.getX() + 0.5, base.getY() + 1.6, base.getZ() + 0.5, 24, 2.0, 0.8, 2.0, 0.02);
        level.playSound(null, base, SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 0.75F, 0.35F + player.getRandom().nextFloat() * 0.25F);
        level.playSound(null, base, SoundEvents.GRAVEL_FALL, SoundSource.BLOCKS, 0.85F, 0.55F);
    }

    private static String collapseMessageKey(int stage) {
        return switch (stage) {
            case 0 -> "nowayup.collapse.0";
            case 1 -> "nowayup.collapse.1";
            case 2 -> "nowayup.collapse.2";
            case 3 -> "nowayup.collapse.3";
            case 4 -> "nowayup.collapse.4";
            default -> "nowayup.collapse.5";
        };
    }

    private static void triggerDescentEnding(ServerPlayer player, PlayerFearState state) {
        sendToDescentVoid(player, state);
        NoWayUpAdvancementSystem.awardEnding(player, NoWayUpAdvancementSystem.DESCENT);
        player.displayClientMessage(NoWayUpText.tr("nowayup.message.descent"), false);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 220, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 220, 0, false, false));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.AMBIENT, 0.8F, 0.7F);
    }

    public static void triggerLoopEnding(ServerPlayer player, PlayerFearState state) {
        state.setLoopEndingComplete();
        NoWayUpAdvancementSystem.awardEnding(player, NoWayUpAdvancementSystem.LOOP);
        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        ServerLevel target = overworld == null ? player.serverLevel() : overworld;
        state.resetProgressForNewLoop(target.getGameTime());
        MineshaftPrisonSystem.buildStartingChamber(target);
        player.teleportTo(target, MineshaftPrisonSystem.START_POS.getX() + 0.5, MineshaftPrisonSystem.START_POS.getY(), MineshaftPrisonSystem.START_POS.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 180, 0, false, false));
        target.setBlock(MineshaftPrisonSystem.START_POS.offset(0, 1, -3), Blocks.AIR.defaultBlockState(), 3);
        SignTextSystem.placeStandingSign(target, MineshaftPrisonSystem.START_POS.offset(0, 0, -2), 8, "How far", "will you", "climb this", "time?");
        player.displayClientMessage(NoWayUpText.tr("nowayup.message.loop"), false);
    }
}
