package com.cavetale.halloween.music;

import com.cavetale.halloween.HalloweenPlugin;
import java.util.logging.Level;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Note.Tone;
import org.bukkit.Note;
import org.bukkit.entity.Player;

public interface Beat {
    Beat ZERO = new PauseBeat(0);

    static SoundBeat of(Instrument instrument, int ticks, int octave, Tone tone, Semitone semitone) {
        return new SoundBeat(instrument, ticks, octave, tone, semitone, semitone.apply(Note.natural(octave, tone)));
    }

    static SoundBeat of(Instrument instrument, int ticks, int octave, Tone tone) {
        return of(instrument, ticks, octave, tone, Semitone.NATURAL);
    }

    static PauseBeat of(int ticks) {
        return new PauseBeat(ticks);
    }

    static Beat deserialize(String in) {
        if (in.startsWith("P")) {
            try {
                return new PauseBeat(Integer.parseInt(in.substring(1)));
            } catch (IllegalArgumentException iae) {
                HalloweenPlugin.getInstance().getLogger().log(Level.SEVERE, "Beat.deserialize:" + in, iae);
                return ZERO;
            }
        } else {
            try {
                return SoundBeat.deserialize(in);
            } catch (IllegalArgumentException iae) {
                HalloweenPlugin.getInstance().getLogger().log(Level.SEVERE, "SoundBeat.deserialize:" + in, iae);
                return ZERO;
            }
        }
    }

    int getTicks();

    boolean hasSound();

    boolean countsAs(Note other);

    String toDisplayString();

    void play(Player player, Location location);

    void play(Location location);

    String serialize();
}
