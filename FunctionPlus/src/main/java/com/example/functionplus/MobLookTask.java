package com.example.functionplus;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public class MobLookTask implements Runnable {

    private static final int ENTITY_FLAGS_INDEX = 0;
    private static final byte GLOWING_FLAG = 0x40;

    private final FunctionPlus plugin;

    public MobLookTask(FunctionPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.isProtocolLibAvailable() || plugin.getMobLookEnabledPlayers().isEmpty()) return;

        for (UUID uuid : plugin.getMobLookEnabledPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) continue;
            showGlowingForPlayer(player);
        }
    }

    public void showGlowingForPlayer(Player player) {
        if (!plugin.isProtocolLibAvailable()) return;

        World world = player.getWorld();
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class)) {
            if (isTransparentMob(entity)) {
                sendGlowingMetadata(player, entity, true);
            }
        }
    }

    public void clearGlowingForPlayer(Player player) {
        if (!plugin.isProtocolLibAvailable()) return;

        World world = player.getWorld();
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class)) {
            if (isTransparentMob(entity)) {
                sendGlowingMetadata(player, entity, false);
            }
        }
    }

    public void refreshGlowingForEnabledPlayers(LivingEntity entity, boolean glowing) {
        if (!plugin.isProtocolLibAvailable()) return;

        for (UUID uuid : plugin.getMobLookEnabledPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && player.getWorld().equals(entity.getWorld())) {
                sendGlowingMetadata(player, entity, glowing);
            }
        }
    }

    private boolean isTransparentMob(LivingEntity entity) {
        return entity.getPersistentDataContainer().has(plugin.transparentKey, PersistentDataType.BYTE);
    }

    private void sendGlowingMetadata(Player player, LivingEntity entity, boolean glowing) {
        byte flags = readEntityFlags(entity);
        flags = glowing ? (byte) (flags | GLOWING_FLAG) : (byte) (flags & ~GLOWING_FLAG);

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, entity.getEntityId());
        packet.getDataValueCollectionModifier().write(0, List.of(
                new WrappedDataValue(
                        ENTITY_FLAGS_INDEX,
                        WrappedDataWatcher.Registry.get(Byte.class),
                        flags
                )
        ));

        try {
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet, false);
        } catch (RuntimeException e) {
            plugin.getLogger().warning("moblook パケット送信に失敗しました: " + e.getMessage());
        }
    }

    private byte readEntityFlags(LivingEntity entity) {
        WrappedDataWatcher watcher = WrappedDataWatcher.getEntityWatcher(entity);
        if (!watcher.hasIndex(ENTITY_FLAGS_INDEX)) {
            return 0;
        }

        Byte flags = watcher.getByte(ENTITY_FLAGS_INDEX);
        return flags == null ? 0 : flags;
    }
}
