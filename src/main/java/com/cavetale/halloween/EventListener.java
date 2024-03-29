package com.cavetale.halloween;

import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.halloween.attraction.Attraction;
import com.cavetale.halloween.attraction.Festival;
import com.cavetale.halloween.attraction.MusicHeroAttraction;
import com.cavetale.halloween.attraction.ShootTargetAttraction;
import com.cavetale.magicmap.event.MagicMapCursorEvent;
import com.cavetale.magicmap.util.Cursors;
import com.cavetale.mytems.event.music.PlayerBeatEvent;
import com.cavetale.mytems.event.music.PlayerCloseMusicalInstrumentEvent;
import com.cavetale.mytems.event.music.PlayerMelodyCompleteEvent;
import com.cavetale.mytems.event.music.PlayerOpenMusicalInstrumentEvent;
import com.destroystokyo.paper.event.entity.ThrownEggHatchEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.map.MapCursor;

@RequiredArgsConstructor
public final class EventListener implements Listener {
    private final HalloweenPlugin plugin;
    protected boolean enabled = true;

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    protected void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (Attraction attraction : plugin.getAttractions(player.getWorld())) {
            attraction.onPlayerQuit(event);
        }
        plugin.clearSession(player.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    protected void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        plugin.clearSession(player.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    protected void onPluginPlayer(PluginPlayerEvent event) {
        Player player = event.getPlayer();
        for (Attraction attraction : plugin.getAttractions(player.getWorld())) {
            if (!attraction.isInArea(player.getLocation())) continue;
            attraction.onPluginPlayer(event);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    protected void onProjectileHit(ProjectileHitEvent event) {
        Projectile entity = event.getEntity();
        for (Attraction attraction : plugin.getAttractions(entity.getWorld())) {
            if (!attraction.isInArea(entity.getLocation())) continue;
            if (attraction instanceof ShootTargetAttraction) {
                ((ShootTargetAttraction) attraction).onProjectileHit(event);
            }
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    protected void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        Location location = entity.getLocation();
        Attraction attraction = plugin.getAttraction(location);
        if (attraction != null && attraction.isInArea(location)) {
            attraction.onEntityDamage(event);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    protected void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        Location location = entity.getLocation();
        for (Attraction attraction : plugin.getAttractions(entity.getWorld())) {
            if (!attraction.isInArea(location)) continue;
            attraction.onEntityDamageByEntity(event);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    protected void onEntityCombust(EntityCombustEvent event) {
        Attraction attraction = plugin.getAttraction(event.getEntity().getLocation());
        if (attraction == null) return;
        attraction.onEntityCombust(event);
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    protected void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Location location = entity.getLocation();
        for (Attraction attraction : plugin.getAttractions(entity.getWorld())) {
            if (!attraction.isInArea(location)) continue;
            attraction.onPlayerInteractEntity(event);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    protected void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasBlock()) return;
        Location location = event.getClickedBlock().getLocation();
        for (Attraction attraction : plugin.getAttractions(location.getWorld())) {
            if (!attraction.isInArea(location)) continue;
            attraction.onPlayerInteract(event);
        }
    }

    @EventHandler
    protected void onPlayerOpenMusicalInstrument(PlayerOpenMusicalInstrumentEvent event) {
        plugin.applyActiveAttraction(MusicHeroAttraction.class, m -> m.onPlayerOpenMusicalInstrument(event));
    }

    @EventHandler
    protected void onPlayerCloseMusicalInstrument(PlayerCloseMusicalInstrumentEvent event) {
        plugin.applyActiveAttraction(MusicHeroAttraction.class, m -> m.onPlayerCloseMusicalInstrument(event));
    }

    @EventHandler
    protected void onPlayerBeat(PlayerBeatEvent event) {
        plugin.applyActiveAttraction(MusicHeroAttraction.class, m -> m.onPlayerBeat(event));
    }

    @EventHandler
    protected void onPlayerMelodyComplete(PlayerMelodyCompleteEvent event) {
        plugin.applyActiveAttraction(MusicHeroAttraction.class, m -> m.onPlayerMelodyComplete(event));
    }

    @EventHandler
    protected void onMagicMapCursor(MagicMapCursorEvent event) {
        Festival festival = plugin.getFestival(event.getPlayer().getWorld());
        if (festival == null) return;
        Session session = plugin.sessionOf(event.getPlayer());
        for (Attraction attraction : festival.getAttractions()) {
            Vec3i vec = attraction.getNpcVector();
            if (vec == null) continue;
            if (vec.x < event.getMinX() || vec.x > event.getMaxX()) continue;
            if (vec.z < event.getMinZ() || vec.z > event.getMaxZ()) continue;
            boolean completed = session.isUniqueLocked(attraction);
            boolean pickedUp = session.getPrizeWaiting(attraction) != 2;
            MapCursor.Type cursorType;
            if (completed && pickedUp) {
                cursorType = MapCursor.Type.MANSION;
            } else if (completed && !pickedUp) {
                cursorType = MapCursor.Type.RED_MARKER;
            } else {
                cursorType = MapCursor.Type.RED_X;
            }
            MapCursor mapCursor = Cursors.make(cursorType,
                                               vec.x - event.getMinX(),
                                               vec.z - event.getMinZ(),
                                               8);
            event.getCursors().addCursor(mapCursor);
        }
    }

    @EventHandler
    protected void onThrownEggHatch(ThrownEggHatchEvent event) {
        Festival festival = plugin.getFestival(event.getEgg().getWorld());
        if (festival != null) {
            event.setHatching(false);
        }
    }

    @EventHandler
    protected void onPlayerHud(PlayerHudEvent event) {
        Attraction attraction = plugin.getAttraction(event.getPlayer().getLocation());
        if (attraction != null && attraction.isCurrentPlayer(event.getPlayer())) {
            attraction.onPlayerHud(event);
        }
    }
}
