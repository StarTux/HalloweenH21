package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.font.VanillaEffects;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import static com.cavetale.mytems.util.Collision.collidesWithBlock;
import static net.kyori.adventure.text.Component.text;

public final class FindGhostsAttraction extends Attraction<FindGhostsAttraction.SaveTag> {
    @Setter protected Duration searchTime = Duration.ofSeconds(180);
    protected final List<Cuboid> ghostAreas = new ArrayList<>();
    protected int secondsLeft;

    protected FindGhostsAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        for (Area area : allAreas) {
            if ("ghost".equals(area.name)) {
                ghostAreas.add(area.toCuboid());
            }
        }
        this.displayName = booth.format("Ghostbusters");
        this.description = text("Help! My house is haunted."
                                + " I can't even see them but they are scary."
                                + " Can you get rid of them all?");
    }

    @Override
    public boolean isPlaying() {
        return saveTag.state != State.IDLE;
    }

    @Override
    protected void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        saveTag.searchStarted = System.currentTimeMillis();
        spawnGhosts();
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
    public void onLoad() { }

    @Override
    public void onDisable() { }

    @Override
    public void onEntityDamage(EntityDamageEvent event) {
        if (saveTag.state != State.SEARCH) return;
        if (saveTag.ghostUuids.contains(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (saveTag.state != State.SEARCH) return;
        if (event.getEntity() instanceof LivingEntity ghost && saveTag.ghostUuids.contains(ghost.getUniqueId())) {
            event.setCancelled(true);
            Player player = getCurrentPlayer();
            if (player == null || !player.equals(event.getEntity())) return;
            findGhost(player, ghost);
        }
    }

    @Override
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (saveTag.state != State.SEARCH) return;
        if (event.getRightClicked() instanceof LivingEntity ghost && saveTag.ghostUuids.contains(ghost.getUniqueId())) {
            event.setCancelled(true);
            Player player = getCurrentPlayer();
            if (player == null || !player.equals(event.getPlayer())) return;
            findGhost(player, ghost);
        }
    }

    public void findGhost(Player player, LivingEntity ghost) {
        confetti(player, ghost.getEyeLocation());
        saveTag.ghostUuids.remove(ghost.getUniqueId());
        ghost.remove();
        if (saveTag.ghostUuids.isEmpty()) {
            victory(player);
            prepareReward(player, true);
            plugin.sessionOf(player).setCooldown(this, completionCooldown);
            changeState(State.IDLE);
        } else {
            int ghostCount = saveTag.totalGhosts - saveTag.ghostUuids.size();
            player.sendActionBar(booth.format("Ghost " + ghostCount + " discovered!"));
            progress(player);
        }
    }

    protected void spawnGhosts() {
        clearGhosts();
        Collections.shuffle(ghostAreas);
        for (Cuboid ghostArea : ghostAreas) {
            if (saveTag.ghostUuids.size() >= 7) break;
            List<Vec3i> ghostBlocks = new ArrayList<>(ghostArea.enumerate());
            ghostBlocks.removeIf(it -> {
                    BoundingBox bb = new BoundingBox((double) it.x, (double) it.y, (double) it.z,
                                                     (double) it.x + 1.0, (double) it.y + 2.0, (double) it.z + 1.0);
                    return collidesWithBlock(world, bb);
                });
            if (ghostBlocks.isEmpty()) continue;
            Vec3i ghostVector = ghostBlocks.get(random.nextInt(ghostBlocks.size()));
            Location location = ghostVector.toCenterFloorLocation(world);
            Villager ghost = world.spawn(ghostVector.toCenterFloorLocation(world), Villager.class, s -> {
                    s.setPersistent(false);
                    s.setRemoveWhenFarAway(false);
                    s.setSilent(true);
                    s.setInvisible(true);
                });
            ghost.getPathfinder().setCanOpenDoors(false);
            ghost.getPathfinder().setCanPassDoors(false);
            saveTag.ghostUuids.add(ghost.getUniqueId());
        }
        saveTag.totalGhosts = saveTag.ghostUuids.size();
    }

    protected void clearGhosts() {
        for (UUID uuid : saveTag.ghostUuids) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) entity.remove();
        }
        saveTag.ghostUuids.clear();
    }

    protected State tickSearch() {
        assert !saveTag.ghostUuids.isEmpty();
        Player player = getCurrentPlayer();
        if (player == null) return State.IDLE;
        long now = System.currentTimeMillis();
        long timeout = saveTag.searchStarted + searchTime.toMillis();
        if (now > timeout || saveTag.ghostUuids.isEmpty()) {
            timeout(player);
            return State.IDLE;
        }
        int seconds = (int) ((timeout - now - 1) / 1000L) + 1;
        if (seconds != secondsLeft) {
            secondsLeft = seconds;
            final UUID uuid = saveTag.ghostUuids.get(random.nextInt(saveTag.ghostUuids.size()));
            if (Bukkit.getEntity(uuid) instanceof LivingEntity ghost) {
                ghost.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 10, 0, true, false, false));
                world.playSound(ghost.getLocation(), Sound.ENTITY_PHANTOM_HURT, 1.5f, 0.5f);
            }
        }
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
            @Override protected State tick(FindGhostsAttraction instance) {
                return instance.tickSearch();
            }

            @Override protected void exit(FindGhostsAttraction instance) {
                instance.clearGhosts();
            }
        };

        protected void enter(FindGhostsAttraction instance) { }

        protected void exit(FindGhostsAttraction instance) { }

        protected State tick(FindGhostsAttraction instance) {
            return null;
        }
    }

    protected static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected List<UUID> ghostUuids = new ArrayList<>();
        protected int totalGhosts;
        protected long searchStarted;
    }

    @Override
    public void onPlayerHud(PlayerHudEvent event) {
        event.bossbar(PlayerHudPriority.HIGHEST,
                      makeProgressComponent(secondsLeft, VanillaEffects.GLOWING,
                                            saveTag.totalGhosts - saveTag.ghostUuids.size(), saveTag.totalGhosts),
                      BossBar.Color.RED, BossBar.Overlay.PROGRESS,
                      (float) secondsLeft / (float) searchTime.toSeconds());
    }
}
