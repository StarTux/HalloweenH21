package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Cuboid;
import com.cavetale.area.struct.Vec3i;
import com.cavetale.core.font.Unicode;
import com.cavetale.core.font.VanillaItems;
import com.cavetale.halloween.HalloweenPlugin;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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
    @Getter final Component displayName = Component.text("Shoot the Targets", NamedTextColor.DARK_RED);

    protected ShootTargetAttraction(final HalloweenPlugin plugin, final String name, final List<Cuboid> areaList) {
        super(plugin, name, areaList, SaveTag.class, SaveTag::new);
        List<Cuboid> list = new ArrayList<>();
        List<Cuboid> ghastList = new ArrayList<>();
        Set<Vec3i> set = new HashSet<>();
        Set<Vec3i> ghastSet = new HashSet<>();
        for (Cuboid cuboid : areaList) {
            if ("target".equals(cuboid.name)) {
                list.add(cuboid);
                set.addAll(cuboid.enumerate());
            } else if ("ghast".equals(cuboid.name)) {
                ghastList.add(cuboid);
                ghastSet.addAll(cuboid.enumerate());
            }
        }
        this.targetAreas = List.copyOf(list);
        this.targetBlocks = Set.copyOf(set);
        this.ghastAreas = List.copyOf(ghastList);
        this.ghastBlocks = Set.copyOf(ghastSet);
    }

    @Override
    protected void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        saveTag.score = 0;
        saveTag.missed = 0;
        saveTag.currentRound = 0;
        startingGun(player);
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

    protected void makeTargets() {
        List<Vec3i> possibleBlocks = new ArrayList<>(targetBlocks);
        Collections.shuffle(possibleBlocks, random);
        saveTag.targetBlocks.clear();
        for (int i = 0; i < Math.max(3, saveTag.currentRound); i += 1) {
            if (i >= possibleBlocks.size()) break;
            Vec3i targetBlock = possibleBlocks.get(i);
            saveTag.targetBlocks.add(targetBlock);
            targetBlock.toBlock(plugin.getWorld()).setType(Material.TARGET);
        }
        if (saveTag.currentRound >= 3 && !ghastBlocks.isEmpty()) {
            List<Vec3i> possibleGhasts = new ArrayList<>(ghastBlocks);
            Vec3i ghastBlock = possibleGhasts.get(random.nextInt(possibleGhasts.size()));
            saveTag.targetGhasts.add(ghastBlock);
        }
        spawnGhastEntities();
        saveTag.roundTargetCount = saveTag.targetBlocks.size() + saveTag.targetGhasts.size();
    }

    protected void clearTargets() {
        for (Vec3i targetBlock : saveTag.targetBlocks) {
            targetBlock.toBlock(plugin.getWorld()).setType(Material.AIR);
        }
        saveTag.targetBlocks.clear();
        clearGhastEntities();
        saveTag.targetGhasts.clear();
    }

    protected void spawnGhastEntities() {
        for (Vec3i ghastBlock : saveTag.targetGhasts) {
            Location location = ghastBlock.toLocation(plugin.getWorld());
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
                player.sendActionBar(Component.text(seconds, NamedTextColor.GOLD));
            }
            return null;
        }
        Title title = Title.title(Component.empty(),
                                  Component.text("Shoot!", NamedTextColor.GOLD, TextDecoration.ITALIC),
                                  Title.Times.of(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO));
        player.showTitle(title);
        player.sendMessage(Component.text("Shoot!", NamedTextColor.GOLD, TextDecoration.ITALIC));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, SoundCategory.MASTER, 1.0f, 2.0f);
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
                player.sendActionBar(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                            Component.text(Unicode.WATCH.string + seconds, NamedTextColor.GOLD),
                            Component.space(),
                            VanillaItems.TARGET.component,
                            Component.text(saveTag.roundScore + "/" + saveTag.roundTargetCount,
                                           NamedTextColor.DARK_RED),
                        }));
            }
            return null;
        }
        if (saveTag.roundScore == 0) {
            fail(player);
            return State.IDLE;
        }
        if (saveTag.currentRound >= MAX_ROUNDS - 1) {
            if (perfectRound) {
                perfect(player);
                prepareReward(player, true);
                plugin.sessionOf(player).setCooldown(this, Duration.ofSeconds(30));
            } else {
                victory(player);
                prepareReward(player, false);
                plugin.sessionOf(player).setCooldown(this, completionCooldown);
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
}
