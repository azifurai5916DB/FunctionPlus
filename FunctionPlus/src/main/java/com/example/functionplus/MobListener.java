package com.example.functionplus;

import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MobListener implements Listener {

    private final FunctionPlus plugin;

    public MobListener(FunctionPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        event.getEntities().forEach(entity -> {
            if (entity instanceof LivingEntity living) {
                if (living.getPersistentDataContainer().has(plugin.frozenKey, PersistentDataType.BYTE)) {
                    living.setAI(false);
                }
                if (living.getPersistentDataContainer().has(plugin.transparentKey, PersistentDataType.BYTE)) {
                    living.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false, false));
                }
                if (living.getPersistentDataContainer().has(plugin.muteKey, PersistentDataType.BYTE)) {
                    living.setSilent(true);
                }
            }
        });
    }

    @EventHandler
    public void onEntityMove(EntityMoveEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(plugin.frozenKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(plugin.frozenKey, PersistentDataType.BYTE)) {
            event.getEntity().setVelocity(new org.bukkit.util.Vector(0, 0, 0));
        }
    }
}
