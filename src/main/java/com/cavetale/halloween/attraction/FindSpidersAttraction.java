package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Cuboid;
import com.cavetale.area.struct.Vec3i;
import com.cavetale.core.font.Unicode;
import com.cavetale.core.font.VanillaItems;
import com.cavetale.halloween.Booth;
import com.cavetale.halloween.HalloweenPlugin;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class FindSpidersAttraction extends Attraction<FindSpidersAttraction.SaveTag> {
    protected static final Duration SEARCH_TIME = Duration.ofSeconds(40);
    protected static final int MAX_SPIDERS = 10;
    protected final Set<Vec3i> possibleSpiderBlocks;
    protected CaveSpider currentSpider;
    protected int secondsLeft;

    protected FindSpidersAttraction(final HalloweenPlugin plugin, final String name, final List<Cuboid> areaList, final Booth booth) {
        super(plugin, name, areaList, booth, SaveTag.class, SaveTag::new);
        Set<Vec3i> spiderSet = new HashSet<>();
        for (Cuboid cuboid : areaList) {
            if ("spider".equals(cuboid.name)) {
                spiderSet.addAll(cuboid.enumerate());
            }
        }
        this.possibleSpiderBlocks = Set.copyOf(spiderSet);
        this.displayName = Component.text("Spider Hunt", NamedTextColor.DARK_RED);
        this.description = Component.text("My place is lousy with spiders."
                                          + " They come out one by one and I can't sleep because they are noisy!"
                                          + " Please find them all.");
    }

    @Override
    public boolean isPlaying() {
        return saveTag.state != State.IDLE;
    }

    @Override
    protected void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        makeSpiderBlocks();
        startingGun(player);
        changeState(State.SEARCH);
    }

    @Override
    protected void stop() {
        changeState(State.IDLE);
    }

    @Override
    public void onTick() {
        State newState = saveTag.state.tick(this);
        if (newState != null) changeState(newState);
    }

    @Override
    public void onLoad() {
        if (saveTag.state == State.SEARCH) {
            spawnSpider();
        }
    }

    @Override
    public void onDisable() {
        clearSpider();
    }

    @Override
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity().equals(currentSpider)) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (saveTag.state != State.SEARCH) return;
        Player player = getCurrentPlayer();
        if (player == null) return;
        if (event.getEntity().equals(currentSpider) && event.getDamager().equals(player)) {
            spiderFound(player);
        }
    }

    @Override
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (saveTag.state != State.SEARCH) return;
        Player player = getCurrentPlayer();
        if (player == null) return;
        if (event.getRightClicked().equals(currentSpider) && event.getPlayer().equals(player)) {
            spiderFound(player);
        }
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (saveTag.state != State.SEARCH) return;
        switch (event.getAction()) {
        case RIGHT_CLICK_BLOCK:
        case LEFT_CLICK_BLOCK:
            break;
        default: return;
        }
        Player player = getCurrentPlayer();
        if (player == null) return;
        Vec3i spiderBlock = saveTag.spiderBlocks.get(saveTag.spiderBlockIndex);
        if (event.getPlayer().equals(player) && Vec3i.of(event.getClickedBlock()).equals(spiderBlock)) {
            spiderFound(player);
        }
    }

    public void spiderFound(Player player) {
        confetti(player, currentSpider.getLocation().add(0, currentSpider.getHeight() * 0.5, 0));
        clearSpider();
        saveTag.spiderBlockIndex += 1;
        if (saveTag.spiderBlockIndex >= saveTag.spiderBlocks.size()) {
            victory(player);
            prepareReward(player, true);
            plugin.sessionOf(player).setCooldown(this, completionCooldown);
            changeState(State.IDLE);
        } else {
            progress(player);
            changeState(State.SEARCH);
        }
    }

    protected void makeSpiderBlocks() {
        List<Vec3i> spiderBlocks = new ArrayList<>(possibleSpiderBlocks);
        World w = plugin.getWorld();
        spiderBlocks.removeIf(it -> {
                Block block = it.toBlock(w);
                if (block.isEmpty()) return false;
                Material mat = block.getType();
                if (Tag.CARPETS.isTagged(mat)) return false;
                if (block.getType() == Material.COBWEB) return false;
                return true;
            });
        Collections.shuffle(spiderBlocks);
        saveTag.spiderBlocks = new ArrayList<>(spiderBlocks.subList(0, Math.min(MAX_SPIDERS, spiderBlocks.size())));
        saveTag.spiderBlockIndex = 0;
    }

    protected void spawnSpider() {
        Vec3i vector = saveTag.spiderBlocks.get(saveTag.spiderBlockIndex);
        World w = plugin.getWorld();
        currentSpider = plugin.getWorld().spawn(vector.toLocation(w), CaveSpider.class, s -> {
                s.setPersistent(false);
                s.setRemoveWhenFarAway(false);
                s.setGravity(false);
                s.setSilent(true);
                Bukkit.getMobGoals().removeAllGoals(s);
            });
    }

    protected void clearSpider() {
        if (currentSpider != null) {
            currentSpider.remove();
            currentSpider = null;
        }
    }

    protected State tickSearch() {
        Player player = getCurrentPlayer();
        if (player == null || currentSpider == null) return State.IDLE;
        long now = System.currentTimeMillis();
        long timeout = saveTag.searchStarted + SEARCH_TIME.toMillis();
        if (now > timeout) {
            timeout(player);
            return State.IDLE;
        }
        int seconds = (int) ((timeout - now - 1) / 1000L) + 1;
        if (seconds != secondsLeft) {
            secondsLeft = seconds;
            String progress = (saveTag.spiderBlockIndex + 1) + "/" + saveTag.spiderBlocks.size();
            player.sendActionBar(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                        Component.text(Unicode.WATCH.string + seconds, NamedTextColor.GOLD),
                        Component.space(),
                        VanillaItems.SPIDER_EYE.component,
                        Component.text(progress, NamedTextColor.DARK_RED),
                    }));
            currentSpider.getWorld().playSound(currentSpider.getLocation(), Sound.ENTITY_SPIDER_HURT, SoundCategory.HOSTILE, 1.0f, 0.5f);
        }
        Location location = currentSpider.getLocation();
        location.setYaw(location.getYaw() + 18.0f);
        currentSpider.teleport(location);
        return null;
    }

    protected void changeState(State newState) {
        State oldState = saveTag.state;
        saveTag.state = newState;
        oldState.exit(this);
        newState.enter(this);
    }

    enum State {
        IDLE,
        SEARCH {
            @Override protected State tick(FindSpidersAttraction instance) {
                return instance.tickSearch();
            }

            @Override protected void enter(FindSpidersAttraction instance) {
                instance.saveTag.searchStarted = System.currentTimeMillis();
                instance.spawnSpider();
            }

            @Override protected void exit(FindSpidersAttraction instance) {
                instance.clearSpider();
            }
        };

        protected void enter(FindSpidersAttraction instance) { }

        protected void exit(FindSpidersAttraction instance) { }

        protected State tick(FindSpidersAttraction instance) {
            return null;
        }
    }

    protected static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected List<Vec3i> spiderBlocks;
        protected int spiderBlockIndex;
        protected long searchStarted;
    }
}
