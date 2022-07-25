package com.cavetale.halloween;

import com.cavetale.area.struct.Area;
import com.cavetale.area.struct.AreasFile;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.halloween.attraction.Attraction;
import com.cavetale.halloween.attraction.AttractionType;
import com.cavetale.halloween.util.Gui;
import com.cavetale.resident.PluginSpawn;
import com.cavetale.resident.ZoneType;
import com.cavetale.resident.save.Loc;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class HalloweenPlugin extends JavaPlugin {
    protected static final String TOTAL_COMPLETION = "TotalCompletion";
    @Getter protected static HalloweenPlugin instance;
    protected static final String WORLD = "halloween";
    protected static final String AREAS_FILE = "Halloween";
    protected final HalloweenCommand halloweenCommand = new HalloweenCommand(this);
    protected final HallowCommand hallowCommand = new HallowCommand(this);
    protected final EventListener eventListener = new EventListener(this);
    protected final Map<String, Attraction> attractionsMap = new HashMap<>();
    protected final Map<UUID, Session> sessionsMap = new HashMap<>();
    @Getter protected File attractionsFolder;
    @Getter protected File playersFolder;
    protected PluginSpawn totalCompletionVillager;

    @Override
    public void onEnable() {
        instance = this;
        halloweenCommand.enable();
        hallowCommand.enable();
        eventListener.enable();
        attractionsFolder = new File(getDataFolder(), "attractions");
        playersFolder = new File(getDataFolder(), "players");
        attractionsFolder.mkdirs();
        playersFolder.mkdirs();
        loadAttractions();
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 0L, 0L);
        Gui.enable(this);
    }

    @Override
    public void onDisable() {
        clearSessions();
        clearAttractions();
    }

    public World getWorld() {
        return Bukkit.getWorld(WORLD);
    }

    protected void tick() {
        for (Attraction attraction : new ArrayList<>(attractionsMap.values())) {
            if (attraction.isPlaying()) {
                try {
                    attraction.tick();
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, attraction.getName(), e);
                }
            }
        }
    }

    protected void clearAttractions() {
        for (Attraction attraction : attractionsMap.values()) {
            try {
                attraction.save();
                attraction.disable();
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
        attractionsMap.clear();
        if (totalCompletionVillager != null) {
            totalCompletionVillager.unregister();
            totalCompletionVillager = null;
        }
    }

    protected void clearSessions() {
        for (Session session : sessionsMap.values()) {
            session.save();
        }
        sessionsMap.clear();
    }

    protected void clearSession(UUID uuid) {
        Session session = sessionsMap.remove(uuid);
        if (session != null) session.save();
    }

    protected void loadAttractions() {
        World world = getWorld();
        if (world == null) throw new IllegalStateException("World not found: " + WORLD);
        AreasFile areasFile = AreasFile.load(world, AREAS_FILE);
        if (areasFile == null) throw new IllegalStateException("Areas file not found: " + AREAS_FILE);
        Set<Booth> unusedBooths = EnumSet.allOf(Booth.class);
        for (Map.Entry<String, List<Area>> entry : areasFile.areas.entrySet()) {
            String name = entry.getKey();
            if (name.equals(TOTAL_COMPLETION)) {
                Location location = entry.getValue().get(0).min.toLocation(world);
                this.totalCompletionVillager = PluginSpawn.register(this, ZoneType.HALLOWEEN, Loc.of(location));
                this.totalCompletionVillager.setOnPlayerClick(this::clickTotalCompletionVillager);
                this.totalCompletionVillager.setOnMobSpawning(mob -> mob.setCollidable(false));
                continue;
            }
            Booth booth = Booth.forName(name);
            if (booth == null) {
                getLogger().warning(name + ": No Booth found!");
            } else {
                unusedBooths.remove(booth);
            }
            List<Area> areaList = entry.getValue();
            Attraction attraction = Attraction.of(this, name, areaList, booth);
            if (attraction == null) {
                getLogger().warning(name + ": No Attraction!");
            }
            try {
                attraction.enable();
                attraction.load();
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            attractionsMap.put(name, attraction);
        }
        for (Booth booth : unusedBooths) {
            getLogger().warning(booth + ": Booth unused");
        }
        Map<AttractionType, Integer> counts = new EnumMap<>(AttractionType.class);
        for (AttractionType type : AttractionType.values()) counts.put(type, 0);
        for (Attraction attraction : attractionsMap.values()) {
            AttractionType type = AttractionType.of(attraction);
            counts.put(type, counts.get(type) + 1);
        }
        List<AttractionType> rankings = new ArrayList<>(List.of(AttractionType.values()));
        Collections.sort(rankings, (a, b) -> Integer.compare(counts.get(a), counts.get(b)));
        for (AttractionType type : rankings) {
            getLogger().info(counts.get(type) + " " + type);
        }
        getLogger().info(attractionsMap.size() + " Total");
    }

    public List<Player> getPlayersIn(Cuboid area) {
        List<Player> result = new ArrayList<>();
        for (Player player : getWorld().getPlayers()) {
            if (area.contains(player.getLocation())) {
                result.add(player);
            }
        }
        return result;
    }

    public Session sessionOf(Player player) {
        return sessionsMap.computeIfAbsent(player.getUniqueId(), u -> {
                Session newSession = new Session(this, player);
                newSession.load();
                return newSession;
            });
    }

    protected <T extends Attraction> void applyActiveAttraction(Class<T> type, Consumer<T> consumer) {
        for (Attraction attraction : attractionsMap.values()) {
            if (attraction.isPlaying() && type.isInstance(attraction)) {
                consumer.accept(type.cast(attraction));
            }
        }
    }

    protected void clickTotalCompletionVillager(Player player) {
        Session session = sessionOf(player);
        if (session.isUniqueNameLocked(TOTAL_COMPLETION)) {
            player.sendMessage(Component.text("You completed everything. Congratulations!",
                                              NamedTextColor.GOLD));
            return;
        }
        Booth[] booths = Booth.values();
        final int total = booths.length;
        int locked = 0;
        for (Booth booth : booths) {
            if (session.isUniqueNameLocked(booth.name)) {
                locked += 1;
            }
        }
        if (locked >= total) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kite member HalloweenComplete " + player.getName());
            session.lockUniqueName(TOTAL_COMPLETION);
            session.save();
            Attraction.perfect(player, true);
        } else {
            player.sendMessage(Component.text("You completed " + locked + "/" + total + " games."
                                              + " Please return when you completed everything!"
                                              + " Use your Magic Map to locate more games.",
                                              NamedTextColor.GOLD));
        }
    }
}
