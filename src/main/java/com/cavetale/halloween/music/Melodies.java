package com.cavetale.halloween.music;

import lombok.RequiredArgsConstructor;
import static com.cavetale.halloween.music.Melody.*;
import static com.cavetale.halloween.music.Semitone.*;
import static org.bukkit.Instrument.*;
import static org.bukkit.Note.Tone.*;

@RequiredArgsConstructor
public enum Melodies {
    MOONSHINE_SONATA(Melody.of(new Beat[] {
                // C D F G
                Beat.of(BASS_GUITAR, 0, 0, C, SHARP),
                Beat.of(BASS_GUITAR, 0, 1, C, SHARP),

                Beat.of(PIANO, N16, 0, G, SHARP),
                Beat.of(PIANO, N16, 0, C, SHARP),
                Beat.of(PIANO, N16, 0, E),
                Beat.of(PIANO, N16, 0, G, SHARP),
                Beat.of(PIANO, N16, 0, C, SHARP),
                Beat.of(PIANO, N16, 0, E),
                Beat.of(PIANO, N16, 0, G, SHARP),
                Beat.of(PIANO, N16, 0, C, SHARP),
                Beat.of(PIANO, N16, 0, E),
                Beat.of(PIANO, N16, 0, G, SHARP),
                Beat.of(PIANO, N16, 0, C, SHARP),
                Beat.of(PIANO, N16, 0, E),

                Beat.of(BASS_GUITAR, 0, 0, B),
                Beat.of(BASS_GUITAR, 0, 1, B),

                Beat.of(PIANO, N16, 0, G, SHARP),
                Beat.of(PIANO, N16, 0, C, SHARP),
                Beat.of(PIANO, N16, 0, E),
                Beat.of(PIANO, N16, 0, G, SHARP),
                Beat.of(PIANO, N16, 0, C, SHARP),
                Beat.of(PIANO, N16, 0, E),
                Beat.of(PIANO, N16, 0, G, SHARP),
                Beat.of(PIANO, N16, 0, C, SHARP),
                Beat.of(PIANO, N16, 0, E),
                Beat.of(PIANO, N16, 0, G, SHARP),
                Beat.of(PIANO, N16, 0, C, SHARP),
                Beat.of(PIANO, N16, 0, E),

                Beat.of(BASS_GUITAR, 0, 0, A),
                Beat.of(BASS_GUITAR, 0, 1, A),

                Beat.of(PIANO, N16, 0, A),
                Beat.of(PIANO, N16, 0, C, SHARP),
                Beat.of(PIANO, N16, 0, E),
                Beat.of(PIANO, N16, 0, A),
                Beat.of(PIANO, N16, 0, C, SHARP),
                Beat.of(PIANO, N16, 0, E),

                Beat.of(BASS_GUITAR, 0, 0, G, SHARP),
                Beat.of(BASS_GUITAR, 0, 1, G, SHARP),

                Beat.of(PIANO, N16, 0, A),
                Beat.of(PIANO, N16, 0, D), // natural
                Beat.of(PIANO, N16, 0, F, SHARP),
                Beat.of(PIANO, N16, 0, A),
                Beat.of(PIANO, N16, 0, D, SHARP),
                Beat.of(PIANO, N16, 0, F, SHARP),
            })),
    FAIRY_FOUNTAIN(Melody.of(new Beat[] {
                // Bb
                Beat.of(CHIME, N4, 1, A),
                Beat.of(CHIME, N4, 1, G),
                Beat.of(CHIME, N4, 0, F, SHARP),
                Beat.of(CHIME, N4, 1, G),
                Beat.of(CHIME, N4, 1, G),
                Beat.of(CHIME, N4, 0, F),
                Beat.of(CHIME, N4, 0, E),
                Beat.of(CHIME, N4, 0, F),
                Beat.of(CHIME, N4, 0, F),
                Beat.of(CHIME, N4, 0, E),
                //
                Beat.of(CHIME, N4, 0, E, FLAT),
                Beat.of(CHIME, N4, 0, E),
                Beat.of(CHIME, N4, 0, E),
                Beat.of(CHIME, N4, 0, D),
                Beat.of(CHIME, N4, 0, C, SHARP),
                Beat.of(CHIME, N4, 0, D),
                Beat.of(CHIME, N4, 1, A),
                Beat.of(CHIME, N4, 1, G),
                Beat.of(CHIME, N4, 0, F, SHARP),
                Beat.of(CHIME, N4, 1, G),
                //
                Beat.of(CHIME, N4, 1, B, FLAT),
                Beat.of(CHIME, N4, 1, A),
                Beat.of(CHIME, N4, 1, G, SHARP),
                Beat.of(CHIME, N4, 1, A),
                Beat.of(CHIME, N4, 1, C),
                Beat.of(CHIME, N4, 1, B),
                Beat.of(CHIME, N4, 1, A),
                Beat.of(CHIME, N4, 1, B),
                Beat.of(CHIME, N4, 1, A),
                Beat.of(CHIME, N4, 1, G),
                //
                Beat.of(CHIME, N4, 0, F),
                Beat.of(CHIME, N4, 0, E),
                Beat.of(CHIME, N4, 1, A),
                Beat.of(CHIME, N4, 1, G),
                Beat.of(CHIME, N4, 0, F, SHARP),
                Beat.of(CHIME, N4, 1, G),
            }));

    public final Melody melody;
}
