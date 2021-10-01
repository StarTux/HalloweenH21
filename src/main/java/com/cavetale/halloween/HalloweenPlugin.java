package com.cavetale.halloween;

import com.cavetale.area.struct.AreasFile;
import com.cavetale.area.struct.Cuboid;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class HalloweenPlugin extends JavaPlugin {
    protected static final String WORLD = "halloween";
    protected static final String AREAS_FILE = "Halloween";
    HalloweenCommand halloweenCommand = new HalloweenCommand(this);
    EventListener eventListener = new EventListener(this);
    protected final Map<String, Attraction> attractionsMap = new HashMap<>();
    protected final Map<UUID, Session> sessionsMap = new HashMap<>();
    protected File attractionsFolder;
    protected File playersFolder;

    @Override
    public void onEnable() {
        halloweenCommand.enable();
        eventListener.enable();
        attractionsFolder = new File(getDataFolder(), "attractions");
        playersFolder = new File(getDataFolder(), "players");
        attractionsFolder.mkdirs();
        playersFolder.mkdirs();
        loadAttractions();
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 0L, 0L);
    }

    @Override
    public void onDisable() {
    }

    protected World getWorld() {
        return Bukkit.getWorld(WORLD);
    }

    protected void tick() {
        for (Attraction attraction : new ArrayList<>(attractionsMap.values())) {
            try {
                attraction.tick();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, attraction.getName(), e);
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
    }

    protected void clearSessions() {
        for (Session session : sessionsMap.values()) {
            session.save();
        }
        sessionsMap.clear();
    }

    protected void clearSession(UUID uuid) {
        Session session = sessionsMap.remove(uuid);
        if (session == null) return;
        session.save();
    }

    protected void loadAttractions() {
        World world = getWorld();
        if (world == null) throw new IllegalStateException("World not found: " + WORLD);
        AreasFile areasFile = AreasFile.load(world, AREAS_FILE);
        if (areasFile == null) throw new IllegalStateException("Areas file not found: " + AREAS_FILE);
        for (Map.Entry<String, List<Cuboid>> entry : areasFile.areas.entrySet()) {
            String name = entry.getKey();
            List<Cuboid> areaList = entry.getValue();
            Attraction attraction = Attraction.of(this, name, areaList);
            try {
                attraction.enable();
                attraction.load();
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            attractionsMap.put(name, attraction);
        }
    }

    protected List<Player> getPlayersIn(Cuboid area) {
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
}
