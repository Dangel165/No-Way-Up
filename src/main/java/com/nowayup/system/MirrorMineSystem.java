package com.nowayup.system;

import com.nowayup.data.PlayerFearState;
import com.nowayup.NoWayUpMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
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
    private static final BlockPos SEAL_GATE_POS = MIRROR_POS.offset(0, 0, 5);
    private static final BlockPos SEAL_CHAMBER_POS = MIRROR_POS.offset(24, 0, 0);
    private static final BlockPos ELIAS_CHAMBER_POS = MIRROR_POS.offset(48, -40, 0);
    private static final String WITNESS_TAG = "nowayup_witness";
    private static final long COLLAPSE_STAGE_TICKS = 600L;
    private static final long COLLAPSE_RUMBLE_TICKS = 100L;
    private static final long REPLACEMENT_END_TICKS = 4800L;

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
        state.resetLoopEndingComplete();
        state.resetDescentEndingComplete();
        state.resetReplacementEndingComplete();
        state.resetWitnessEndingComplete();
        state.resetSealEndingComplete();
        state.setNextMirrorFootstepTick(level.getGameTime() + 100L);
        state.addFear(50);
        player.teleportTo(level, MIRROR_POS.getX() + 0.5, MIRROR_POS.getY(), MIRROR_POS.getZ() + 0.5, player.getYRot() + 180.0F, 0.0F);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 45, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0, false, false));
        player.displayClientMessage(Component.literal("Do not climb this time."), true);
        level.playSound(null, player.blockPosition(), SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), SoundSource.AMBIENT, 1.0F, 0.5F);
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
            player.displayClientMessage(Component.literal(collapseMessage(targetStage)), true);
            level.playSound(null, player.blockPosition(), SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 1.0F, 0.55F);
        }
        tickActiveCollapse(level, player, state, mirrorTicks);

        if (isNearDescentExit(player.blockPosition())) {
            triggerDescentChoice(player, state);
        } else if (!hasTerminalEnding(state) && state.collapseStage() >= 2 && player.isShiftKeyDown() && player.blockPosition().distSqr(SEAL_GATE_POS) < 9.0) {
            triggerSealEnding(player, state);
        } else if (!hasTerminalEnding(state) && state.mirrorEventStage() >= 3 && player.blockPosition().distSqr(WITNESS_EXIT_POS) < 9.0) {
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
        triggerFalseDoor(player);
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
        player.displayClientMessage(Component.literal(collapseMessage(clamped)), true);
    }

    private static boolean hasTerminalEnding(PlayerFearState state) {
        return state.loopEndingComplete()
            || state.descentEndingComplete()
            || state.replacementEndingComplete()
            || state.witnessEndingComplete()
            || state.sealEndingComplete()
            || state.eliasEndingComplete()
            || state.happyEndingComplete();
    }

    private static void tickMirrorFootsteps(ServerPlayer player, PlayerFearState state, long gameTime) {
        if (gameTime < state.nextMirrorFootstepTick()) {
            return;
        }
        BlockPos behind = player.blockPosition().relative(player.getDirection().getOpposite(), 3);
        player.serverLevel().playSound(null, behind, SoundEvents.DEEPSLATE_STEP, SoundSource.AMBIENT, 0.9F, 0.55F);
        if (player.getRandom().nextInt(3) == 0) {
            player.displayClientMessage(Component.literal("Your steps are late."), true);
        }
        state.setNextMirrorFootstepTick(gameTime + 80L + player.getRandom().nextInt(180));
    }

    private static void tickMirrorEvents(ServerPlayer player, PlayerFearState state, long mirrorTicks) {
        if (state.mirrorEventStage() < 1 && mirrorTicks >= 600L) {
            placeReversedSigns(player.serverLevel());
            player.displayClientMessage(Component.literal("This time, do not wake up."), true);
            state.setMirrorEventStage(1);
        }
        if (state.mirrorEventStage() < 2 && mirrorTicks >= 1200L) {
            String reversedName = new StringBuilder(player.getGameProfile().getName()).reverse().toString();
            SignTextSystem.placeStandingSign(player.serverLevel(), MIRROR_POS.offset(3, 0, -5), 10, reversedName, "was here", "first.", "");
            player.displayClientMessage(Component.literal(reversedName + " was here first."), true);
            state.setMirrorEventStage(2);
        }
        if (state.mirrorEventStage() < 3 && mirrorTicks >= 1800L) {
            placeFalseDescentDoor(player.serverLevel());
            player.displayClientMessage(Component.literal("That was not the exit."), true);
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
        player.displayClientMessage(Component.literal("You waited long enough for it to learn you."), false);
        player.displayClientMessage(Component.literal("You are not the one moving anymore."), true);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 240, 8, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 240, 0, false, false));
        player.setDeltaMovement(0.0, 0.0, 0.0);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 1.0F, 0.4F);
    }

    public static void triggerWitnessEnding(ServerPlayer player, PlayerFearState state) {
        if (state.witnessEndingComplete()) {
            return;
        }
        state.setWitnessEndingComplete();
        NoWayUpAdvancementSystem.awardEnding(player, NoWayUpAdvancementSystem.WITNESS);
        state.setMirrorEntered(false);
        ServerLevel level = player.serverLevel();
        buildWitnessRoom(level);
        player.teleportTo(level, WITNESS_EXIT_POS.getX() + 0.5, WITNESS_EXIT_POS.getY(), WITNESS_EXIT_POS.getZ() + 0.5, 180.0F, 0.0F);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 180, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 180, 3, false, false));
        player.displayClientMessage(Component.literal("You are early this time."), false);
        player.displayClientMessage(Component.literal("Do not warn them. They warned you too."), true);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.AMBIENT, 0.9F, 0.55F);
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
        player.teleportTo(level, SEAL_CHAMBER_POS.getX() + 0.5, SEAL_CHAMBER_POS.getY(), SEAL_CHAMBER_POS.getZ() + 0.5, 90.0F, 0.0F);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 260, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 260, 4, false, false));
        player.displayClientMessage(Component.literal("AND SOMEONE TAUGHT IT TO SLEEP."), false);
        player.displayClientMessage(Component.literal("The mine is quiet. For now."), true);
        level.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.AMBIENT, 1.0F, 0.45F);
    }

    public static void triggerEliasEnding(ServerPlayer player, PlayerFearState state) {
        if (state.eliasEndingComplete()) {
            return;
        }
        ServerLevel level = mirrorLevelOrFallback(player);
        state.setEliasEndingComplete();
        NoWayUpAdvancementSystem.awardEnding(player, NoWayUpAdvancementSystem.ELIAS);
        state.setMirrorEntered(false);
        state.setCollapseStage(0);
        state.setMirrorEventStage(0);
        buildEliasChamber(level);
        player.teleportTo(level, ELIAS_CHAMBER_POS.getX() + 0.5, ELIAS_CHAMBER_POS.getY(), ELIAS_CHAMBER_POS.getZ() + 0.5, 0.0F, 0.0F);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 240, 0, false, false));
        player.displayClientMessage(Component.literal("You came without a map. Good."), false);
        player.displayClientMessage(Component.literal("Maps are how it learned us."), true);
        level.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.AMBIENT, 0.6F, 1.2F);
    }

    public static void tickEliasChamber(ServerPlayer player, PlayerFearState state) {
        if (!state.eliasEndingComplete()
            || state.eliasAscentComplete()
            || !player.serverLevel().dimension().equals(MIRROR_LEVEL)) {
            return;
        }

        BlockPos top = ELIAS_CHAMBER_POS.offset(0, 11, 0);
        boolean reachedRopeTop = player.getY() >= top.getY() - 0.2D
            && Math.abs(player.getX() - (top.getX() + 0.5D)) <= 1.5D
            && Math.abs(player.getZ() - (top.getZ() + 0.5D)) <= 1.5D;
        if (!reachedRopeTop) {
            return;
        }

        state.setEliasAscentComplete();
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 260, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 260, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 180, 8, false, false));
        player.displayClientMessage(Component.literal("This was before up belonged to it."), false);
        player.serverLevel().sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 80, 1.5, 1.0, 1.5, 0.01);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMBIENT_CAVE.value(), SoundSource.AMBIENT, 1.0F, 0.25F);
    }

    public static void triggerHappyEnding(ServerPlayer player, PlayerFearState state) {
        if (state.happyEndingComplete()) {
            return;
        }
        ServerLevel target = dawnLevelOrFallback(player);
        state.setHappyEndingComplete();
        NoWayUpAdvancementSystem.awardEnding(player, NoWayUpAdvancementSystem.DAWN);
        state.setMirrorEntered(false);
        state.setCollapseStage(0);
        state.setMirrorEventStage(0);
        buildDawnClearing(target);
        player.teleportTo(target, DAWN_POS.getX() + 0.5, DAWN_POS.getY() + 1.0, DAWN_POS.getZ() + 0.5, 0.0F, 0.0F);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 220, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 80, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0, false, false));
        player.displayClientMessage(Component.literal("You carried every name back to the morning."), false);
        player.displayClientMessage(Component.literal("For once, the way out stayed open."), true);
        target.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    public static void sendToDawn(ServerPlayer player) {
        ServerLevel target = dawnLevelOrFallback(player);
        buildDawnClearing(target);
        player.teleportTo(target, DAWN_POS.getX() + 0.5, DAWN_POS.getY() + 1.0, DAWN_POS.getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    public static void tickDawn(ServerPlayer player, PlayerFearState state, long gameTime) {
        if (!player.serverLevel().dimension().equals(DAWN_LEVEL)) {
            return;
        }
        state.setHappyEndingComplete();
        state.setMirrorEntered(false);
        state.setCollapseStage(0);
        state.setMirrorEventStage(0);
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
        state.setDescentEndingComplete();
        state.setMirrorEntered(false);
        player.teleportTo(target, DESCENT_VOID_POS.getX() + 0.5, DESCENT_VOID_POS.getY() + 1.0, DESCENT_VOID_POS.getZ() + 0.5, 180.0F, 0.0F);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 260, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 1200, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 4, false, false));
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
            player.displayClientMessage(Component.literal("There is no up here."), true);
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
        for (int z = 6; z <= 24; z++) {
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

        buildWitnessRoom(level);
        SignTextSystem.placeStandingSign(level, MIRROR_POS.offset(0, 0, 8), 8, "Follow", "your steps", "backward.", "");
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
        spawnWitnessFigure(level, center.offset(0, 0, -6));
    }

    private static void spawnWitnessFigure(ServerLevel level, BlockPos pos) {
        for (ArmorStand old : level.getEntitiesOfClass(ArmorStand.class, new net.minecraft.world.phys.AABB(pos).inflate(16.0), armorStand -> armorStand.getTags().contains(WITNESS_TAG))) {
            old.discard();
        }
        ArmorStand figure = new ArmorStand(EntityType.ARMOR_STAND, level);
        figure.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180.0F, 0.0F);
        figure.setNoGravity(true);
        figure.setInvulnerable(true);
        figure.setSilent(true);
        figure.addTag(WITNESS_TAG);
        figure.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.PLAYER_HEAD));
        figure.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        level.addFreshEntity(figure);
    }

    public static void cleanupWitnessFigures(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (ArmorStand witness : level.getEntitiesOfClass(ArmorStand.class, player.getBoundingBox().inflate(96.0), armorStand -> armorStand.getTags().contains(WITNESS_TAG))) {
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
        for (int x = -5; x <= 5; x++) {
            for (int y = -1; y <= 12; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    boolean wall = Math.abs(x) == 5 || Math.abs(z) == 5 || y == -1 || y == 12;
                    level.setBlock(pos, wall ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        for (int y = 0; y <= 11; y++) {
            level.setBlock(center.offset(0, y, 0), Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, net.minecraft.core.Direction.SOUTH), 3);
        }
        level.setBlock(center.offset(0, 12, 0), Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(center.offset(0, 11, 1), Blocks.CHAIN.defaultBlockState(), 3);
        SignTextSystem.placeStandingSign(level, center.offset(2, 0, 0), 12, "Maps are", "how it", "learned us.", "");
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

    private static void triggerFalseDoor(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.setBlock(FALSE_DESCENT_DOOR_POS.south(), Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 3);
        level.setBlock(FALSE_DESCENT_DOOR_POS.south().above(), Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 3);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false));
        player.displayClientMessage(Component.literal("That was not the exit."), true);
        level.playSound(null, player.blockPosition(), SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.4F);
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

    private static String collapseMessage(int stage) {
        return switch (stage) {
            case 0 -> "The mirror mine is holding its breath.";
            case 1 -> "Something above you broke.";
            case 2 -> "The way behind you is closing.";
            case 3 -> "The light is leaving.";
            case 4 -> "The mine is forgetting its shape.";
            default -> "Choose. Up, down, or stay.";
        };
    }

    private static void triggerDescentEnding(ServerPlayer player, PlayerFearState state) {
        sendToDescentVoid(player, state);
        NoWayUpAdvancementSystem.awardEnding(player, NoWayUpAdvancementSystem.DESCENT);
        player.displayClientMessage(Component.literal("You found a way down. That was never allowed."), false);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 220, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 220, 0, false, false));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.AMBIENT, 0.8F, 0.7F);
    }

    public static void triggerLoopEnding(ServerPlayer player, PlayerFearState state) {
        state.setLoopEndingComplete();
        NoWayUpAdvancementSystem.awardEnding(player, NoWayUpAdvancementSystem.LOOP);
        state.setMirrorEntered(false);
        state.setCollapseStage(0);
        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        ServerLevel target = overworld == null ? player.serverLevel() : overworld;
        MineshaftPrisonSystem.buildStartingChamber(target);
        player.teleportTo(target, MineshaftPrisonSystem.START_POS.getX() + 0.5, MineshaftPrisonSystem.START_POS.getY(), MineshaftPrisonSystem.START_POS.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 180, 0, false, false));
        target.setBlock(MineshaftPrisonSystem.START_POS.offset(0, 1, -3), Blocks.AIR.defaultBlockState(), 3);
        SignTextSystem.placeStandingSign(target, MineshaftPrisonSystem.START_POS.offset(0, 0, -2), 8, "How far", "will you", "climb this", "time?");
        player.displayClientMessage(Component.literal("How far will you climb this time?"), false);
    }
}
