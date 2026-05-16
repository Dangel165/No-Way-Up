package com.nowayup.system;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;

public final class SignTextSystem {
    private SignTextSystem() {
    }

    public static void placeStandingSign(ServerLevel level, BlockPos pos, int rotation, String... lines) {
        level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation), 3);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SignBlockEntity sign) {
            SignText text = new SignText().setColor(DyeColor.BLACK);
            for (int i = 0; i < Math.min(4, lines.length); i++) {
                text = text.setMessage(i, NoWayUpText.signLine(lines[i]));
            }
            sign.setText(text, true);
            sign.setText(text, false);
            sign.setChanged();
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            Packet<ClientGamePacketListener> packet = sign.getUpdatePacket();
            if (packet != null) {
                for (ServerPlayer player : level.players()) {
                    player.connection.send(packet);
                }
            }
        }
    }
}
