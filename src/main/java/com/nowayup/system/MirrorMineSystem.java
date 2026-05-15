package com.nowayup.system;

import com.nowayup.data.PlayerFearState;
import com.nowayup.NoWayUpMod;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class MirrorMineSystem {
    public static final ResourceKey<net.minecraft.world.level.Level> MIRROR_LEVEL = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(NoWayUpMod.MOD_ID, "mirror_mine"));
    public static final BlockPos MIRROR_POS = new BlockPos(128, -32, 0);
    private static final BlockPos DESCENT_EXIT_POS = MIRROR_POS.offset(0, -20, -18);
    private static final BlockPos FALSE_DESCENT_DOOR_POS = MIRROR_POS.offset(0, -8, -13);

    private MirrorMineSystem() {
    }

    public static boolean shouldEnterMirror(PlayerFearState state) {
        return !state.mirrorEntered()
            && state.fearProgress() >= 250
            && state.fakeExitCount() >= 5
            && state.watcherSightings() >= 3;
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
        state.setMirrorEventStage(0);
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
        long mirrorTicks = level.getGameTime() - state.mirrorStartTick();
        tickMirrorFootsteps(player, state, level.getGameTime());
        tickMirrorEvents(player, state, mirrorTicks);
        int targetStage = (int) Math.min(5, mirrorTicks / 2400L);
        if (targetStage > state.collapseStage()) {
            applyCollapseStage(level, player, targetStage);
            state.setCollapseStage(targetStage);
            player.displayClientMessage(Component.literal(collapseMessage(targetStage)), true);
            level.playSound(null, player.blockPosition(), SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 1.0F, 0.55F);
        }

        if (!state.descentEndingComplete() && player.blockPosition().distSqr(DESCENT_EXIT_POS) < 9.0) {
            triggerDescentEnding(player, state);
        } else if (state.mirrorEventStage() >= 3 && player.blockPosition().distSqr(FALSE_DESCENT_DOOR_POS) < 6.0) {
            triggerFalseDoor(player);
        } else if (!state.loopEndingComplete() && player.getY() > MIRROR_POS.getY() + 6 && state.collapseStage() >= 1) {
            triggerLoopEnding(player, state);
        }
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

    public static void triggerReplacementEnding(ServerPlayer player, PlayerFearState state) {
        if (state.replacementEndingComplete() || state.watcherSightings() < 8) {
            return;
        }
        state.setReplacementEndingComplete();
        player.displayClientMessage(Component.literal("It fits better now."), false);
        player.displayClientMessage(Component.literal("You are not the one moving anymore."), true);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 240, 8, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 240, 0, false, false));
        player.setDeltaMovement(0.0, 0.0, 0.0);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 1.0F, 0.4F);
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
        placeMirrorChest(level, center.offset(-3, 0, 4));
        buildDescentTunnel(level);
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
        level.setBlock(DESCENT_EXIT_POS, Blocks.OAK_DOOR.defaultBlockState(), 3);
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

    private static String collapseMessage(int stage) {
        return switch (stage) {
            case 1 -> "Something above you broke.";
            case 2 -> "The way behind you is closing.";
            case 3 -> "The light is leaving.";
            case 4 -> "The mine is forgetting its shape.";
            default -> "The world is coming apart.";
        };
    }

    private static void triggerDescentEnding(ServerPlayer player, PlayerFearState state) {
        state.setDescentEndingComplete();
        player.displayClientMessage(Component.literal("You found a way down. That was never allowed."), false);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 220, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 220, 0, false, false));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.AMBIENT, 0.8F, 0.7F);
    }

    public static void triggerLoopEnding(ServerPlayer player, PlayerFearState state) {
        state.setLoopEndingComplete();
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
