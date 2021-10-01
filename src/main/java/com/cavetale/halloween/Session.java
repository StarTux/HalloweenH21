package com.cavetale.halloween;

import com.cavetale.core.util.Json;
import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class Session {
    protected final HalloweenPlugin plugin;
    protected final UUID uuid;
    protected final String name;
    protected final File saveFile;
    protected Tag tag;

    protected Session(final HalloweenPlugin plugin, final Player player) {
        this.plugin = plugin;
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.saveFile = new File(plugin.playersFolder, uuid + ".json");
    }

    protected void load() {
        this.tag = Json.load(saveFile, Tag.class, Tag::new);
    }

    protected void save() {
        Json.save(saveFile, tag);
    }

    public long getCooldown(Attraction attraction) {
        return tag.attractionCooldowns.computeIfAbsent(attraction.getName(), s -> 0L);
    }

    public void setCooldown(Attraction attraction, Duration duration) {
        tag.attractionCooldowns.put(attraction.getName(), duration.toMillis() + System.currentTimeMillis());
    }
}

final class Tag {
    protected final Map<String, Long> attractionCooldowns = new HashMap<>();
}
