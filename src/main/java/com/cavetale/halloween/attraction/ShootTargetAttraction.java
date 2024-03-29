package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.font.VanillaItems;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.halloween.Session;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

public final class ShootTargetAttraction extends Attraction<ShootTargetAttraction.SaveTag> {
    protected static final Duration SHOOT_TIME = Duration.ofSeconds(30);
    protected static final Duration WARMUP_TIME = Duration.ofSeconds(3);
    protected static final int MAX_ROUNDS = 10;
    private final List<Cuboid> targetAreas;
    private final List<Cuboid> ghastAreas;
    private final Set<Vec3i> targetBlocks;
    private final Set<Vec3i> ghastBlocks;
    protected long secondsLeft; // cached
    /**
     * One ghast is spawned for each SaveTag.targetGhasts and stored here.
     * Ghast lifetime is from spawnGhastEntities() to clearGhastEntities().
     */
    private final Map<UUID, Vec3i> targetGhastMap = new HashMap<>();

    protected ShootTargetAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        List<Cuboid> list = new ArrayList<>();
        List<Cuboid> ghastList = new ArrayList<>();
        Set<Vec3i> set = new HashSet<>();
        Set<Vec3i> ghastSet = new HashSet<>();
        for (Area area : allAreas) {
            if ("target".equals(area.name)) {
                list.add(area.toCuboid());
                set.addAll(area.enumerate());
            } else if ("ghast".equals(area.name)) {
                ghastList.add(area.toCuboid());
                ghastSet.addAll(area.enumerate());
            }
        }
        this.targetAreas = List.copyOf(list);
        this.targetBlocks = Set.copyOf(set);
        this.ghastAreas = List.copyOf(ghastList);
        this.ghastBlocks = Set.copyOf(ghastSet);
        this.displayName = Component.text("Bull's Eye!", NamedTextColor.DARK_RED);
        this.description = Component.text("Shoot all the target blocks and ghasts as they appear!");
    }

    @Override
    protected void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        saveTag.score = 0;
        saveTag.missed = 0;
        saveTag.currentRound = 0;
        changeState(State.WARMUP);
    }

    @Override
    protected void stop() {
        changeState(State.IDLE);
    }

    @Override
    public boolean isPlaying() {
        return saveTag.state != State.IDLE;
    }

    @Override
    protected void onTick() {
        State newState = saveTag.state.tick(this);
        if (newState != null) {
            changeState(newState);
        }
    }

    @Override
    protected void onLoad() {
        if (saveTag.state == State.SHOOT) {
            spawnGhastEntities();
        }
    }

    @Override
    public void onEntityDamage(EntityDamageEvent event) {
        if (targetGhastMap.containsKey(event.getEntity().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    protected void onDisable() {
        clearGhastEntities();
    }

    protected void changeState(State newState) {
        State oldState = saveTag.state;
        saveTag.state = newState;
        oldState.exit(this);
        newState.enter(this);
    }

    /**
     * Called by EventListener.
     */
    public void onProjectileHit(ProjectileHitEvent event) {
        if (saveTag.state != State.SHOOT) return;
        Projectile projectile = event.getEntity();
        switch (projectile.getType()) {
        case ARROW: case SPECTRAL_ARROW: break;
        default: return;
        }
        if (!(projectile.getShooter() instanceof Player)) return;
        Player player = (Player) projectile.getShooter();
        if (!player.getUniqueId().equals(saveTag.currentPlayer)) return;
        Block hitBlock = event.getHitBlock();
        if (hitBlock != null) {
            Vec3i targetBlock = Vec3i.of(hitBlock);
            if (!saveTag.targetBlocks.remove(targetBlock)) return;
            hitBlock.setType(Material.AIR);
            confetti(player, projectile.getLocation());
            projectile.remove();
            saveTag.score += 1;
            saveTag.roundScore += 1;
            progress(player);
            player.sendActionBar(makeProgressComponent((int) secondsLeft, VanillaItems.TARGET.component,
                                                       saveTag.roundScore, saveTag.roundTargetCount));
            return;
        }
        Entity hitEntity = event.getHitEntity();
        if (hitEntity != null) {
            Vec3i targetGhast = targetGhastMap.remove(hitEntity.getUniqueId());
            if (targetGhast == null) return;
            hitEntity.remove();
            confetti(player, projectile.getLocation());
            projectile.remove();
            if (!saveTag.targetGhasts.remove(targetGhast)) {
                throw new IllegalStateException(name + ": no target ghast: " + targetGhast);
            }
            saveTag.score += 1;
            saveTag.roundScore += 1;
            progress(player);
            return;
        }
    }

    private static int maxDistance(Vec3i a, Vec3i b) {
        return Math.max(Math.abs(a.x - b.x),
                        Math.max(Math.abs(a.y - b.y),
                                 Math.abs(a.z - b.z)));
    }

    protected void makeTargets() {
        List<Vec3i> possibleBlocks = new ArrayList<>(targetBlocks);
        possibleBlocks.removeIf(v -> !v.toBlock(world).isEmpty());
        Collections.shuffle(possibleBlocks, random);
        saveTag.targetBlocks.clear();
        for (int i = 0; i < Math.max(3, saveTag.currentRound); i += 1) {
            if (i >= possibleBlocks.size()) break;
            Vec3i targetBlock = possibleBlocks.get(i);
            saveTag.targetBlocks.add(targetBlock);
            targetBlock.toBlock(world).setType(Material.TARGET);
        }
        if (saveTag.currentRound >= 2 && !ghastBlocks.isEmpty()) {
            int ghastCount = Math.max(1, (saveTag.currentRound - 1) / 2);
            for (int i = 0; i < ghastCount; i += 1) {
                List<Vec3i> possibleGhasts = new ArrayList<>(ghastBlocks);
                for (Vec3i old : saveTag.targetGhasts) {
                    possibleGhasts.removeIf(b -> maxDistance(b, old) < 4);
                }
                if (possibleGhasts.isEmpty()) break;
                Vec3i ghastBlock = possibleGhasts.get(random.nextInt(possibleGhasts.size()));
                saveTag.targetGhasts.add(ghastBlock);
            }
        }
        spawnGhastEntities();
        saveTag.roundTargetCount = saveTag.targetBlocks.size() + saveTag.targetGhasts.size();
    }

    protected void clearTargets() {
        for (Vec3i targetBlock : saveTag.targetBlocks) {
            targetBlock.toBlock(world).setType(Material.AIR);
        }
        saveTag.targetBlocks.clear();
        clearGhastEntities();
        saveTag.targetGhasts.clear();
    }

    protected void spawnGhastEntities() {
        for (Vec3i ghastBlock : saveTag.targetGhasts) {
            Location location = ghastBlock.toCenterFloorLocation(world);
            location.setYaw(random.nextFloat() * 360.0f);
            Ghast ghast = location.getWorld().spawn(location, Ghast.class, g -> {
                    g.setPersistent(false);
                    g.setRemoveWhenFarAway(false);
                    Bukkit.getMobGoals().removeAllGoals(g);
                });
            if (ghast == null || ghast.isDead()) {
                throw new IllegalStateException(name + ": spawning ghast at " + ghastBlock);
            }
            targetGhastMap.put(ghast.getUniqueId(), ghastBlock);
        }
    }

    protected void clearGhastEntities() {
        for (UUID uuid : targetGhastMap.keySet()) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) entity.remove();
        }
        targetGhastMap.clear();
    }

    protected State tickWarmup() {
        Player player = getCurrentPlayer();
        if (player == null) return State.IDLE;
        long now = System.currentTimeMillis();
        if (now < saveTag.warmupStarted + WARMUP_TIME.toMillis()) {
            long seconds = (WARMUP_TIME.toMillis() - (now - saveTag.warmupStarted) - 1L) / 1000L + 1L;
            if (secondsLeft != seconds) {
                secondsLeft = seconds;
                countdown(player, (int) seconds);
            }
            return null;
        }
        Title title = Title.title(Component.empty(),
                                  Component.text("Shoot!", NamedTextColor.GOLD, TextDecoration.ITALIC),
                                  Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO));
        player.showTitle(title);
        player.sendMessage(Component.text("Shoot!", NamedTextColor.GOLD, TextDecoration.ITALIC));
        startingGun(player);
        return State.SHOOT;
    }

    protected State tickShoot() {
        Player player = getCurrentPlayer();
        if (player == null || !isInArea(player.getLocation())) return State.IDLE;
        final long now = System.currentTimeMillis();
        final int missed = saveTag.targetBlocks.size() + saveTag.targetGhasts.size();
        final boolean perfectRound = missed == 0;
        final boolean shouldEnd = perfectRound || now >= saveTag.shootingStarted + SHOOT_TIME.toMillis();
        if (!shouldEnd) {
            long seconds = (SHOOT_TIME.toMillis() - (now - saveTag.shootingStarted) - 1L) / 1000L + 1L;
            if (secondsLeft != seconds) {
                secondsLeft = seconds;
                for (Vec3i vec : saveTag.targetBlocks) {
                    highlight(player, vec.toCenterLocation(player.getWorld()));
                }
            }
            for (UUID uuid : targetGhastMap.keySet()) {
                Entity e = Bukkit.getEntity(uuid);
                if (e == null) continue;
                Location loc = e.getLocation();
                loc.setYaw(loc.getYaw() + 18.0f);
                e.teleport(loc);
            }
            return null;
        }
        if (saveTag.roundScore == 0) {
            fail(player);
            return State.IDLE;
        }
        if (saveTag.currentRound >= MAX_ROUNDS - 1) {
            Session session = plugin.sessionOf(player);
            if (perfectRound) {
                perfect(player);
                prepareReward(player, true);
                session.setCooldown(this, completionCooldown);
            } else {
                victory(player);
                prepareReward(player, false);
                session.setCooldown(this, session.isUniqueLocked(this)
                                    ? completionCooldown
                                    : Duration.ofSeconds(30));
            }
            return State.IDLE;
        }
        // Game's not over yet!
        progress(player);
        if (perfectRound) {
            subtitle(player, Component.text("Perfect Round!", NamedTextColor.GOLD));
            player.sendMessage(Component.text("Perfect Round!", NamedTextColor.GOLD));
        } else {
            if (missed == 1) {
                subtitle(player, Component.text("You missed one target", NamedTextColor.GOLD));
                player.sendMessage(Component.text("You missed one target", NamedTextColor.GOLD));
            } else {
                subtitle(player, Component.text("You missed " + missed + " targets", NamedTextColor.GOLD));
                player.sendMessage(Component.text("You missed " + missed + " targets", NamedTextColor.GOLD));
            }
        }
        saveTag.currentRound += 1;
        return State.WARMUP;
    }

    enum State {
        IDLE,
        WARMUP {
            @Override protected void enter(ShootTargetAttraction instance) {
                instance.saveTag.warmupStarted = System.currentTimeMillis();
                instance.secondsLeft = 0;
            }

            @Override protected State tick(ShootTargetAttraction instance) {
                return instance.tickWarmup();
            }
        },
        SHOOT {
            @Override protected void enter(ShootTargetAttraction instance) {
                instance.secondsLeft = 0L;
                instance.makeTargets();
                instance.saveTag.shootingStarted = System.currentTimeMillis();
                instance.saveTag.roundScore = 0;
            }

            @Override protected void exit(ShootTargetAttraction instance) {
                instance.saveTag.missed += instance.saveTag.targetBlocks.size()
                    + instance.saveTag.targetGhasts.size();
                instance.clearTargets();
            }

            @Override protected State tick(ShootTargetAttraction instance) {
                return instance.tickShoot();
            }
        };

        protected void enter(ShootTargetAttraction instance) { }

        protected void exit(ShootTargetAttraction instance) { }

        protected State tick(ShootTargetAttraction instance) {
            return null;
        }
    }

    static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected List<Vec3i> targetBlocks = new ArrayList<>();
        protected List<Vec3i> targetGhasts = new ArrayList<>();
        protected long warmupStarted;
        protected long shootingStarted;
        protected int score;
        protected int missed;
        protected int currentRound;
        protected int roundScore;
        protected int roundTargetCount;
    }


    @Override
    public void onPlayerHud(PlayerHudEvent event) {
        event.bossbar(PlayerHudPriority.HIGHEST,
                      makeProgressComponent((int) secondsLeft, VanillaItems.TARGET.component, saveTag.roundScore, saveTag.roundTargetCount),
                      BossBar.Color.RED, BossBar.Overlay.PROGRESS,
                      (float) secondsLeft / (float) SHOOT_TIME.toSeconds());
    }
}
