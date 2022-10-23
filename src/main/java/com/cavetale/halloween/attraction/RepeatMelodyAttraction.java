package com.cavetale.halloween.attraction;

import com.cavetale.core.event.player.PluginPlayerEvent.Detail;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.mytems.item.music.Beat;
import com.cavetale.mytems.item.music.Melody;
import com.cavetale.mytems.item.music.MelodyBuilder;
import com.cavetale.mytems.item.music.Semitone;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Note.Tone;
import org.bukkit.Note;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class RepeatMelodyAttraction extends Attraction<RepeatMelodyAttraction.SaveTag> {
    protected Melody melody;
    @Setter protected Instrument instrument = Instrument.PIANO;
    @Setter protected int octave = 0;

    protected RepeatMelodyAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        this.doesRequireInstrument = true;
        this.displayName = booth.format("Play the Melody");
        this.description = text("I'll give you a melody and you're gonna repeat it. It gets harder every round.");
        Random random2 = new Random(npcVector.hashCode());
        octave = random2.nextInt(2);
        Instrument[] instruments = Instrument.values();
        instrument = instruments[random2.nextInt(instruments.length)];
        makeMelody(random2);
    }

    public void set(Instrument theInstr, int theOctave) {
        this.instrument = theInstr;
        this.octave = theOctave;
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
    }

    @Override
    public boolean isPlaying() {
        return saveTag.state != State.IDLE;
    }

    @Override
    protected void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
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

    private String toString(Beat beat) {
        return beat.getTone() + (beat.getSemitone() != null ? beat.getSemitone().toString() : "");
    }

    protected void playNote(Player player, Location location) {
        Beat beat = melody.getBeats().get(saveTag.noteIndex);
        for (Player online : getPlayersIn(mainArea)) {
            beat.play(melody, online, location);
        }
        List<Component> comps = new ArrayList<>();
        for (int i = 0; i <= saveTag.noteIndex; i += 1) {
            comps.add(text(toString(melody.getBeats().get(i))));
        }
        Component line = join(separator(space()), comps).color(AQUA);
        player.showTitle(Title.title(empty(), line,
                                     Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1))));
        saveTag.noteIndex += 1;
    }

    protected void makeMelody(Random random2) {
        MelodyBuilder melodyBuilder = Melody.builder(instrument, 100L);
        Tone[] tones = Tone.values();
        List<Tone> semis = new ArrayList<>(List.of(tones));
        final Semitone semitone;
        if (random2.nextBoolean()) {
            semis.removeIf(tone -> !tone.isSharpable());
            semitone = Semitone.SHARP;
        } else {
            semis.removeIf(tone -> tone.ordinal() > 0 && !tones[tone.ordinal() - 1].isSharpable());
            semitone = Semitone.FLAT;
        }
        Collections.shuffle(semis, random2);
        semis = new ArrayList<>(semis.subList(0, random2.nextInt(4)));
        for (int i = 0; i < 8; i += 1) {
            Tone tone = tones[random2.nextInt(tones.length)];
            if (semis.contains(tone)) {
                melodyBuilder.beat(6, tone, semitone, octave);
            } else {
                melodyBuilder.beat(6, tone, octave);
            }
        }
        melody = melodyBuilder.build();
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
        player.sendActionBar(text("Your turn!", GREEN));
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
        if (beat.countsAs(melody, note)) {
            saveTag.noteIndex += 1;
            if (saveTag.noteIndex > saveTag.maxNoteIndex) {
                saveTag.maxNoteIndex += 1;
                player.closeInventory();
                if (saveTag.maxNoteIndex >= melody.getBeats().size()) {
                    perfect(player);
                    melody.play(plugin, player);
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

    @RequiredArgsConstructor
    enum State {
        IDLE {
            @Override void enter(RepeatMelodyAttraction instance) {
                instance.saveTag.currentPlayer = null;
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
    }
}
