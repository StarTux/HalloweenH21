package com.cavetale.halloween.attraction;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum AttractionType {
    REPEAT_MELODY(RepeatMelodyAttraction.class, RepeatMelodyAttraction::new),
    SHOOT_TARGET(ShootTargetAttraction.class, ShootTargetAttraction::new),
    FIND_SPIDERS(FindSpidersAttraction.class, FindSpidersAttraction::new),
    OPEN_CHEST(OpenChestAttraction.class, OpenChestAttraction::new),
    FIND_BLOCKS(FindBlocksAttraction.class, FindBlocksAttraction::new),
    RACE(RaceAttraction.class, RaceAttraction::new),
    MUSIC_HERO(MusicHeroAttraction.class, MusicHeroAttraction::new),
    POSTER(PosterAttraction.class, PosterAttraction::new),
    SNOWBALL_FIGHT(SnowballFightAttraction.class, SnowballFightAttraction::new),
    MEMORY(MemoryAttraction.class, MemoryAttraction::new),
    ;

    public final Class<? extends Attraction> type;
    private final AttractionConstructor ctor;

    public static AttractionType forName(String name) {
        for (AttractionType it : AttractionType.values()) {
            if (name.toUpperCase().equals(it.name())) return it;
        }
        return null;
    }

    public static AttractionType of(Attraction attraction) {
        for (AttractionType it : AttractionType.values()) {
            if (it.type.isInstance(attraction)) return it;
        }
        return null;
    }

    @FunctionalInterface
    public interface AttractionConstructor {
        Attraction make(AttractionConfiguration config);
    }

    public Attraction make(AttractionConfiguration config) {
        return ctor.make(config);
    }
}
