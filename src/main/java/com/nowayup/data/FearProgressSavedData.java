package com.nowayup.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class FearProgressSavedData extends SavedData {
    private static final String DATA_NAME = "nowayup_fear_progress";

    private final Map<UUID, PlayerFearState> playerStates = new HashMap<>();
    private boolean worldInitialized;

    public static FearProgressSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FearProgressSavedData::load, FearProgressSavedData::new, DATA_NAME);
    }

    public static FearProgressSavedData load(CompoundTag tag) {
        FearProgressSavedData data = new FearProgressSavedData();
        data.worldInitialized = tag.getBoolean("WorldInitialized");
        ListTag players = tag.getList("Players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag playerTag = players.getCompound(i);
            UUID id = playerTag.getUUID("Id");
            data.playerStates.put(id, PlayerFearState.load(playerTag.getCompound("State")));
        }
        return data;
    }

    public PlayerFearState stateFor(UUID playerId) {
        return playerStates.computeIfAbsent(playerId, id -> new PlayerFearState());
    }

    public boolean worldInitialized() {
        return worldInitialized;
    }

    public void setWorldInitialized() {
        worldInitialized = true;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("WorldInitialized", worldInitialized);
        ListTag players = new ListTag();
        for (Map.Entry<UUID, PlayerFearState> entry : playerStates.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("Id", entry.getKey());
            playerTag.put("State", entry.getValue().save());
            players.add(playerTag);
        }
        tag.put("Players", players);
        return tag;
    }
}
