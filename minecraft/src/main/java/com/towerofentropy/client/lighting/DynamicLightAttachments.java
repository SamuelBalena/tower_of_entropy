package com.towerofentropy.client.lighting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Client-side registration for visibly equipped attachments. Call providers on the render thread. */
public final class DynamicLightAttachments {
    public enum Socket { MAIN_HAND, OFF_HAND, BACK_LEFT, BACK_RIGHT, BACK_CENTER, BELT_LEFT, BELT_RIGHT }

    public record Emission(Vec3 color, float strength, float radius, float flicker) {
        public Emission {
            Objects.requireNonNull(color);
            for (double channel : new double[]{color.x, color.y, color.z}) {
                if (!Double.isFinite(channel) || channel < 0 || channel > 1) throw new IllegalArgumentException("Light color");
            }
            if (!Float.isFinite(strength) || strength < 0 || strength > 1 ||
                    !Float.isFinite(radius) || radius < 0.5f || radius > 7.5f ||
                    !Float.isFinite(flicker) || flicker < 0 || flicker > 0.08f)
                throw new IllegalArgumentException("Light emission outside supported range");
        }
    }

    /** Omit worldPosition to use the socket. Omit emission to use the equipped item's vanilla profile. */
    public record Attachment(ResourceLocation id, Socket socket, ItemStack item, Vec3 worldPosition,
                             Emission emission, boolean active) {
        public Attachment {
            Objects.requireNonNull(id);
            Objects.requireNonNull(socket);
            Objects.requireNonNull(item);
        }
    }

    @FunctionalInterface
    public interface Provider {
        List<Attachment> equippedLights(AbstractClientPlayer owner, float partialTick);
    }

    public interface Registration extends AutoCloseable {
        @Override void close();
    }

    private record Entry(Provider provider) {}

    private static final LinkedHashMap<ResourceLocation, Entry> PROVIDERS = new LinkedHashMap<>();
    private static List<Provider> snapshot = List.of();

    private DynamicLightAttachments() {}

    public static synchronized Registration register(ResourceLocation id, Provider provider) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(provider);
        Entry entry = new Entry(provider);
        if (PROVIDERS.putIfAbsent(id, entry) != null) throw new IllegalArgumentException("Duplicate provider " + id);
        updateSnapshot();
        return () -> unregister(id, entry);
    }

    private static synchronized void unregister(ResourceLocation id, Entry entry) {
        if (PROVIDERS.get(id) == entry) {
            PROVIDERS.remove(id);
            updateSnapshot();
        }
    }

    private static void updateSnapshot() {
        snapshot = PROVIDERS.values().stream().map(Entry::provider).toList();
    }

    static synchronized List<Provider> providers() {
        return snapshot;
    }
}
