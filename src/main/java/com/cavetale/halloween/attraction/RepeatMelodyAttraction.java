package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Cuboid;
import com.cavetale.core.event.player.PluginPlayerEvent.Detail;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.halloween.HalloweenPlugin;
import com.cavetale.halloween.music.Beat;
import com.cavetale.halloween.music.Melody;
import com.cavetale.halloween.music.Semitone;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Note.Tone;
import org.bukkit.Note;
import org.bukkit.entity.Player;

public final class RepeatMelodyAttraction extends Attraction<SaveTag> {
    protected Melody melody = null;

    RepeatMelodyAttraction(final HalloweenPlugin plugin, final String name, final List<Cuboid> areaList) {
        super(plugin, name, areaList, SaveTag.class);
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
        changeState(State.PLAY);
    }

    private void changeState(@NonNull State newState) {
        saveTag.state.exit(this);
        saveTag.state = newState;
        saveTag.state.enter(this);
    }

    protected void playNote(Player player, Location location) {
        Beat beat = melody.getBeat(saveTag.noteIndex);
        for (Player online : plugin.getPlayersIn(mainArea)) {
            beat.play(online, location);
        }
        List<Component> comps = new ArrayList<>();
        for (int i = 0; i <= saveTag.noteIndex; i += 1) {
            comps.add(Component.text(melody.getBeat(i).toDisplayString()));
        }
        Component line = Component.join(JoinConfiguration.separator(Component.space()), comps).color(NamedTextColor.AQUA);
        player.showTitle(Title.title(Component.empty(), line,
                                     Title.Times.of(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1))));
        saveTag.noteIndex += 1;
    }

    protected void makeMelody() {
        List<Beat> beats = new ArrayList<>();
        Tone[] tones = Tone.values();
        List<Tone> semis = new ArrayList<>(List.of(tones));
        Collections.shuffle(semis, random);
        semis = semis.subList(0, 1 + random.nextInt(4));
        Semitone semitone = random.nextBoolean() ? Semitone.SHARP : Semitone.FLAT;
        for (int i = 0; i < 8; i += 1) {
            Tone tone = tones[random.nextInt(tones.length)];
            Semitone theSemi = semis.contains(tone) ? semitone : Semitone.NATURAL;
            beats.add(Beat.of(Instrument.PIANO, 16, 1, tone, theSemi));
        }
        melody = Melody.of(beats);
        saveTag.melody = melody.serialize();
    }

    protected State tickPlay() {
        Player player = Bukkit.getPlayer(saveTag.currentPlayer);
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
        Player player = Bukkit.getPlayer(saveTag.currentPlayer);
        if (player == null) return;
        player.sendActionBar(Component.text("Your turn!", NamedTextColor.GREEN));
    }

    protected State tickReplay() {
        Player player = Bukkit.getPlayer(saveTag.currentPlayer);
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
        Beat beat = melody.getBeat(saveTag.noteIndex);
        if (beat.countsAs(note)) {
            saveTag.noteIndex += 1;
            if (saveTag.noteIndex > saveTag.maxNoteIndex) {
                saveTag.maxNoteIndex += 1;
                player.closeInventory();
                if (saveTag.maxNoteIndex >= melody.size()) {
                    victory(player);
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
            wrong(player);
            changeState(State.IDLE);
        }
    }
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

final class SaveTag {
    protected State state = State.IDLE;
    protected UUID currentPlayer = null;
    protected int noteIndex;
    protected int maxNoteIndex;
    protected long lastNotePlayed;
    protected long playerTimeout;
    protected List<String> melody;
}
