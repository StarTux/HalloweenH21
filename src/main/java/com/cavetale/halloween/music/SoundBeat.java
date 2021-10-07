package com.cavetale.halloween.music;

import lombok.NonNull;
import lombok.Value;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Note.Tone;
import org.bukkit.Note;
import org.bukkit.entity.Player;

@Value
public final class SoundBeat implements Beat {
    @NonNull protected final Instrument instrument;
    protected final int ticks;
    protected final int octave;
    @NonNull protected final Tone tone;
    @NonNull protected final Semitone semitone;
    @NonNull protected final Note note;

    @Override
    public boolean hasSound() {
        return true;
    }

    @Override
    public boolean countsAs(Note other) {
        return note.equals(other)
            // Ignore octave
            || (note.getTone() == other.getTone()
                && note.isSharped() == other.isSharped());
    }

    @Override
    public String toDisplayString() {
        return tone + semitone.symbol;
    }

    @Override
    public void play(Player player, Location location) {
        player.playNote(location, instrument, note);
    }

    @Override
    public void play(Location location) {
        for (Player player : location.getWorld().getNearbyEntitiesByType(Player.class, location, 24.0)) {
            play(player, location);
        }
    }

    @Override
    public String serialize() {
        return instrument.name().toLowerCase()
            + "," + ticks
            + "," + tone + semitone.serialize() + octave;
    }

    public static SoundBeat deserialize(String in) {
        String[] toks = in.split(",");
        if (toks.length != 3) {
            throw new IllegalArgumentException("toks.length != 3");
        }
        final Instrument instrument = Instrument.valueOf(toks[0].toUpperCase());
        final int ticks = Integer.parseInt(toks[1]);
        final Tone tone = Tone.valueOf(toks[2].substring(0, 1));
        final Semitone semitone = toks[2].length() == 3
            ? Semitone.deserialize(toks[2].substring(1, 2))
            : Semitone.NATURAL;
        final int octave = Integer.parseInt(toks[2].substring(toks[2].length() - 1, toks[2].length()));
        return Beat.of(instrument, ticks, octave, tone, semitone);
    }
}
