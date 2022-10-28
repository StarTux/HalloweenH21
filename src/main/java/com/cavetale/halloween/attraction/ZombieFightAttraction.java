package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.mytems.Mytems;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

public final class ZombieFightAttraction extends Attraction<ZombieFightAttraction.SaveTag> {
    protected final Duration playTime = Duration.ofSeconds(60);
    protected final Duration warmupTime = Duration.ofSeconds(5);
    protected final List<Vec3i> zombieBlocks = new ArrayList<>();
    @Setter private int totalRounds = 5;
    protected int secondsLeft;

    protected ZombieFightAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        Set<Vec3i> zombieBlockSet = new HashSet<>();
        Set<Vec3i> noZombieBlockSet = new HashSet<>();
        for (Area area : allAreas) {
            if ("snowman".equals(area.name)) {
                zombieBlockSet.addAll(area.enumerate());
            } else if ("nosnowman".equals(area.name)) {
                noZombieBlockSet.addAll(area.enumerate());
            }
        }
        zombieBlocks.addAll(zombieBlockSet);
        zombieBlocks.removeAll(noZombieBlockSet);
        this.displayName = booth.format("Zombie Defense");
        this.description = text("Zombies are rising from the graves! Please show them the meaning of trick or treat and hit them all with an egg in the face!");
    }

    @Override
    public void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        saveTag.round = 0;
        changeState(State.WARMUP);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING,
                         SoundCategory.MASTER, 1.0f, 1.0f);
    }

    @Override
    public void stop() {
        changeState(State.IDLE);
    }

    protected void spawnZombies() {
        zombieBlocks.removeIf(v -> {
                Block block = v.toBlock(world);
                if (!block.getRelative(0, 1, 0).isEmpty()) return true;
                if (block.isEmpty()) return false;
                Material mat = block.getType();
                if (Tag.WOOL_CARPETS.isTagged(mat)) return false;
                if (Tag.CROPS.isTagged(mat)) return false;
                switch (mat) {
                case COBWEB:
                case GRASS:
                case PUMPKIN_STEM:
                case MELON_STEM:
                case CARROTS:
                case POTATOES:
                case WHEAT:
                case BEETROOTS:
                case SNOW:
                    return false;
                default: return true;
                }
            });
        Collections.shuffle(zombieBlocks);
        saveTag.zombies = new ArrayList<>();
        saveTag.round += 1;
        saveTag.zombiesHit = 0;
        saveTag.zombieCount = (saveTag.round - 1) * 2 + 10;
        for (int i = 0; i < saveTag.zombieCount; i += 1) {
            Vec3i vec = zombieBlocks.get(i);
            Location location = vec.toCenterFloorLocation(world);
            location.setYaw(random.nextFloat() * 360.0f);
            Zombie zombie = location.getWorld().spawn(location, Zombie.class, s -> {
                    s.setPersistent(false);
                    s.setRemoveWhenFarAway(false);
                    s.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.0);
                    s.setGlowing(true);
                    Bukkit.getMobGoals().removeAllGoals(s);
                });
            if (zombie != null) {
                saveTag.zombies.add(zombie.getUniqueId());
            }
        }
    }

    protected void clearZombies() {
        if (saveTag.zombies == null) return;
        for (UUID uuid : saveTag.zombies) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) entity.remove();
        }
        saveTag.zombies = null;
    }

    @Override
    public boolean isPlaying() {
        return saveTag.state != State.IDLE;
    }

    @Override
    public void onTick() {
        if (saveTag.state == State.IDLE) return;
        Player player = getCurrentPlayer();
        if (player == null) return;
        State newState = saveTag.state.tick(this, player);
        if (newState != null) changeState(newState);
    }

    protected State tickWarmup(Player player) {
        long playedTime = System.currentTimeMillis() - saveTag.warmupStarted;
        long timeLeft = warmupTime.toMillis() - playedTime;
        if (timeLeft < 0) {
            player.getInventory().addItem(new ItemStack(Material.EGG, 16));
            startingGun(player);
            player.showTitle(Title.title(empty(),
                                         booth.format("Round " + saveTag.round + "/" + totalRounds),
                                         Title.Times.times(Duration.ZERO,
                                                           Duration.ofSeconds(1),
                                                           Duration.ZERO)));
            return State.PLAY;
        }
        secondsLeft = (int) ((timeLeft - 1) / 1000L) + 1;
        return null;
    }

    protected State tickPlay(Player player) {
        long playedTime = System.currentTimeMillis() - saveTag.playStarted;
        long timeLeft = playTime.toMillis() - playedTime;
        if (timeLeft < 0) {
            timeout(player);
            return State.IDLE;
        }
        secondsLeft = (int) ((timeLeft - 1) / 1000L) + 1;
        // Egg every X ticks
        if (saveTag.shootCooldown > 0) {
            saveTag.shootCooldown -= 1;
        } else {
            saveTag.shootCooldown = 60;
            for (UUID uuid : saveTag.zombies) {
                Entity entity = Bukkit.getEntity(uuid);
                if (!(entity instanceof Zombie zombie)) continue;
                if (player.getLocation().getY() >= zombie.getLocation().getY() + 1.5 && zombie.hasLineOfSight(player)) {
                    Vector direction = player.getEyeLocation().toVector()
                        .subtract(zombie.getEyeLocation().toVector())
                        .multiply(1.5);
                    zombie.launchProjectile(Egg.class, direction);
                } else {
                    final int total = 4;
                    for (int i = 0; i < total; i += 1) {
                        double frac = (double) i / (double) total;
                        double pi = frac * 2.0 * Math.PI + random.nextDouble() * Math.PI;
                        final double str = 1.5;
                        Vector direction = new Vector(Math.cos(pi) * str,
                                                      0.0,
                                                      Math.sin(pi) * str);
                        zombie.launchProjectile(Egg.class, direction);
                    }
                }
            }
            player.playSound(player.getLocation(), Sound.ENTITY_SNOW_GOLEM_SHOOT,
                             SoundCategory.MASTER, 1.0f, 1.5f);
            player.showTitle(Title.title(empty(),
                                         booth.format("pew"),
                                         Title.Times.times(Duration.ZERO,
                                                           Duration.ofMillis(500L),
                                                           Duration.ZERO)));
        }
        return null;
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (saveTag.state != State.PLAY) return;
        if (!(event.getDamager() instanceof Egg egg)) return;
        if (event.getEntity() instanceof Player target) {
            if (!target.equals(getCurrentPlayer())) return;
            if (!(egg.getShooter() instanceof Zombie)) return;
            target.sendActionBar(booth.format("You were hit by a egg!"));
            fail(target);
            stop();
        } else if (event.getEntity() instanceof Zombie target) {
            event.setCancelled(true);
            if (!(egg.getShooter() instanceof Player shooter)) return;
            if (!shooter.equals(getCurrentPlayer())) return;
            if (!saveTag.zombies.remove(target.getUniqueId())) return;
            confetti(shooter, target.getEyeLocation());
            target.remove();
            progress(shooter);
            saveTag.zombiesHit += 1;
            if (saveTag.zombies.isEmpty()) {
                if (saveTag.round >= totalRounds) {
                    perfect(shooter, true);
                    prepareReward(shooter, true);
                    changeState(State.IDLE);
                } else {
                    changeState(State.WARMUP);
                    shooter.showTitle(Title.title(empty(),
                                                  booth.format("Round complete!"),
                                                  Title.Times.times(Duration.ZERO,
                                                                    Duration.ofSeconds(1),
                                                                    Duration.ZERO)));
                }
            }
        }
    }

    protected void changeState(State newState) {
        State oldState = saveTag.state;
        saveTag.state = newState;
        oldState.exit(this);
        newState.enter(this);
    }

    enum State {
        IDLE {
            @Override protected void enter(ZombieFightAttraction instance) {
                instance.saveTag.currentPlayer = null;
            }
        },
        WARMUP {
            @Override protected void enter(ZombieFightAttraction instance) {
                instance.saveTag.warmupStarted = System.currentTimeMillis();
            }

            @Override protected State tick(ZombieFightAttraction instance, Player player) {
                return instance.tickWarmup(player);
            }
        },
        PLAY {
            @Override protected void enter(ZombieFightAttraction instance) {
                instance.spawnZombies();
                instance.saveTag.playStarted = System.currentTimeMillis();
                instance.saveTag.shootCooldown = 40;
            }

            @Override protected void exit(ZombieFightAttraction instance) {
                instance.clearZombies();
            }

            @Override protected State tick(ZombieFightAttraction instance, Player player) {
                return instance.tickPlay(player);
            }
        };

        protected void enter(ZombieFightAttraction instance) { }

        protected void exit(ZombieFightAttraction instance) { }

        protected State tick(ZombieFightAttraction instance, Player player) {
            return null;
        }
    }

    protected static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected long warmupStarted;
        protected long playStarted;
        protected List<UUID> zombies;
        protected int zombiesHit;
        protected int zombieCount;
        protected int round;
        protected int shootCooldown;
    }

    @Override
    public void onPlayerHud(PlayerHudEvent event) {
        event.bossbar(PlayerHudPriority.HIGHEST,
                      makeProgressComponent(secondsLeft, Mytems.ZOMBIE_FACE, saveTag.zombiesHit, saveTag.zombieCount),
                      BossBar.Color.RED, BossBar.Overlay.PROGRESS,
                      (float) secondsLeft / (float) playTime.toSeconds());
    }

    @Override
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.MELTING) {
            event.setCancelled(true);
        }
    }
}
