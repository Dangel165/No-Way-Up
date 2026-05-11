package com.nowayup.system;

import com.nowayup.data.PlayerFearState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class EnvironmentMutationSystem {
    private static final String[] SIGN_LINES = {
        "Why did you",
        "come back?",
        "You blocked",
        "the exit."
    };

    private EnvironmentMutationSystem() {
    }

    public static void mutateOnReconnect(ServerLevel level, PlayerFearState state) {
        if (state.reconnectCount() <= state.environmentMutationCount()) {
            return;
        }

        int step = state.environmentMutationCount() + 1;
        if (step == 1) {
            extinguishTorches(level);
            placeOpenDoor(level, MineshaftPrisonSystem.START_POS.offset(-2, 0, 4), Direction.SOUTH);
        } else if (step == 2) {
            SignTextSystem.placeStandingSign(level, MineshaftPrisonSystem.START_POS.offset(0, 0, -4), 8, SIGN_LINES);
            placeMessageChest(level, MineshaftPrisonSystem.START_POS.offset(-3, 0, 3), 0);
        } else if (step == 3) {
            SignTextSystem.placeStandingSign(level, MineshaftPrisonSystem.START_POS.offset(4, 0, 0), 12,
                "That was not",
                "you passing",
                "by.",
                ""
            );
            placeMessageChest(level, MineshaftPrisonSystem.START_POS.offset(-4, 0, 0), 1);
        } else {
            SignTextSystem.placeStandingSign(level, MineshaftPrisonSystem.START_POS.offset(0, 0, 0), 0,
                "You are not",
                "the first.",
                "Do not",
                "climb."
            );
        }

        state.incrementEnvironmentMutationCount();
        state.addFear(15);
    }

    public static void applyLateMutation(ServerLevel level, PlayerFearState state) {
        if (state.fearProgress() < 120) {
            return;
        }
        extinguishTorches(level);
    }

    private static void extinguishTorches(ServerLevel level) {
        replaceTorch(level, MineshaftPrisonSystem.START_POS.offset(0, 1, -3));
        replaceTorch(level, MineshaftPrisonSystem.START_POS.offset(0, 1, 3));
    }

    private static void replaceTorch(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(Blocks.TORCH)) {
            level.setBlock(pos, Blocks.SOUL_TORCH.defaultBlockState(), 3);
        }
    }

    private static void placeOpenDoor(ServerLevel level, BlockPos bottom, Direction facing) {
        level.setBlock(bottom, Blocks.OAK_DOOR.defaultBlockState()
            .setValue(DoorBlock.FACING, facing)
            .setValue(DoorBlock.OPEN, true)
            .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 3);
        level.setBlock(bottom.above(), Blocks.OAK_DOOR.defaultBlockState()
            .setValue(DoorBlock.FACING, facing)
            .setValue(DoorBlock.OPEN, true)
            .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 3);
    }

    private static void placeMessageChest(ServerLevel level, BlockPos pos, int bookIndex) {
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 3);
        BlockEntity chest = level.getBlockEntity(pos);
        if (chest instanceof Container container) {
            LoreBookSystem.addMessageBook(container, bookIndex);
        }
    }
}
