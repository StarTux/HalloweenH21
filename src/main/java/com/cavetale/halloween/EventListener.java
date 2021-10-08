package com.cavetale.halloween;

import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.halloween.attraction.Attraction;
import com.cavetale.halloween.attraction.ShootTargetAttraction;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public final class EventListener implements Listener {
    private final HalloweenPlugin plugin;
    protected boolean enabled = true;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerJoin(PlayerJoinEvent event) {
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerQuit(PlayerQuitEvent event) {
        plugin.clearSession(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        plugin.clearSession(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPluginPlayer(PluginPlayerEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getWorld().equals(player.getWorld())) return;
        for (Attraction attraction : new ArrayList<>(plugin.attractionsMap.values())) {
            if (!attraction.isInArea(player.getLocation())) continue;
            attraction.onPluginPlayer(event);
            break;
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    void onProjectileHit(ProjectileHitEvent event) {
        Projectile entity = event.getEntity();
        if (!plugin.getWorld().equals(entity.getWorld())) return;
        for (Attraction attraction : new ArrayList<>(plugin.attractionsMap.values())) {
            if (!attraction.isInArea(entity.getLocation())) continue;
            if (attraction instanceof ShootTargetAttraction) {
                ((ShootTargetAttraction) attraction).onProjectileHit(event);
            }
            break;
        }
    }
}
