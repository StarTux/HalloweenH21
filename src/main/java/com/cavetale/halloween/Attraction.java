package com.cavetale.halloween;

import com.cavetale.area.struct.Cuboid;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.util.Json;
import com.cavetale.resident.PluginSpawn;
import com.cavetale.resident.ZoneType;
import com.cavetale.resident.save.Loc;
import java.io.File;
import java.time.Duration;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

/**
 * Base class for all attractions.
 */
@Getter
public abstract class Attraction {
    protected final HalloweenPlugin plugin;
    protected final String name;
    protected final List<Cuboid> allAreas;
    protected final File saveFile;
    protected final Cuboid mainArea;
    protected PluginSpawn mainVillager;

    static Attraction of(HalloweenPlugin plugin, @NonNull final String name, @NonNull final List<Cuboid> areaList) {
        if (areaList.isEmpty()) throw new IllegalArgumentException(name + ": area list is empty");
        if (areaList.get(0).name == null) throw new IllegalArgumentException(name + ": first area has no name!");
        String typeName = areaList.get(0).name;
        switch (typeName) {
        case "play_music": return new PlayMusicAttraction(plugin, name, areaList);
        default:
            throw new IllegalArgumentException(name + ": first area has unknown name: " + typeName);
        }
    }

    protected Attraction(final HalloweenPlugin plugin, final String name, final List<Cuboid> areaList) {
        this.plugin = plugin;
        this.name = name;
        this.allAreas = areaList;
        this.saveFile = new File(plugin.attractionsFolder, name + ".json");
        this.mainArea = areaList.get(0);
        for (Cuboid area : areaList) {
            if ("npc".equals(area.name)) {
                Location location = area.min.toBlock(plugin.getWorld()).getLocation().add(0.5, 0.0, 0.5);
                mainVillager = PluginSpawn.register(plugin, ZoneType.HALLOWEEN, Loc.of(location));
                mainVillager.setOnPlayerClick(this::onClickMainVillager);
            }
        }
    }

    protected final void load() {
        Object saveTag = getSaveTag();
        if (saveTag != null) {
            Json.save(saveFile, saveTag, true);
        }
        onLoad();
    }

    protected final void save() {
        onSave();
        if (saveFile.exists()) {
            Class<?> saveTagClass = getSaveTagClass();
            if (saveTagClass != null) {
                Object saveTag = Json.load(saveFile, saveTagClass);
                if (saveTag != null) {
                    setSaveTag(saveTag);
                }
            }
        }
    }

    protected final void enable() {
        plugin.getLogger().info("Enabling " + name);
        onEnable();
    }

    protected final void disable() {
        onDisable();
    }

    protected final void tick() {
        onTick();
    }

    protected final void timeout(Player player) {
        player.showTitle(Title.title(Component.text("Timeout", NamedTextColor.DARK_RED),
                                     Component.text("Try Again", NamedTextColor.DARK_RED)));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.MASTER, 2.0f, 0.5f);
        plugin.sessionOf(player).setCooldown(this, Duration.ofSeconds(10));
    }

    protected final void wrong(Player player) {
        player.showTitle(Title.title(Component.text("Wrong", NamedTextColor.DARK_RED),
                                     Component.text("Try Again", NamedTextColor.DARK_RED)));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.MASTER, 2.0f, 0.5f);
        plugin.sessionOf(player).setCooldown(this, Duration.ofSeconds(3));
    }

    protected final void progress(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.3f, 2.0f);
    }

    protected final void victory(Player player) {
        player.showTitle(Title.title(Component.text("Complete", NamedTextColor.GREEN),
                                     Component.text("Good Job!", NamedTextColor.GREEN)));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.3f, 2.0f);
        plugin.sessionOf(player).setCooldown(this, Duration.ofMinutes(20));
    }

    protected void onTick() { }

    protected void onEnable() { }

    protected void onDisable() { }

    protected void onLoad() { }

    protected void onSave() { }

    /**
     * Set the save tag after it was loaded.
     */
    protected void setSaveTag(Object saveTag) { }

    /**
     * Return the save tag.
     */
    protected Object getSaveTag() {
        return null;
    }

    protected final Class<?> getSaveTagClass() {
        Object saveTag = getSaveTag();
        return saveTag != null ? saveTag.getClass() : null;
    }

    protected void onClickMainVillager(Player player) { }

    protected void onPluginPlayer(PluginPlayerEvent event) { }
}
