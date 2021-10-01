package com.cavetale.halloween;

import com.cavetale.area.struct.Cuboid;
import com.cavetale.core.event.player.PluginPlayerEvent.Detail;
import com.cavetale.core.event.player.PluginPlayerEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.entity.Player;

public final class PlayMusicAttraction extends Attraction {
    @Getter @Setter protected SaveTag saveTag = new SaveTag();
    protected List<Click> melody;
    protected final Instrument instrument;

    PlayMusicAttraction(final HalloweenPlugin plugin, final String name, final List<Cuboid> areaList) {
        super(plugin, name, areaList);
        this.melody = makeMelody(name);
        this.instrument = makeInstrument(name);
    }

    protected static List<Click> makeMelody(String name) {
        switch (name) {
        default:
            return List.of(Click.of(Note.Tone.C),
                           Click.of(Note.Tone.A),
                           Click.of(Note.Tone.F),
                           Click.of(Note.Tone.F),
                           Click.of(Note.Tone.E),
                           Click.of(Note.Tone.E));
        }
    }

    protected static Instrument makeInstrument(String name) {
        switch (name) {
        default:
            return Instrument.PIANO;
        }
    }

    @Override
    protected void onTick() {
        State newState = saveTag.state.tick(this);
        if (newState != null) {
            changeState(newState);
        }
    }

    @Override
    protected void onClickMainVillager(Player player) {
        if (saveTag.state == State.IDLE) {
            saveTag.currentPlayer = player.getUniqueId();
            changeState(State.PLAY);
        }
    }

    protected void changeState(@NonNull State newState) {
        saveTag.state.exit(this);
        saveTag.state = newState;
        saveTag.state.enter(this);
    }

    protected void playNote(Player player, Location location) {
        Click click = melody.get(saveTag.noteIndex);
        for (Player online : plugin.getPlayersIn(mainArea)) {
            online.playNote(location, instrument, melody.get(saveTag.noteIndex).bukkitNote);
        }
        List<Component> comps = new ArrayList<>();
        for (int i = 0; i <= saveTag.noteIndex; i += 1) {
            comps.add(Component.text(melody.get(i).toString()));
        }
        Component line = Component.join(JoinConfiguration.separator(Component.space()), comps).color(NamedTextColor.AQUA);
        player.showTitle(Title.title(Component.empty(), line,
                                     Title.Times.of(Duration.ZERO, Duration.ofSeconds(1), Duration.ofSeconds(1))));
        saveTag.noteIndex += 1;
    }

    protected enum State {
        IDLE {
            @Override void enter(PlayMusicAttraction instance) {
                instance.saveTag.currentPlayer = null;
            }

            @Override void exit(PlayMusicAttraction instance) {
                instance.saveTag.maxNoteIndex = 2;
            }
        },
        PLAY {
            @Override void enter(PlayMusicAttraction instance) {
                instance.saveTag.lastNotePlayed = System.currentTimeMillis();
                instance.saveTag.noteIndex = 0;
            }

            @Override State tick(PlayMusicAttraction instance) {
                Player player = Bukkit.getPlayer(instance.saveTag.currentPlayer);
                if (player == null || !instance.mainArea.contains(player.getLocation())) return State.IDLE;
                long now = System.currentTimeMillis();
                if (now < instance.saveTag.lastNotePlayed + 1000L) return null;
                instance.saveTag.lastNotePlayed = now;
                instance.playNote(player, player.getLocation()); // will +1
                if (instance.saveTag.noteIndex > instance.saveTag.maxNoteIndex) return State.REPLAY;
                return null;
            }
        },
        REPLAY {
            @Override void enter(PlayMusicAttraction instance) {
                instance.saveTag.noteIndex = 0;
                instance.saveTag.playerTimeout = System.currentTimeMillis() + 10000L;
            }

            @Override State tick(PlayMusicAttraction instance) {
                Player player = Bukkit.getPlayer(instance.saveTag.currentPlayer);
                if (player == null || !instance.mainArea.contains(player.getLocation())) return State.IDLE;
                if (System.currentTimeMillis() > instance.saveTag.playerTimeout) {
                    player.closeInventory();
                    instance.timeout(player);
                    return State.IDLE;
                }
                return null;
            }
        };

        void enter(PlayMusicAttraction instance) { }
        void exit(PlayMusicAttraction instance) { }
        State tick(PlayMusicAttraction instance) {
            return null;
        }
    }

    @RequiredArgsConstructor
    protected enum Modifier {
        NATURAL("") {
            @Override public Note modify(Note in) {
                return in;
            }
        },
        SHARP("\uu266F") {
            @Override public Note modify(Note in) {
                return in.sharped();
            }
        },
        FLAT("\u266D") {
            @Override public Note modify(Note in) {
                return in.flattened();
            }
        };

        public final String symbol;
        public abstract Note modify(Note in);
    }

    protected static final class Click {
        protected final Note.Tone tone;
        protected final Modifier modifier;
        protected final int octave;
        protected final Note bukkitNote;

        protected Click(final Note.Tone tone, final Modifier modifier, final int octave) {
            this.tone = tone;
            this.modifier = modifier;
            this.octave = octave;
            this.bukkitNote = modifier.modify(Note.natural(octave, tone));
        }

        public static Click of(Note.Tone tone, final Modifier modifier, int octave) {
            return new Click(tone, modifier, octave);
        }

        public static Click of(Note.Tone tone, final Modifier modifier) {
            return new Click(tone, modifier, 0);
        }

        public static Click of(Note.Tone tone) {
            return new Click(tone, Modifier.NATURAL, 0);
        }

        public boolean countsAs(Note note) {
            return bukkitNote.equals(note)
                // Ignore octave:
                || (bukkitNote.getTone() == note.getTone()
                    && bukkitNote.isSharped() == note.isSharped());
        }

        @Override
        public String toString() {
            return tone.name() + modifier.symbol;
        }
    }

    protected static final class SaveTag {
        protected State state = State.IDLE;
        protected UUID currentPlayer = null;
        protected int noteIndex;
        protected int maxNoteIndex;
        protected long lastNotePlayed;
        protected long playerTimeout;
    }

    protected void onPluginPlayer(PluginPlayerEvent event) {
        if (saveTag.state != State.REPLAY) return;
        if (event.getName() != PluginPlayerEvent.Name.PLAY_NOTE) return;
        Player player = event.getPlayer();
        if (!player.getUniqueId().equals(saveTag.currentPlayer)) return;
        Note note = Detail.NOTE.get(event, null);
        if (note == null) return;
        Click click = melody.get(saveTag.noteIndex);
        if (click.countsAs(note)) {
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
