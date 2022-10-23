package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.font.VanillaItems;
import com.cavetale.core.struct.Vec3i;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.text;

public final class SnowballFightAttraction extends Attraction<SnowballFightAttraction.SaveTag> {
    protected final Duration playTime = Duration.ofSeconds(60);
    protected final Duration warmupTime = Duration.ofSeconds(5);
    protected final List<Vec3i> snowmanBlocks = new ArrayList<>();
    @Setter private int totalRounds = 5;
    protected int secondsLeft;

    protected SnowballFightAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        Set<Vec3i> snowmanBlockSet = new HashSet<>();
        Set<Vec3i> noSnowmanBlockSet = new HashSet<>();
        for (Area area : allAreas) {
            if ("snowman".equals(area.name)) {
                snowmanBlockSet.addAll(area.enumerate());
            } else if ("nosnowman".equals(area.name)) {
                noSnowmanBlockSet.addAll(area.enumerate());
            }
        }
        snowmanBlocks.addAll(snowmanBlockSet);
        snowmanBlocks.removeAll(noSnowmanBlockSet);
        this.displayName = booth.format("Snowball Fight");
        this.description = text("These golems think they are undefeatable."
                                + " Please teach them a lesson for me!");
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

    protected void spawnSnowmen() {
        snowmanBlocks.removeIf(v -> {
                Block block = v.toBlock(world);
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
        Collections.shuffle(snowmanBlocks);
        saveTag.snowmen = new ArrayList<>();
        saveTag.round += 1;
        saveTag.snowmenHit = 0;
        saveTag.snowmanCount = (saveTag.round - 1) * 2 + 10;
        for (int i = 0; i < saveTag.snowmanCount; i += 1) {
            Vec3i vec = snowmanBlocks.get(i);
            Location location = vec.toCenterFloorLocation(world);
            location.setYaw(random.nextFloat() * 360.0f);
            Snowman snowman = location.getWorld().spawn(location, Snowman.class, s -> {
                    s.setPersistent(false);
                    s.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.0);
                    s.setGlowing(true);
                    Bukkit.getMobGoals().removeAllGoals(s);
                });
            if (snowman != null) {
                saveTag.snowmen.add(snowman.getUniqueId());
            }
        }
    }

    protected void clearSnowmen() {
        if (saveTag.snowmen == null) return;
        for (UUID uuid : saveTag.snowmen) {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) entity.remove();
        }
        saveTag.snowmen = null;
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
            player.getInventory().addItem(new ItemStack(Material.SNOWBALL, 16));
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
        // Snowball every X ticks
        if (saveTag.shootCooldown > 0) {
            saveTag.shootCooldown -= 1;
        } else {
            saveTag.shootCooldown = 60;
            for (UUID uuid : saveTag.snowmen) {
                Entity entity = Bukkit.getEntity(uuid);
                if (!(entity instanceof Snowman snowman)) continue;
                if (player.getLocation().getY() >= snowman.getLocation().getY() + 1.5 && snowman.hasLineOfSight(player)) {
                    Vector direction = player.getEyeLocation().toVector()
                        .subtract(snowman.getEyeLocation().toVector())
                        .multiply(1.5);
                    snowman.launchProjectile(Snowball.class, direction);
                } else {
                    final int total = 4;
                    for (int i = 0; i < total; i += 1) {
                        double frac = (double) i / (double) total;
                        double pi = frac * 2.0 * Math.PI + random.nextDouble() * Math.PI;
                        final double str = 1.5;
                        Vector direction = new Vector(Math.cos(pi) * str,
                                                      0.0,
                                                      Math.sin(pi) * str);
                        snowman.launchProjectile(Snowball.class, direction);
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
        if (!(event.getDamager() instanceof Snowball snowball)) return;
        if (event.getEntity() instanceof Player target) {
            if (!target.equals(getCurrentPlayer())) return;
            if (!(snowball.getShooter() instanceof Snowman)) return;
            target.sendActionBar(booth.format("You were hit by a snowball!"));
            fail(target);
            stop();
        } else if (event.getEntity() instanceof Snowman target) {
            event.setCancelled(true);
            if (!(snowball.getShooter() instanceof Player shooter)) return;
            if (!shooter.equals(getCurrentPlayer())) return;
            if (!saveTag.snowmen.remove(target.getUniqueId())) return;
            confetti(shooter, target.getEyeLocation());
            target.remove();
            progress(shooter);
            saveTag.snowmenHit += 1;
            if (saveTag.snowmen.isEmpty()) {
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
            @Override protected void enter(SnowballFightAttraction instance) {
                instance.saveTag.currentPlayer = null;
            }
        },
        WARMUP {
            @Override protected void enter(SnowballFightAttraction instance) {
                instance.saveTag.warmupStarted = System.currentTimeMillis();
            }

            @Override protected State tick(SnowballFightAttraction instance, Player player) {
                return instance.tickWarmup(player);
            }
        },
        PLAY {
            @Override protected void enter(SnowballFightAttraction instance) {
                instance.spawnSnowmen();
                instance.saveTag.playStarted = System.currentTimeMillis();
                instance.saveTag.shootCooldown = 40;
            }

            @Override protected void exit(SnowballFightAttraction instance) {
                instance.clearSnowmen();
            }

            @Override protected State tick(SnowballFightAttraction instance, Player player) {
                return instance.tickPlay(player);
            }
        };

        protected void enter(SnowballFightAttraction instance) { }

        protected void exit(SnowballFightAttraction instance) { }

        protected State tick(SnowballFightAttraction instance, Player player) {
            return null;
        }
    }

    protected static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected long warmupStarted;
        protected long playStarted;
        protected List<UUID> snowmen;
        protected int snowmenHit;
        protected int snowmanCount;
        protected int round;
        protected int shootCooldown;
    }

    @Override
    public void onPlayerHud(PlayerHudEvent event) {
        event.bossbar(PlayerHudPriority.HIGHEST,
                      makeProgressComponent(secondsLeft, VanillaItems.CARVED_PUMPKIN, saveTag.snowmenHit, saveTag.snowmanCount),
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
