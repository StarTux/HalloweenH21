package com.cavetale.halloween.attraction;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum AttractionType {
    REPEAT_MELODY(RepeatMelodyAttraction.class),
    SHOOT_TARGET(ShootTargetAttraction.class),
    FIND_SPIDERS(FindSpidersAttraction.class),
    OPEN_CHEST(OpenChestAttraction.class),
    FIND_BLOCKS(FindBlocksAttraction.class),
    RACE(RaceAttraction.class),
    MUSIC_HERO(MusicHeroAttraction.class);

    public final Class<? extends Attraction> type;

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
}
