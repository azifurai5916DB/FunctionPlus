package com.example.functionplus;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class MobCommands implements CommandExecutor {

    private final FunctionPlus plugin;

    public MobCommands(FunctionPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("このコマンドはプレイヤーのみ実行できます。");
            return true;
        }

        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("moblook")) {
            handleMobLook(player);
            plugin.addLog(player, cmdName, "TRANSPARENT_MOBS");
            return true;
        }

        LivingEntity target = getTargetMob(player);
        if (target == null) {
            player.sendMessage("§cカーソル先にMobが見つかりません。");
            return true;
        }

        PersistentDataContainer pdc = target.getPersistentDataContainer();

        switch (cmdName) {
            case "mobtoumei" -> {
                boolean isTransparent = pdc.has(plugin.transparentKey, PersistentDataType.BYTE);
                if (!isTransparent) {
                    pdc.set(plugin.transparentKey, PersistentDataType.BYTE, (byte) 1);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false, false));
                    new MobLookTask(plugin).refreshGlowingForEnabledPlayers(target, true);
                    player.sendMessage("Mobを透明化しました。");
                } else {
                    pdc.remove(plugin.transparentKey);
                    target.removePotionEffect(PotionEffectType.INVISIBILITY);
                    new MobLookTask(plugin).refreshGlowingForEnabledPlayers(target, false);
                    player.sendMessage("Mobの透明化を解除しました。");
                }
            }
            case "kotei" -> {
                boolean isFrozen = pdc.has(plugin.frozenKey, PersistentDataType.BYTE);
                if (!isFrozen) {
                    pdc.set(plugin.frozenKey, PersistentDataType.BYTE, (byte) 1);
                    target.setAI(false);
                    player.sendMessage("Mobを固定しました。");
                } else {
                    pdc.remove(plugin.frozenKey);
                    target.setAI(true);
                    player.sendMessage("Mobの固定を解除しました。");
                }
            }
            case "mobkoe" -> {
                boolean isMuted = pdc.has(plugin.muteKey, PersistentDataType.BYTE);
                if (!isMuted) {
                    pdc.set(plugin.muteKey, PersistentDataType.BYTE, (byte) 1);
                    target.setSilent(true);
                    player.sendMessage("Mobの声を無効化しました。");
                } else {
                    pdc.remove(plugin.muteKey);
                    target.setSilent(false);
                    player.sendMessage("Mobの声を有効化しました。");
                }
            }
            default -> {
                return false;
            }
        }

        plugin.addLog(player, cmdName, target);
        return true;
    }

    private LivingEntity getTargetMob(Player player) {
        Entity target = player.getTargetEntity(10, false);
        if (target instanceof LivingEntity livingEntity) {
            if (livingEntity instanceof Player) return null;
            if (livingEntity instanceof ArmorStand) return null;
            return livingEntity;
        }
        return null;
    }

    private void handleMobLook(Player player) {
        if (!plugin.isProtocolLibAvailable()) {
            player.sendMessage("§cProtocolLib が見つからないため /moblook は使用できません。");
            return;
        }

        boolean hasTransparentMob = false;
        for (World world : plugin.getServer().getWorlds()) {
            for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class)) {
                if (entity.getPersistentDataContainer().has(plugin.transparentKey, PersistentDataType.BYTE)) {
                    hasTransparentMob = true;
                    break;
                }
            }
            if (hasTransparentMob) break;
        }

        if (!hasTransparentMob) {
            player.sendMessage("透明化されているMobが存在しません。");
            return;
        }

        if (plugin.getMobLookEnabledPlayers().remove(player.getUniqueId())) {
            new MobLookTask(plugin).clearGlowingForPlayer(player);
            player.sendMessage("透明化Mobの輪郭表示を無効化しました。");
        } else {
            plugin.getMobLookEnabledPlayers().add(player.getUniqueId());
            new MobLookTask(plugin).showGlowingForPlayer(player);
            player.sendMessage("透明化Mobの輪郭表示を有効化しました。");
        }
    }
}
