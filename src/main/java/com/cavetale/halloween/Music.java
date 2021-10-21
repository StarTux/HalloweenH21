package com.cavetale.halloween;

import com.cavetale.mytems.item.music.Melody;
import com.cavetale.mytems.item.music.Semitone;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.Instrument;
import org.bukkit.Note.Tone;
import static com.cavetale.mytems.item.music.Semitone.*;
import static org.bukkit.Instrument.*;
import static org.bukkit.Note.Tone.*;

public enum Music {
    HAPPY_BIRTHDAY(PLING, 100L,
                   Map.of(),
                   b -> b
                   .beat(2, G, 0)
                   .beat(2, G, 0)
                   .beat(0, C, 0)
                   .beat(0, E, 0)
                   .beat(0, G, 1)
                   .beat(4, A, 0)
                   .beat(4, G, 0)
                   .beat(4, C, 0)
                   .beat(0, PIANO, D, 0)
                   .beat(0, PIANO, F, 0)
                   .beat(0, PIANO, G, 1)
                   .beat(8, B, 0)
                   .beat(2, G, 0)
                   .beat(2, G, 0)
                   .beat(0, PIANO, A, 0)
                   .beat(0, PIANO, F, 0)
                   .beat(0, PIANO, G, 1)
                   .beat(4, A, 0)
                   .beat(4, G, 0)
                   .beat(4, D, 0)
                   .beat(0, PIANO, C, 0)
                   .beat(0, PIANO, E, 0)
                   .beat(0, PIANO, G, 1)
                   .beat(8, C, 0)
                   .beat(2, G, 0)
                   .beat(2, G, 0)
                   .beat(0, PIANO, E, 0)
                   .beat(0, PIANO, A, SHARP, 1)
                   .beat(0, PIANO, B, 1)
                   .beat(4, G, 1)
                   .beat(4, E, 0)
                   .beat(4, C, 0)
                   .beat(0, PIANO, F, 0)
                   .beat(0, PIANO, A, 1)
                   .beat(0, PIANO, B, 1)
                   .beat(4, B, 0)
                   .beat(4, A, 0)
                   .beat(2, F, 0)
                   .beat(2, F, 0)
                   .beat(0, PIANO, E, 0)
                   .beat(0, PIANO, G, 1)
                   .beat(0, PIANO, C, 1)
                   .beat(4, E, 0)
                   .beat(4, C, 0)
                   .beat(0, PIANO, D, 0)
                   .beat(0, PIANO, F, 0)
                   .beat(0, PIANO, G, 1)
                   .beat(4, D, 0)
                   .beat(0, PIANO, C, 0)
                   .beat(0, PIANO, E, 0)
                   .beat(0, PIANO, G, 1)
                   .beat(8, C, 0)),

    FOX_DU_HAST_DIE_GANS_GESTOHLEN(FLUTE, 200L,
                                   Map.of(C, SHARP,
                                          F, SHARP),
                                   b -> b
                                   .beat(2, D, 0) // Fox
                                   .beat(2, E, 0) // du
                                   .beat(2, F, 0) // hast
                                   .beat(2, G, 1) // die
                                   .beat(2, A, 1) // Gans
                                   .beat(2, A, 1) // ge-
                                   .beat(2, A, 1) // stoh-
                                   .beat(2, A, 1) // len
                                   .beat(2, B, 1) // gib
                                   .beat(2, G, 1) // sie
                                   .beat(2, D, 1) // wie-
                                   .beat(2, B, 1) // der
                                   .beat(8, A, 1) // her // 16
                                   .beat(2, B, 1)
                                   .beat(2, G, 1)
                                   .beat(2, D, 1)
                                   .beat(2, B, 1)
                                   .beat(8, A, 1) // 16
                                   .beat(2, A, 1) // sonst
                                   .beat(2, G, 1) // wird
                                   .beat(2, G, 1) // dich
                                   .beat(2, G, 1) // der
                                   .beat(2, G, 1) // Jä-
                                   .beat(2, F, 0) // ger
                                   .beat(2, F, 0) // ho-
                                   .beat(2, F, 0) // len
                                   .beat(2, F, 0) // mit
                                   .beat(2, E, 0) // dem
                                   .beat(2, F, 0) // Schieß-
                                   .beat(2, E, 0) // ge-
                                   .beat(2, D, 0) // we-
                                   .beat(2, F, 0) // he-
                                   .beat(4, A, 1) // her
                                   .beat(2, A, 1) // sonst
                                   .beat(2, G, 1) // wird
                                   .beat(2, G, 1) // dich
                                   .beat(2, G, 1) // der
                                   .beat(2, G, 1) // Jä-
                                   .beat(2, F, 0) // ger
                                   .beat(2, F, 0) // ho-
                                   .beat(2, F, 0) // len
                                   .beat(2, F, 0) // mit
                                   .beat(2, E, 0) // dem
                                   .beat(2, F, 0) // Schieß-
                                   .beat(2, E, 0) // ge-
                                   .beat(8, D, 0) // wehr // 16
                                   ),

    MOONSHINE_SONATA(PIANO, 200L,
                     Map.of(C, SHARP,
                            D, SHARP,
                            F, SHARP,
                            G, SHARP),
                     b -> b
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
                     .beat(2, F, 0)),

    ALLE_MEINE_ENTCHEN(GUITAR, 150L,
                       Map.of(),
                       b -> b
                       .beat(2, C, 0) // Al-
                       .beat(2, D, 0) // le
                       .beat(2, E, 0) // mei-
                       .beat(2, F, 0) // ne
                       .beat(4, G, 1) // Ent-
                       .beat(4, G, 1) // chen
                       .beat(2, A, 1) // schwim-
                       .beat(2, A, 1) // men
                       .beat(2, A, 1) // auf
                       .beat(2, A, 1) // dem
                       .beat(8, G, 1) // See,
                       .beat(2, A, 1) // schwim-
                       .beat(2, A, 1) // men
                       .beat(2, A, 1) // auf
                       .beat(2, A, 1) // dem
                       .beat(8, G, 1) // See,
                       .beat(2, F, 0) // Köpf-
                       .beat(2, F, 0) // chen
                       .beat(2, F, 0) // in
                       .beat(2, F, 0) // das
                       .beat(4, E, 0) // Was-
                       .beat(4, E, 0) // ser,
                       .beat(2, D, 0) // Schänz-
                       .beat(2, D, 0) // chen
                       .beat(2, D, 0) // in
                       .beat(2, D, 0) // die
                       .beat(8, C, 0)), // Höh'

    FLINTSTONES(IRON_XYLOPHONE, 100L,
                Map.of(),
                b -> b
                .beat(4, G, 1) // Flint-
                .beat(4, C, 0) // stones,
                .pause(4)
                .beat(4, C, 1) // meet
                .beat(2, A, 1) // the
                .beat(4, G, 1) // Flint-
                .beat(4, C, 0) // stones,
                .pause(4)
                .beat(4, G, 1) // they're
                .beat(2, F, 0) // the
                .beat(2, E, 0) // mod-
                .beat(2, E, 0) // ern
                .beat(2, F, 0) // stone
                .beat(2, G, 1) // age
                .beat(4, C, 0) // fam-
                .beat(4, D, 0) // -i-
                .beat(4, E, 0) // ly.
                .beat(1, XYLOPHONE, A, 0)
                .beat(1, XYLOPHONE, B, 0)
                .beat(1, XYLOPHONE, C, 0)
                .beat(1, XYLOPHONE, D, 0)
                .beat(1, XYLOPHONE, B, 0)
                .beat(1, XYLOPHONE, C, 0)
                .beat(1, XYLOPHONE, D, 0)
                .beat(1, XYLOPHONE, E, 0)
                .beat(1, XYLOPHONE, C, 0)
                .beat(1, XYLOPHONE, D, 0)
                .beat(1, XYLOPHONE, E, 0)
                .beat(1, XYLOPHONE, F, 0)
                .beat(4, G, 1) // Flint-
                .beat(4, C, 0) // stones,
                .pause(4)
                .beat(4, C, 1) // meet
                .beat(2, A, 1) // the
                .beat(4, G, 1) // Flint-
                .beat(4, C, 0) // stones,
                .pause(4)
                .beat(4, G, 1) // they're
                .beat(2, F, 0) // the
                .beat(2, E, 0) // mod-
                .beat(2, E, 0) // ern
                .beat(2, F, 0) // stone
                .beat(2, G, 1) // age
                .beat(4, C, 0) // fam-
                .beat(4, D, 0) // -i-
                .beat(4, E, 0) // ly.
                .beat(1, XYLOPHONE, A, 0)
                .beat(1, XYLOPHONE, B, 0)
                .beat(1, XYLOPHONE, C, 0)
                .beat(1, XYLOPHONE, D, 0)
                .beat(1, XYLOPHONE, B, 0)
                .beat(1, XYLOPHONE, C, 0)
                .beat(1, XYLOPHONE, D, 0)
                .beat(1, XYLOPHONE, E, 0)
                .beat(1, XYLOPHONE, C, 0)
                .beat(1, XYLOPHONE, D, 0)
                .beat(1, XYLOPHONE, E, 0)
                .beat(1, XYLOPHONE, F, 0)),

    ODE_TO_JOY(PIANO, 100L,
               Map.of(F, SHARP),
               b -> b
               .beat(0, G, 1).beat(0, B, 1).beat(0, D, 1)
               .beat(4, B, 1) // Freu-
               .beat(4, B, 1) // de
               .beat(4, C, 1) // schö-
               .beat(4, D, 1) // ner
               .beat(0, F, 0).beat(0, A, 1).beat(0, D, 1)
               .beat(4, D, 1) // Göt-
               .beat(4, C, 1) // ter
               .beat(4, B, 1) // Fun-
               .beat(4, A, 1) // ken
               .beat(0, G, 1).beat(0, B, 1).beat(0, D, 1)
               .beat(4, G, 1) // Toch-
               .beat(4, G, 1) // ter
               .beat(4, A, 1) // aus
               .beat(4, B, 1) // E-
               .beat(0, G, 1).beat(0, B, 1).beat(0, D, 1)
               .beat(6, B, 1) // ly
               .beat(2, A, 1) // si
               .beat(0, F, 0).beat(0, A, 1).beat(0, D, 1)
               .beat(8, A, 1) // um
               .beat(0, G, 1).beat(0, B, 1).beat(0, D, 1)
               .beat(4, B, 1) // Wir
               .beat(4, B, 1) // be
               .beat(4, C, 1) // tre
               .beat(4, D, 1) // ten
               .beat(0, F, 0).beat(0, A, 1).beat(0, D, 1)
               .beat(4, D, 1) // feu
               .beat(4, C, 1) // er
               .beat(4, B, 1) // trun
               .beat(4, A, 1) // ken
               .beat(0, G, 1).beat(0, B, 1).beat(0, D, 1)
               .beat(4, G, 1) // himm
               .beat(4, G, 1) // li
               .beat(4, A, 1) // sche
               .beat(4, B, 1) // dein
               .beat(0, F, 0).beat(0, A, 1).beat(0, D, 1)
               .beat(6, A, 1) // Hei
               .beat(2, G, 1) // lig
               .beat(0, G, 1).beat(0, B, 1).beat(0, D, 1)
               .beat(8, G, 1) // tum
               .beat(0, PIANO, D, 0) // Octave -1
               .beat(4, A, 1) // Dei
               .beat(4, A, 1) // ne
               .beat(0, D, 0).beat(0, G, 1).beat(0, B, 1)
               .beat(4, B, 1) // Zau
               .beat(4, G, 1) // ber
               .beat(0, PIANO, D, 0) // Octave -1
               .beat(4, A, 1) // bi
               .beat(2, B, 1) // in
               .beat(2, C, 1) // den
               .beat(0, D, 0).beat(0, G, 1).beat(0, B, 1)
               .beat(4, B, 1) // wie
               .beat(4, G, 1) // der
               .beat(0, PIANO, D, 0) // Octave -1
               .beat(4, A, 1) // was
               .beat(2, B, 1) // die
               .beat(2, C, 1) //
               .beat(0, D, 0).beat(0, G, 1).beat(0, B, 1)
               .beat(4, B, 1) // Mo
               .beat(4, A, 1) // de
               .beat(0, C, SHARP, 0)
               .beat(4, G, 1) // streng
               .beat(4, A, 1) // ge
               .beat(0, D, 0).beat(0, F, 0).beat(0, A, 1)
               .beat(8, D, 0) // teilt
               .beat(0, G, 1).beat(0, B, 1).beat(0, D, 1)
               .beat(4, B, 1) // Al
               .beat(4, B, 1) // le
               .beat(4, C, 1) // Men
               .beat(4, D, 1) // schen
               .beat(0, F, 0).beat(0, A, 1).beat(0, D, 1)
               .beat(4, D, 1) // wer
               .beat(4, C, 1) // den
               .beat(4, B, 1) // Brü
               .beat(4, A, 1) // der
               .beat(0, G, 1).beat(0, B, 1).beat(0, D, 1)
               .beat(4, G, 1) // wo
               .beat(4, G, 1) // dei
               .beat(4, A, 1) // zar
               .beat(4, B, 1) // ter
               .beat(0, F, 0).beat(0, A, 1).beat(0, D, 1)
               .beat(6, A, 1) // Flü
               .beat(2, G, 1) // gel
               .beat(0, G, 1).beat(0, B, 1).beat(0, D, 1)
               .beat(8, G, 1)), // weilt

    FRERE_JACQUES(PIANO, 100L,
                  Map.of(B, FLAT),
                  b -> b
                  .beat(0, GUITAR, F, 1)
                  .beat(4, F, 0)
                  .beat(4, G, 1)
                  .beat(4, A, 1)
                  .beat(4, F, 0)
                  .beat(0, GUITAR, F, 1)
                  .beat(4, F, 0)
                  .beat(4, G, 1)
                  .beat(4, A, 1)
                  .beat(4, F, 0)
                  .beat(0, GUITAR, F, 1)
                  .beat(4, A, 1)
                  .beat(4, B, 1)
                  .beat(8, C, 1)
                  .beat(0, GUITAR, F, 1)
                  .beat(4, A, 1)
                  .beat(4, B, 1)
                  .beat(8, C, 1)
                  .beat(0, GUITAR, C, 1)
                  .beat(2, C, 1)
                  .beat(2, D, 1)
                  .beat(2, C, 1)
                  .beat(2, B, 1)
                  .beat(0, GUITAR, F, 1)
                  .beat(4, A, 1)
                  .beat(4, F, 0)
                  .beat(0, GUITAR, C, 1)
                  .beat(2, C, 1)
                  .beat(2, D, 1)
                  .beat(2, C, 1)
                  .beat(2, B, 1)
                  .beat(0, GUITAR, F, 1)
                  .beat(4, A, 1)
                  .beat(4, F, 0)
                  .beat(0, GUITAR, C, 1)
                  .beat(4, G, 1)
                  .beat(4, C, 0)
                  .beat(0, GUITAR, F, 1)
                  .beat(8, F, 0)
                  .beat(0, GUITAR, C, 1)
                  .beat(4, G, 1)
                  .beat(4, C, 0)
                  .beat(0, GUITAR, F, 1)
                  .beat(8, F, 0)),

    EINE_KLEINE_NACHTMUSIK(BIT, 100L,
                           Map.of(F, SHARP),
                           b -> b
                           .beat(4, G, 1)
                           .pause(2)
                           .beat(2, D, 0)
                           .beat(4, G, 1)
                           .pause(2)
                           .beat(2, D, 0)
                           .beat(2, G, 1)
                           .beat(2, D, 0)
                           .beat(2, G, 1)
                           .beat(2, B, 1)
                           .beat(4, D, 1)
                           .pause(4)
                           .beat(4, C, 1)
                           .pause(2)
                           .beat(2, A, 1)
                           .beat(4, C, 1)
                           .pause(2)
                           .beat(2, A, 1)
                           .beat(2, C, 1)
                           .beat(2, A, 1)
                           .beat(2, F, 0)
                           .beat(2, A, 1)
                           .beat(4, D, 0)
                           .pause(4)),

    FAIRY_FOUNTAIN(CHIME, 100L,
                   Map.of(B, FLAT),
                   b -> b
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
                   .beat(8, G, 1)),

    DECKED_OUT(DIDGERIDOO, 100L,
               Map.of(),
               b -> b
               .beat(3, C, 0)
               .beat(3, B, 0)
               .beat(3, G, FLAT, 0));

    public final Instrument instrument;
    public final long speed;
    public final Map<Tone, Semitone> keys;
    public final Melody melody;

    Music(final Instrument instrument, final long speed, final Map<Tone, Semitone> keys, final Consumer<Melody.Builder> build) {
        this.instrument = instrument;
        this.speed = speed;
        this.keys = keys;
        Melody.Builder builder = Melody.builder(instrument, speed).keys(keys);
        build.accept(builder);
        this.melody = builder.build();
    }
}
