package com.cavetale.halloween;

import com.cavetale.mytems.item.music.Melody;
import com.cavetale.mytems.item.music.Semitone;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.Note.Tone;
import static com.cavetale.mytems.item.music.Semitone.*;
import static org.bukkit.Instrument.*;
import static org.bukkit.Note.Tone.*;

@RequiredArgsConstructor
public enum Music {
    MOONSHINE_SONATA(Map.of(C, SHARP,
                            D, SHARP,
                            F, SHARP,
                            G, SHARP),
                     Melody.builder(PIANO, 200L)
                     .key(C, SHARP)
                     .key(D, SHARP)
                     .key(F, SHARP)
                     .key(G, SHARP)
                     .beat(BASS_GUITAR, C, 0)
                     .beat(BASS_GUITAR, C, 0)
                     .beat(2, G, 0)
                     .beat(2, C, 0)
                     .beat(2, E, 0)
                     .beat(2, G, 0)
                     .beat(2, C, 0)
                     .beat(2, E, 0)
                     .beat(2, G, 0)
                     .beat(2, C, 0)
                     .beat(2, E, 0)
                     .beat(2, G, 0)
                     .beat(2, C, 0)
                     .beat(2, E, 0)
                     .beat(BASS_GUITAR, B, 0)
                     .beat(BASS_GUITAR, B, 0)
                     .beat(2, G, 0)
                     .beat(2, C, 0)
                     .beat(2, E, 0)
                     .beat(2, G, 0)
                     .beat(2, C, 0)
                     .beat(2, E, 0)
                     .beat(2, G, 0)
                     .beat(2, C, 0)
                     .beat(2, E, 0)
                     .beat(2, G, 0)
                     .beat(2, C, 0)
                     .beat(2, E, 0)
                     .beat(BASS_GUITAR, A, 0)
                     .beat(BASS_GUITAR, A, 0)
                     .beat(2, A, 0)
                     .beat(2, C, 0)
                     .beat(2, E, 0)
                     .beat(2, A, 0)
                     .beat(2, C, 0)
                     .beat(2, E, 0)
                     .beat(BASS_GUITAR, G, 0)
                     .beat(BASS_GUITAR, G, 0)
                     .beat(2, A, 0)
                     .beat(2, D, NATURAL, 0)
                     .beat(2, F, 0)
                     .beat(2, A, 0)
                     .beat(2, D, 0)
                     .beat(2, F, 0)
                     .build()),

    FAIRY_FOUNTAIN(Map.of(B, FLAT),
                   Melody.builder(CHIME, 100L)
                   .key(B, FLAT)
                   .beat(8, A, 1)
                   .beat(8, G, 1)
                   .beat(8, F, SHARP, 0)
                   .beat(8, G, 1)
                   .beat(8, G, 1)
                   .beat(8, F, 0)
                   .beat(8, E, 0)
                   .beat(8, F, 0)
                   .beat(8, F, 0)
                   .beat(8, E, 0)
                   .beat(8, E, FLAT, 0)
                   .beat(8, E, 0)
                   .beat(8, E, 0)
                   .beat(8, D, 0)
                   .beat(8, C, SHARP, 0)
                   .beat(8, D, 0)
                   .beat(8, A, 1)
                   .beat(8, G, 1)
                   .beat(8, F, SHARP, 0)
                   .beat(8, G, 1)
                   .beat(8, B, 1)
                   .beat(8, A, 1)
                   .beat(8, G, SHARP, 1)
                   .beat(8, A, 1)
                   .beat(8, C, 1)
                   .beat(8, B, 1)
                   .beat(8, A, 1)
                   .beat(8, B, 1)
                   .beat(8, A, 1)
                   .beat(8, G, 1)
                   .beat(8, F, 0)
                   .beat(8, E, 0)
                   .beat(8, A, 1)
                   .beat(8, G, 1)
                   .beat(8, F, SHARP, 0)
                   .beat(8, G, 1)
                   .build());

    public final Map<Tone, Semitone> keys;
    public final Melody melody;
}
