package com.cavetale.halloween.music;

import com.cavetale.mytems.Mytems;
import lombok.RequiredArgsConstructor;
import org.bukkit.Note;

@RequiredArgsConstructor
public enum Semitone {
    NATURAL("", Mytems.INVISIBLE_ITEM) {
        @Override public Note apply(Note in) {
            return in;
        }
    },
    SHARP("\u266F", Mytems.MUSICAL_SHARP) {
        @Override public Note apply(Note in) {
            return in.sharped();
        }
    },
    FLAT("\u266D", Mytems.MUSICAL_FLAT) {
        @Override public Note apply(Note in) {
            return in.flattened();
        }
    };

    public final String symbol;
    public final Mytems mytems;
    public abstract Note apply(Note in);

    public String serialize() {
        return symbol;
    }

    public static Semitone deserialize(String in) {
        if (in.isEmpty()) return NATURAL;
        if (in.equals(SHARP.symbol)) return SHARP;
        if (in.equals(FLAT.symbol)) return FLAT;
        throw new IllegalArgumentException(in);
    }
}
