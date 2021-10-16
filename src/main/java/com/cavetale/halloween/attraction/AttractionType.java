package com.cavetale.halloween.attraction;

public enum AttractionType {
    REPEAT_MELODY,
    SHOOT_TARGET,
    FIND_SPIDERS,
    OPEN_CHEST,
    FIND_BLOCKS,
    RACE,
    MUSIC_HERO;

    public static AttractionType forName(String name) {
        for (AttractionType it : AttractionType.values()) {
            if (name.toUpperCase().equals(it.name())) return it;
        }
        return null;
    }
}
