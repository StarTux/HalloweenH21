package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Cuboid;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.util.Json;
import com.cavetale.halloween.HalloweenPlugin;
import com.cavetale.resident.PluginSpawn;
import com.cavetale.resident.ZoneType;
import com.cavetale.resident.save.Loc;
import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

/**
 * Base class for all attractions.
 * @param <T> the save tag class
 */
@Getter
public abstract class Attraction<T extends Attraction.SaveTag> {
    protected final HalloweenPlugin plugin;
    protected final String name;
    protected final List<Cuboid> allAreas;
    protected final File saveFile;
    protected final Cuboid mainArea;
    protected final Class<T> saveTagClass;
    protected PluginSpawn mainVillager;
    protected final Random random = ThreadLocalRandom.current();
    protected T saveTag;
    protected Supplier<T> saveTagSupplier;

    public static Attraction of(HalloweenPlugin plugin, @NonNull final String name, @NonNull final List<Cuboid> areaList) {
        if (areaList.isEmpty()) throw new IllegalArgumentException(name + ": area list is empty");
        if (areaList.get(0).name == null) throw new IllegalArgumentException(name + ": first area has no name!");
        String typeName = areaList.get(0).name;
        switch (typeName) {
        case "repeat_melody": return new RepeatMelodyAttraction(plugin, name, areaList);
        case "shoot_target": return new ShootTargetAttraction(plugin, name, areaList);
        default:
            throw new IllegalArgumentException(name + ": first area has unknown name: " + typeName);
        }
    }

    protected Attraction(final HalloweenPlugin plugin, final String name, final List<Cuboid> areaList,
                         final Class<T> saveTagClass, final Supplier<T> saveTagSupplier) {
        this.plugin = plugin;
        this.name = name;
        this.allAreas = areaList;
        this.saveFile = new File(plugin.getAttractionsFolder(), name + ".json");
        this.mainArea = areaList.get(0);
        this.saveTagClass = saveTagClass;
        this.saveTagSupplier = saveTagSupplier;
        for (Cuboid area : areaList) {
            if ("npc".equals(area.name)) {
                Location location = area.min.toBlock(plugin.getWorld()).getLocation().add(0.5, 0.0, 0.5);
                mainVillager = PluginSpawn.register(plugin, ZoneType.HALLOWEEN, Loc.of(location));
                mainVillager.setOnPlayerClick(this::clickMainVillager);
            }
        }
    }


    public final boolean isInArea(Location location) {
        return mainArea.contains(location);
    }

    public final void load() {
        saveTag = Json.load(saveFile, saveTagClass, saveTagSupplier);
        onLoad();
    }

    public final void save() {
        onSave();
        if (saveTag != null) {
            Json.save(saveFile, saveTag, true);
        }
    }

    public final void enable() {
        plugin.getLogger().info("Enabling " + name);
        onEnable();
    }

    public final void disable() {
        onDisable();
    }

    public final void tick() {
        onTick();
    }

    protected final void timeout(Player player) {
        player.showTitle(Title.title(Component.text("Timeout", NamedTextColor.DARK_RED),
                                     Component.text("Try Again", NamedTextColor.DARK_RED)));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.MASTER, 2.0f, 0.5f);
        plugin.sessionOf(player).setCooldown(this, Duration.ofSeconds(10));
    }

    protected final void fail(Player player) {
        player.showTitle(Title.title(Component.text("Wrong", NamedTextColor.DARK_RED),
                                     Component.text("Try Again", NamedTextColor.DARK_RED)));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.MASTER, 2.0f, 0.5f);
        plugin.sessionOf(player).setCooldown(this, Duration.ofSeconds(3));
    }

    protected final void progress(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.3f, 2.0f);
    }

    protected final void victory(Player player) {
        // TODO Regular reward
        // TODO cooldown
        Component message = Component.text("Complete", NamedTextColor.GOLD);
        player.showTitle(Title.title(message, Component.text("Good Job!", NamedTextColor.GOLD)));
        player.sendMessage(message);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.3f, 2.0f);
        plugin.sessionOf(player).setCooldown(this, Duration.ofMinutes(20));
    }

    protected final void perfect(Player player) {
        // TODO Perfect reward
        // TODO cooldown
        Component message = Component.join(JoinConfiguration.noSeparators(), new Component[] {
                Component.text("P", NamedTextColor.GOLD),
                Component.text("E", NamedTextColor.RED),
                Component.text("R", NamedTextColor.YELLOW),
                Component.text("F", NamedTextColor.GOLD),
                Component.text("E", NamedTextColor.RED),
                Component.text("C", NamedTextColor.YELLOW),
                Component.text("T", NamedTextColor.GOLD),
                Component.text("!", NamedTextColor.DARK_RED, TextDecoration.BOLD),
            });
        player.showTitle(Title.title(message, Component.empty()));
        player.sendMessage(message);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.3f, 2.0f);
        plugin.sessionOf(player).setCooldown(this, Duration.ofMinutes(20));
    }

    /**
     * Override me to tell if this attraction is currently playing!
     */
    public abstract boolean isPlaying();

    public final Player getCurrentPlayer() {
        return saveTag.currentPlayer != null
            ? Bukkit.getPlayer(saveTag.currentPlayer)
            : null;
    }

    protected abstract void onTick();

    protected void onEnable() { }

    protected void onDisable() { }

    protected void onLoad() { }

    protected void onSave() { }

    /**
     * Override me when a player starts the game!
     */
    protected abstract void start(Player player);

    protected final void clickMainVillager(Player player) {
        // TODO dialogue
        // TODO cooldowns
        // TODO price of admission
        if (isPlaying()) return;
        start(player);
    }

    public void onPluginPlayer(PluginPlayerEvent event) { }

    protected abstract static class SaveTag {
        protected UUID currentPlayer = null;
    }

    protected final void subtitle(Player player, Component component) {
        player.showTitle(Title.title(Component.empty(), component));
    }
}
