package com.nowayup.system;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BarrelBlock;

public final class MineshaftPrisonSystem {
    public static final BlockPos START_POS = new BlockPos(0, -32, 0);
    public static final BlockPos SUPPLY_CHEST_POS = START_POS.offset(-3, 0, 2);
    public static final double SURFACE_ESCAPE_Y = START_POS.getY() + 20.0;
    public static final double MIRROR_ESCAPE_Y = START_POS.getY() + 44.0;
    private static final int SEGMENT_SPACING_X = 34;
    private static final int SEGMENT_DROP_Y = 12;
    private static final int SEGMENT_SPACING_Z = 18;

    private MineshaftPrisonSystem() {
    }

    public static void buildStartingChamber(ServerLevel level) {
        BlockPos center = START_POS;

        for (int x = -5; x <= 5; x++) {
            for (int y = -1; y <= 4; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    boolean wall = Math.abs(x) == 5 || Math.abs(z) == 5 || y == -1 || y == 4;
                    if (wall) {
                        level.setBlock(pos, Blocks.DEEPSLATE.defaultBlockState(), 3);
                    } else {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        for (int z = -5; z <= 5; z++) {
            level.setBlock(center.offset(-4, 0, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
            level.setBlock(center.offset(4, 0, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
            level.setBlock(center.offset(-4, 3, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
            level.setBlock(center.offset(4, 3, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
        }

        for (int z = -4; z <= 4; z += 2) {
            level.setBlock(center.offset(0, 0, z), Blocks.RAIL.defaultBlockState(), 3);
        }

        level.setBlock(center.offset(0, 1, -3), Blocks.TORCH.defaultBlockState(), 3);
        level.setBlock(center.offset(0, 1, 3), Blocks.TORCH.defaultBlockState(), 3);
        placeLoreChest(level, center.offset(0, 0, 4), 0);
        placeLoreChest(level, center.offset(-3, 0, -3), 1);
        placeLoreChest(level, center.offset(3, 0, -3), 2);
        placeLoreChest(level, center.offset(3, 0, 3), 3);
        updateSupplyChest(level);
        buildNoSurfaceColumn(level);
    }

    public static void buildNoSurfaceColumn(ServerLevel level) {
        BlockPos center = START_POS;
        for (int y = 5; y <= 96; y++) {
            for (int x = -6; x <= 6; x++) {
                for (int z = -6; z <= 6; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    boolean shell = Math.abs(x) == 6 || Math.abs(z) == 6;
                    boolean support = (Math.abs(x) == 4 || Math.abs(z) == 4) && y % 7 == 0;
                    if (shell || y % 11 == 0) {
                        level.setBlock(pos, Blocks.DEEPSLATE.defaultBlockState(), 3);
                    } else if (support) {
                        level.setBlock(pos, Blocks.OAK_LOG.defaultBlockState(), 3);
                    } else if (Math.abs(x) <= 1 && Math.abs(z) <= 1) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    } else if (level.getRandom().nextInt(28) == 0) {
                        level.setBlock(pos, Blocks.COBWEB.defaultBlockState(), 3);
                    } else {
                        level.setBlock(pos, Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 3);
                    }
                }
            }
        }

        SignTextSystem.placeStandingSign(level, center.offset(0, 6, 4), 8, "The surface", "was removed.", "", "Go below.");
    }

    public static void updateSupplyChest(ServerLevel level) {
        level.setBlock(SUPPLY_CHEST_POS, Blocks.CHEST.defaultBlockState(), 3);
        BlockEntity chest = level.getBlockEntity(SUPPLY_CHEST_POS);
        if (chest instanceof Container container) {
            int targetCount = Math.max(1, Math.min(container.getContainerSize(), level.getServer().getPlayerCount()));
            int currentCount = countIronPickaxes(container);
            for (int slot = 0; slot < container.getContainerSize() && currentCount < targetCount; slot++) {
                if (container.getItem(slot).isEmpty()) {
                    container.setItem(slot, new ItemStack(Items.IRON_PICKAXE));
                    currentCount++;
                }
            }
        }
    }

    public static void sendToStart(ServerPlayer player) {
        player.teleportTo(player.serverLevel(), START_POS.getX() + 0.5, START_POS.getY(), START_POS.getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    public static void sendDeeper(ServerPlayer player, int depth) {
        ServerLevel level = player.serverLevel();
        BlockPos deeper = segmentCenter(depth);
        buildDeepSegment(level, deeper, depth);
        player.teleportTo(level, deeper.getX() + 0.5, deeper.getY(), deeper.getZ() + 0.5, player.getYRot() + 180.0F, player.getXRot());
    }

    public static BlockPos segmentCenter(int depth) {
        int lane = Math.floorMod(depth, 4);
        int x = switch (lane) {
            case 1 -> SEGMENT_SPACING_X;
            case 2 -> 0;
            case 3 -> -SEGMENT_SPACING_X;
            default -> 0;
        };
        int z = -depth * SEGMENT_SPACING_Z;
        int y = START_POS.getY() - depth * SEGMENT_DROP_Y;
        return new BlockPos(x, y, z);
    }

    public static boolean isInsideMineRegion(ServerPlayer player) {
        BlockPos pos = player.blockPosition();
        return pos.getY() <= START_POS.getY() + 24
            && pos.getY() >= START_POS.getY() - 280
            && Math.abs(pos.getX()) <= 160
            && pos.getZ() <= 48
            && pos.getZ() >= -420;
    }

    public static boolean reachedFalseExit(ServerPlayer player, int depth) {
        BlockPos center = depth <= 0 ? START_POS : segmentCenter(depth);
        return player.getY() >= center.getY() + 4.0
            && player.blockPosition().distSqr(center.offset(0, 4, 5)) < 16.0;
    }

    private static void buildDeepSegment(ServerLevel level, BlockPos center, int depth) {
        int radius = 5 + Math.min(2, depth % 3);
        for (int x = -radius; x <= radius; x++) {
            for (int y = -1; y <= 4; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    boolean wall = Math.abs(x) == radius || Math.abs(z) == radius || y == -1 || y == 4;
                    level.setBlock(pos, wall ? wallBlock(depth) : Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }

        for (int z = -radius + 1; z <= radius - 1; z++) {
            if (z % 2 == 0) {
                level.setBlock(center.offset(0, 0, z), Blocks.RAIL.defaultBlockState(), 3);
            }
            if (z % 3 == 0) {
                level.setBlock(center.offset(-radius + 1, 0, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
                level.setBlock(center.offset(radius - 1, 0, z), Blocks.OAK_FENCE.defaultBlockState(), 3);
                level.setBlock(center.offset(-radius + 1, 3, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
                level.setBlock(center.offset(radius - 1, 3, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
            }
        }

        level.setBlock(center.offset(0, 1, 0), depth % 2 == 0 ? Blocks.REDSTONE_TORCH.defaultBlockState() : Blocks.SOUL_TORCH.defaultBlockState(), 3);
        buildFalseExit(level, center.offset(0, 0, radius - 1));
        buildSegmentVariant(level, center, radius, depth);
        if (depth % 2 == 0) {
            placeMessageChest(level, center.offset(-radius + 2, 0, -radius + 2), depth);
        }
    }

    private static void buildFalseExit(ServerLevel level, BlockPos bottom) {
        for (int y = 0; y <= 4; y++) {
            level.setBlock(bottom.above(y), Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH), 3);
        }
        level.setBlock(bottom.above(5), Blocks.DEEPSLATE.defaultBlockState(), 3);
        SignTextSystem.placeStandingSign(level, bottom.north(), 8, "THIS WAY", "UP", "", "NO WAY OUT");
    }

    private static void buildSegmentVariant(ServerLevel level, BlockPos center, int radius, int depth) {
        switch (Math.floorMod(depth, 5)) {
            case 0 -> buildStorage(level, center.offset(radius - 2, 0, -radius + 2));
            case 1 -> buildBunks(level, center, radius);
            case 2 -> buildCaveIn(level, center, radius);
            case 3 -> buildShrine(level, center, radius);
            default -> buildSplitRails(level, center, radius);
        }
    }

    private static void buildStorage(ServerLevel level, BlockPos corner) {
        level.setBlock(corner, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP), 3);
        level.setBlock(corner.west(), Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(corner.south(), Blocks.COBWEB.defaultBlockState(), 3);
        level.setBlock(corner.above(), Blocks.LANTERN.defaultBlockState(), 3);
    }

    private static void buildBunks(ServerLevel level, BlockPos center, int radius) {
        BlockPos left = center.offset(-radius + 2, 0, 1);
        BlockPos right = center.offset(radius - 3, 0, -1);
        level.setBlock(left, Blocks.OAK_SLAB.defaultBlockState(), 3);
        level.setBlock(left.east(), Blocks.OAK_SLAB.defaultBlockState(), 3);
        level.setBlock(right, Blocks.OAK_SLAB.defaultBlockState(), 3);
        level.setBlock(right.west(), Blocks.OAK_SLAB.defaultBlockState(), 3);
        SignTextSystem.placeStandingSign(level, center.offset(0, 0, -radius + 1), 0, "He slept", "here after", "he saw", "himself.");
    }

    private static void buildCaveIn(ServerLevel level, BlockPos center, int radius) {
        for (int x = -2; x <= 2; x++) {
            level.setBlock(center.offset(x, 0, -radius + 1), Blocks.GRAVEL.defaultBlockState(), 3);
            if (x % 2 == 0) {
                level.setBlock(center.offset(x, 1, -radius + 1), Blocks.COBBLED_DEEPSLATE.defaultBlockState(), 3);
            }
        }
        SignTextSystem.placeStandingSign(level, center.offset(2, 0, -radius + 2), 4, "The exit", "fell upward.", "", "");
    }

    private static void buildShrine(ServerLevel level, BlockPos center, int radius) {
        level.setBlock(center.offset(0, 0, -radius + 2), Blocks.SCULK.defaultBlockState(), 3);
        level.setBlock(center.offset(1, 0, -radius + 2), Blocks.SCULK_VEIN.defaultBlockState(), 3);
        level.setBlock(center.offset(-1, 0, -radius + 2), Blocks.SCULK_VEIN.defaultBlockState(), 3);
        level.setBlock(center.offset(0, 1, -radius + 2), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
        SignTextSystem.placeStandingSign(level, center.offset(0, 0, -radius + 3), 8, "It learned", "your route.", "It kept", "your name.");
    }

    private static void buildSplitRails(ServerLevel level, BlockPos center, int radius) {
        for (int i = 0; i < 4; i++) {
            level.setBlock(center.offset(i, 0, i - 2), Blocks.RAIL.defaultBlockState(), 3);
            level.setBlock(center.offset(-i, 0, i - 2), Blocks.RAIL.defaultBlockState(), 3);
        }
        SignTextSystem.placeStandingSign(level, center.offset(0, 0, radius - 3), 8, "Both paths", "go below.", "", "");
    }

    private static void placeMessageChest(ServerLevel level, BlockPos pos, int depth) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        BlockEntity chest = level.getBlockEntity(pos);
        if (chest instanceof Container container) {
            LoreBookSystem.addMessageBook(container, depth % 2);
        }
    }

    private static net.minecraft.world.level.block.state.BlockState wallBlock(int depth) {
        return switch (depth % 4) {
            case 1 -> Blocks.COBBLED_DEEPSLATE.defaultBlockState();
            case 2 -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            case 3 -> Blocks.TUFF.defaultBlockState();
            default -> Blocks.DEEPSLATE.defaultBlockState();
        };
    }

    private static void placeLoreChest(ServerLevel level, BlockPos pos, int loreIndex) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        BlockEntity chest = level.getBlockEntity(pos);
        if (chest instanceof Container container) {
            LoreBookSystem.addLoreBook(container, loreIndex);
        }
    }

    private static int countIronPickaxes(Container container) {
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).is(Items.IRON_PICKAXE)) {
                count++;
            }
        }
        return count;
    }
}
