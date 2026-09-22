package com.galaxyleveling.storage;

import com.galaxyleveling.model.PlayerProfile;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ProfileStorage {
    CompletableFuture<PlayerProfile> loadProfile(UUID uuid);
    CompletableFuture<Void> saveProfile(PlayerProfile profile);
    void close();
}
