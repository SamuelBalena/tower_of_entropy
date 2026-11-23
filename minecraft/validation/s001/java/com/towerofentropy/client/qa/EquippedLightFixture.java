package com.towerofentropy.client.qa;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;

interface EquippedLightFixture {
    void setClientScene(Minecraft mc, String scene);
    void prepare(ServerPlayer player, String scene);
    void tick(Minecraft mc, String scene, int tick, int warmup);

    static EquippedLightFixture load() {
        try {
            return (EquippedLightFixture) Class.forName(
                    "com.towerofentropy.client.qa.EquippedLightGallery").getConstructor().newInstance();
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Equipment fixtures require the full Tower of Entropy client", error);
        }
    }
}
