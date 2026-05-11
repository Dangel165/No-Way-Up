package com.nowayup.system;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.Rotations;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class WatcherIllusionSystem {
    public static final String WATCHER_TAG = "nowayup_watcher";

    private WatcherIllusionSystem() {
    }

    public static boolean spawnWatcher(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 origin = player.position();
        Vec3 target = origin.add(look.scale(player.getRandom().nextBoolean() ? -18.0 : 28.0));

        HitResult hit = level.clip(new ClipContext(origin.add(0, 1.4, 0), target.add(0, 1.4, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.MISS) {
            target = hit.getLocation().subtract(look.scale(2.0));
        }

        ArmorStand watcher = new ArmorStand(EntityType.ARMOR_STAND, level);
        watcher.moveTo(target.x, Math.floor(target.y), target.z, player.getYRot() + 180.0F, 0.0F);
        watcher.setInvisible(false);
        watcher.setNoGravity(true);
        watcher.setInvulnerable(true);
        watcher.setSilent(true);
        watcher.setGlowingTag(player.getRandom().nextInt(4) == 0);
        watcher.setCustomNameVisible(false);
        watcher.setYHeadRot(player.getYRot() + 180.0F);
        watcher.setHeadPose(new Rotations(0.0F, player.getYRot() + 180.0F, 0.0F));
        watcher.addTag(WATCHER_TAG);

        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        CompoundTag skullOwner = NbtUtils.writeGameProfile(new CompoundTag(), new GameProfile(player.getUUID(), player.getGameProfile().getName()));
        head.getOrCreateTag().put("SkullOwner", skullOwner);
        watcher.setItemSlot(EquipmentSlot.HEAD, head);
        watcher.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.LEATHER_CHESTPLATE));
        watcher.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        watcher.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));

        return level.addFreshEntity(watcher);
    }
}
