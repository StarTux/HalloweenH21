package com.cavetale.halloween;

import com.cavetale.halloween.attraction.Attraction;
import com.cavetale.halloween.attraction.AttractionType;
import com.cavetale.halloween.attraction.Festival;
import com.cavetale.mytems.Mytems;
import net.kyori.adventure.text.Component;
import static net.kyori.adventure.text.Component.text;

public final class DefaultBooth implements Booth {
    @Override
    public AttractionType getType() {
        return null;
    }

    @Override
    public Component getDisplayName() {
        return null;
    }

    @Override
    public Component getDescription() {
        return null;
    }

    @Override
    public Mytems getReward() {
        return null;
    }

    @Override
    public void apply(Attraction attraction) { }

    @Override
    public Component format(String txt) {
        return text(txt);
    }

    @Override
    public Festival getFestival() {
        return null;
    }
}
