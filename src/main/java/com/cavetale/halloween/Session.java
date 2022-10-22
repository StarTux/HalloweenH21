package com.cavetale.halloween;

import com.cavetale.core.util.Json;
import com.cavetale.halloween.attraction.Attraction;
import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
        this.saveFile = new File(plugin.getPlayersFolder(), uuid + ".json");
    }

    protected void load() {
        this.tag = Json.load(saveFile, Tag.class, Tag::new);
    }

    public void save() {
        Json.save(saveFile, tag, true);
    }

    public Duration getCooldown(Attraction attraction) {
        Long cd = tag.cooldowns.get(attraction.getUniqueKey());
        if (cd == null) return null;
        long now = System.currentTimeMillis();
        if (now > cd) {
            tag.cooldowns.remove(attraction.getUniqueKey());
            return null;
        }
        return Duration.ofMillis(cd - now);
    }

    public void setCooldown(Attraction attraction, Duration duration) {
        tag.cooldowns.put(attraction.getUniqueKey(), duration.toMillis() + System.currentTimeMillis());
    }

    public boolean isUniqueLocked(Attraction attraction) {
        return tag.uniquesGot.contains(attraction.getUniqueKey());
    }

    public void lockUnique(Attraction attraction) {
        tag.uniquesGot.add(attraction.getUniqueKey());
    }

    public boolean isTotallyCompleted() {
        return tag.totallyCompleted;
    }

    public void lockTotallyCompleted() {
        tag.totallyCompleted = true;
    }

    public int getPrizeWaiting(Attraction attraction) {
        Integer result = tag.prizesWaiting.get(attraction.getUniqueKey());
        return result != null ? result : 0;
    }

    public void setFirstCompletionPrizeWaiting(Attraction attraction) {
        tag.prizesWaiting.put(attraction.getUniqueKey(), 2);
    }

    public void setRegularCompletionPrizeWaiting(Attraction attraction) {
        tag.prizesWaiting.put(attraction.getUniqueKey(), 1);
    }

    public void clearPrizeWaiting(Attraction attraction) {
        tag.prizesWaiting.remove(attraction.getUniqueKey());
    }

    static final class Tag {
        protected boolean totallyCompleted;
        protected final Map<String, Long> cooldowns = new HashMap<>();
        protected final Set<String> uniquesGot = new HashSet<>();
        protected final Map<String, Integer> prizesWaiting = new HashMap<>(); // 1 = regular, 2 = unique
    }
}
