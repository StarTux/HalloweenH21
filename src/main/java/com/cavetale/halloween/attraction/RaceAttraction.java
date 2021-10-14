package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Cuboid;
import com.cavetale.area.struct.Vec3i;
import com.cavetale.core.font.Unicode;
import com.cavetale.halloween.HalloweenPlugin;
import com.cavetale.mytems.Mytems;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class RaceAttraction extends Attraction<RaceAttraction.SaveTag> {
    protected static final Duration COUNTDOWN_TIME = Duration.ofSeconds(3);
    protected static final Duration MAX_RACE_TIME = Duration.ofSeconds(60);
    @Getter protected final Component displayName = Component.text("Who's Faster?", NamedTextColor.RED);
    protected int secondsRaced;
    protected int countdownSeconds;
    protected List<Cuboid> aiCheckpointList = new ArrayList<>();
    protected List<Cuboid> playerCheckpointList = new ArrayList<>();
    Cuboid startArea = Cuboid.ZERO;

    protected RaceAttraction(final HalloweenPlugin plugin, final String name, final List<Cuboid> areaList) {
        super(plugin, name, areaList, SaveTag.class, SaveTag::new);
        for (Cuboid area : areaList) {
            if ("ai".equals(area.name)) {
                aiCheckpointList.add(area);
            } else if ("player".equals(area.name)) {
                playerCheckpointList.add(area);
            } else if ("start".equals(area.name)) {
                startArea = area;
            }
        }
    }

    @Override
    public boolean isPlaying() {
        return saveTag.state != State.IDLE;
    }

    @Override
    public void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        changeState(State.COUNTDOWN);
    }

    @Override
    protected void stop() {
        changeState(State.IDLE);
    }

    @Override
    public void onTick() {
        if (saveTag.state == State.IDLE) return;
        Player player = getCurrentPlayer();
        if (player == null) return;
        State newState = saveTag.state.tick(this, player);
        if (newState != null) changeState(newState);
    }

    @Override
    public Component getDescription() {
        return Component.text("Race me once around the house? I bet you can't beat me; I'm super fast!");
    }

    protected State tickCountdown(Player player) {
        if (!startArea.contains(player.getLocation())) {
            Component message = Component.text("You left the starting area!", NamedTextColor.DARK_RED);
            player.sendMessage(message);
            player.showTitle(Title.title(Component.text("No cheating!", NamedTextColor.DARK_RED),
                                         message));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.MASTER, 2.0f, 0.5f);
            return State.IDLE;
        }
        long now = System.currentTimeMillis();
        long time = now - saveTag.countdownStarted;
        long timeLeft = COUNTDOWN_TIME.toMillis() - time;
        if (timeLeft <= 0) {
            player.showTitle(Title.title(Component.text("GO!", NamedTextColor.GOLD),
                                         Component.empty(),
                                         Title.Times.of(Duration.ZERO, Duration.ofMillis(500L), Duration.ZERO)));
            startingGun(player);
            return State.RACE;
        }
        int seconds = (int) ((timeLeft - 1L) / 1000L + 1L);
        if (seconds != countdownSeconds) {
            countdownSeconds = seconds;
            player.showTitle(Title.title(Component.text(seconds, NamedTextColor.GOLD),
                                         Component.text("Get Ready", NamedTextColor.GOLD),
                                         Title.Times.of(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)));
            List<Note.Tone> tones = List.of(Note.Tone.C, Note.Tone.A, Note.Tone.G);
            if (seconds < tones.size()) {
                player.playNote(player.getLocation(), Instrument.PLING, Note.natural(0, tones.get(seconds)));
            }
        }
        return null;
    }

    protected State tickRace(Player player) {
        if (saveTag.aiCheckpointIndex >= aiCheckpointList.size()) return State.IDLE;
        if (saveTag.playerCheckpointIndex >= playerCheckpointList.size()) return State.IDLE;
        long now = System.currentTimeMillis();
        long time = now - saveTag.raceStarted;
        if (time > MAX_RACE_TIME.toMillis()) {
            timeout(player);
            return State.IDLE;
        }
        int seconds = (int) (time / 1000L);
        if (mainVillager.getEntity() != null) {
            Cuboid checkpoint = aiCheckpointList.get(saveTag.aiCheckpointIndex);
            if (checkpoint.contains(mainVillager.getEntity().getLocation())) {
                saveTag.aiCheckpointIndex += 1;
                if (saveTag.aiCheckpointIndex >= aiCheckpointList.size()) {
                    if (!saveTag.playerFinished) {
                        timeout(player);
                    }
                    return State.IDLE;
                } else {
                    pathTo(saveTag.aiCheckpointIndex);
                }
            }
        }
        Cuboid playerCheckpoint = playerCheckpointList.get(saveTag.playerCheckpointIndex);
        if (playerCheckpoint.contains(player.getLocation())) {
            saveTag.playerCheckpointIndex += 1;
            if (saveTag.playerCheckpointIndex >= playerCheckpointList.size()) {
                victory(player);
                plugin.sessionOf(player).setCooldown(this, completionCooldown);
                prepareReward(player, true);
                return State.IDLE;
            } else {
                progress(player);
            }
        }
        if (seconds != secondsRaced) {
            secondsRaced = seconds;
            String progress = 0 + "/" + 1;
            player.sendActionBar(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                        Component.text(Unicode.WATCH.string + seconds, NamedTextColor.GOLD),
                        Component.space(),
                        Mytems.GOLDEN_CUP.component,
                        Component.text(progress, NamedTextColor.DARK_RED),
                    }));
            List<Vec3i> vectorList = playerCheckpoint.enumerate();
            Vec3i vector = vectorList.get(random.nextInt(vectorList.size()));
            confetti(player, vector.toLocation(plugin.getWorld()).add(0, 0.6, 0));
            pathTo(saveTag.aiCheckpointIndex);
        }
        return null;
    }

    protected void pathTo(int index) {
        if (index >= aiCheckpointList.size()) return;
        Cuboid checkpoint = aiCheckpointList.get(index);
        Vec3i vec = new Vec3i((checkpoint.min.x + checkpoint.max.x) / 2,
                              checkpoint.min.y,
                              (checkpoint.min.z + checkpoint.max.z) / 2);
        Block block = vec.toBlock(plugin.getWorld());
        while (block.isSolid()) {
            block = block.getRelative(0, 1, 0);
        }
        Location location = block.getLocation().add(0.5, 0.0, 0.5);
        mainVillager.pathing(mob -> {
                mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.45);
                mob.setCollidable(false);
                mob.getPathfinder().moveTo(location, 1.0);
            });
    }

    protected void changeState(State newState) {
        State oldState = saveTag.state;
        saveTag.state = newState;
        oldState.exit(this);
        newState.enter(this);
    }

    enum State {
        IDLE,
        COUNTDOWN {
            @Override protected void enter(RaceAttraction instance) {
                instance.countdownSeconds = -1;
                instance.saveTag.countdownStarted = System.currentTimeMillis();
            }

            @Override protected State tick(RaceAttraction instance, Player player) {
                return instance.tickCountdown(player);
            }
        },
        RACE {
            @Override protected State tick(RaceAttraction instance, Player player) {
                return instance.tickRace(player);
            }

            @Override protected void enter(RaceAttraction instance) {
                instance.saveTag.raceStarted = System.currentTimeMillis();
                instance.secondsRaced = -1;
                instance.saveTag.aiCheckpointIndex = 0;
                instance.saveTag.playerCheckpointIndex = 0;
                instance.saveTag.playerFinished = false;
                instance.mainVillager.teleportHome();
                instance.pathTo(0);
            }

            @Override protected void exit(RaceAttraction instance) {
                instance.mainVillager.pathing(mob -> mob.getPathfinder().stopPathfinding());
                instance.mainVillager.teleportHome();
            }
        };

        protected void enter(RaceAttraction instance) { }

        protected void exit(RaceAttraction instance) { }

        protected State tick(RaceAttraction instance, Player player) {
            return null;
        }
    }

    protected static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected long countdownStarted;
        protected long raceStarted;
        protected int aiCheckpointIndex;
        protected int playerCheckpointIndex;
        protected boolean playerFinished;
    }
}