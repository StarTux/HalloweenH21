package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Cuboid;
import com.cavetale.core.event.player.PluginPlayerEvent.Detail;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.halloween.HalloweenPlugin;
import com.cavetale.mytems.item.music.Beat;
import com.cavetale.mytems.item.music.Melody;
import com.cavetale.mytems.item.music.Semitone;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Note.Tone;
import org.bukkit.Note;
import org.bukkit.entity.Player;

public final class RepeatMelodyAttraction extends Attraction<RepeatMelodyAttraction.SaveTag> {
    protected Melody melody = null;
    @Getter final Component displayName = Component.text("Play the Melody", NamedTextColor.DARK_RED);

    protected RepeatMelodyAttraction(final HalloweenPlugin plugin, final String name, final List<Cuboid> areaList) {
        super(plugin, name, areaList, SaveTag.class, SaveTag::new);
        this.doesRequireInstrument = true;
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
        if (saveTag.melody != null) {
            melody = Melody.deserialize(saveTag.melody);
        }
    }

    @Override
    public boolean isPlaying() {
        return saveTag.state != State.IDLE;
    }

    @Override
    protected void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        makeMelody();
        saveTag.maxNoteIndex = 2;
        startingGun(player);
        changeState(State.PLAY);
    }

    @Override
    protected void stop() {
        changeState(State.IDLE);
    }

    private void changeState(@NonNull State newState) {
        saveTag.state.exit(this);
        saveTag.state = newState;
        saveTag.state.enter(this);
    }

    protected void playNote(Player player, Location location) {
        Beat beat = melody.getBeats().get(saveTag.noteIndex);
        for (Player online : plugin.getPlayersIn(mainArea)) {
            beat.play(online, location);
        }
        List<Component> comps = new ArrayList<>();
        for (int i = 0; i <= saveTag.noteIndex; i += 1) {
            comps.add(Component.text(melody.getBeats().get(i).displayString));
        }
        Component line = Component.join(JoinConfiguration.separator(Component.space()), comps).color(NamedTextColor.AQUA);
        player.showTitle(Title.title(Component.empty(), line,
                                     Title.Times.of(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1))));
        saveTag.noteIndex += 1;
    }

    protected void makeMelody() {
        Melody.Builder melodyBuilder = Melody.builder(Instrument.PIANO, 100L);
        Tone[] tones = Tone.values();
        List<Tone> semis = new ArrayList<>(List.of(tones));
        Collections.shuffle(semis, random);
        semis = new ArrayList<>(semis.subList(0, 4));
        Semitone semitone = random.nextBoolean() ? Semitone.SHARP : Semitone.FLAT;
        for (int i = 0; i < 8; i += 1) {
            Tone tone = tones[random.nextInt(tones.length)];
            Semitone theSemi = semis.contains(tone) ? semitone : Semitone.NATURAL;
            melodyBuilder.beat(6, tone, theSemi, 1);
        }
        melody = melodyBuilder.build();
        saveTag.melody = melody.serialize();
    }

    protected State tickPlay() {
        Player player = getCurrentPlayer();
        if (player == null || !mainArea.contains(player.getLocation())) return State.IDLE;
        long now = System.currentTimeMillis();
        if (now < saveTag.lastNotePlayed + 666L) return null;
        saveTag.lastNotePlayed = now;
        playNote(player, player.getLocation()); // will +1
        if (saveTag.noteIndex > saveTag.maxNoteIndex) return State.REPLAY;
        return null;
    }

    protected void onEnterReplay() {
        saveTag.noteIndex = 0;
        saveTag.playerTimeout = System.currentTimeMillis() + 10000L;
        Player player = getCurrentPlayer();
        if (player == null) return;
        player.sendActionBar(Component.text("Your turn!", NamedTextColor.GREEN));
    }

    protected State tickReplay() {
        Player player = getCurrentPlayer();
        if (player == null || !mainArea.contains(player.getLocation())) return State.IDLE;
        if (System.currentTimeMillis() > saveTag.playerTimeout) {
            player.closeInventory();
            timeout(player);
            return State.IDLE;
        }
        return null;
    }

    @Override
    public void onPluginPlayer(PluginPlayerEvent event) {
        if (saveTag.state != State.REPLAY) return;
        if (event.getName() != PluginPlayerEvent.Name.PLAY_NOTE) return;
        Player player = event.getPlayer();
        if (!player.getUniqueId().equals(saveTag.currentPlayer)) return;
        Note note = Detail.NOTE.get(event, null);
        if (note == null) return;
        Beat beat = melody.getBeats().get(saveTag.noteIndex);
        if (beat.countsAs(note)) {
            saveTag.noteIndex += 1;
            if (saveTag.noteIndex > saveTag.maxNoteIndex) {
                saveTag.maxNoteIndex += 1;
                player.closeInventory();
                if (saveTag.maxNoteIndex >= melody.getBeats().size()) {
                    perfect(player);
                    melody.play(player);
                    prepareReward(player, true);
                    plugin.sessionOf(player).setCooldown(this, completionCooldown);
                    changeState(State.IDLE);
                    return;
                } else {
                    progress(player);
                    changeState(State.PLAY);
                    return;
                }
            } else {
                saveTag.playerTimeout = System.currentTimeMillis() + 10000L;
            }
        } else {
            player.closeInventory();
            fail(player);
            changeState(State.IDLE);
        }
    }

    @Override
    public Component getDescription() {
        return Component.text("I'll give you a melody and you're gonna repeat it. It gets harder every round.");
    }

    @RequiredArgsConstructor
    enum State {
        IDLE {
            @Override void enter(RepeatMelodyAttraction instance) {
                instance.saveTag.currentPlayer = null;
                instance.saveTag.melody = null;
                instance.melody = null;
            }

            @Override void exit(RepeatMelodyAttraction instance) {
            }
        },
        PLAY {
            @Override void enter(RepeatMelodyAttraction instance) {
                instance.saveTag.lastNotePlayed = System.currentTimeMillis();
                instance.saveTag.noteIndex = 0;
            }

            @Override State tick(RepeatMelodyAttraction instance) {
                return instance.tickPlay();
            }
        },
        REPLAY {
            @Override void enter(RepeatMelodyAttraction instance) {
                instance.onEnterReplay();
            }

            @Override State tick(RepeatMelodyAttraction instance) {
                return instance.tickReplay();
            }
        };

        void enter(RepeatMelodyAttraction instance) { }

        void exit(RepeatMelodyAttraction instance) { }

        State tick(RepeatMelodyAttraction instance) {
            return null;
        }
    }

    static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected int noteIndex;
        protected int maxNoteIndex;
        protected long lastNotePlayed;
        protected long playerTimeout;
        protected String melody;
    }
}
