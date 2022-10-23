package com.cavetale.halloween;

import com.cavetale.halloween.attraction.Attraction;
import com.cavetale.halloween.attraction.Festival;
import com.cavetale.halloween.util.Gui;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class HalloweenPlugin extends JavaPlugin {
    @Getter protected static HalloweenPlugin instance;
    protected final HalloweenCommand halloweenCommand = new HalloweenCommand(this);
    protected final HallowCommand hallowCommand = new HallowCommand(this);
    protected final EventListener eventListener = new EventListener(this);
    protected final Map<String, Festival> festivalMap = new HashMap<>();
    protected final Map<UUID, Session> sessionsMap = new HashMap<>();
    @Getter protected File attractionsFolder;
    @Getter protected File playersFolder;

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
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 0L, 0L);
        Gui.enable(this);
        loadFestivals();
    }

    @Override
    public void onDisable() {
        clearSessions();
        clearFestivals();
    }

    public void loadFestivals() {
        loadFestival(Booth2021.FESTIVAL);
        loadFestival(new Festival("twin_gourds", s -> new Booth2022()));

    }

    private void loadFestival(Festival festival) {
        festivalMap.put(festival.getWorldName(), festival);
        festival.load();
    }

    public void clearFestivals() {
        for (Festival festival : festivalMap.values()) {
            festival.clear();
        }
        festivalMap.clear();
    }

    public void clearSessions() {
        for (Session session : sessionsMap.values()) {
            session.save();
        }
        sessionsMap.clear();
    }

    private void tick() {
        for (Festival festival : festivalMap.values()) {
            festival.tick();
        }
    }

    protected void clearSession(UUID uuid) {
        Session session = sessionsMap.remove(uuid);
        if (session != null) session.save();
    }

    public Session sessionOf(Player player) {
        return sessionsMap.computeIfAbsent(player.getUniqueId(), u -> {
                Session newSession = new Session(this, player);
                newSession.load();
                return newSession;
            });
    }

    protected <T extends Attraction> void applyActiveAttraction(Class<T> type, Consumer<T> consumer) {
        for (Festival festival : festivalMap.values()) {
            festival.applyActiveAttraction(type, consumer);
        }
    }

    public static HalloweenPlugin plugin() {
        return instance;
    }

    public Attraction getAttraction(World world, String attractionName) {
        Festival festival = festivalMap.get(world.getName());
        if (festival == null) return null;
        return festival.getAttraction(attractionName);
    }

    public Attraction getAttraction(Location location) {
        Festival festival = festivalMap.get(location.getWorld().getName());
        if (festival == null) return null;
        return festival.getAttraction(location);
    }

    public List<Attraction> getAttractions(World world) {
        Festival festival = festivalMap.get(world.getName());
        if (festival == null) return List.of();
        return festival.getAttractions();
    }

    public List<Attraction> getAllAttractions() {
        List<Attraction> result = new ArrayList<>();
        for (Festival festival : festivalMap.values()) {
            result.addAll(festival.getAttractions());
        }
        return result;
    }

    public Festival getFestival(World world) {
        return festivalMap.get(world.getName());
    }
}
