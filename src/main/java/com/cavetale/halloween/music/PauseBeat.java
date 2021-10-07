package com.cavetale.halloween.music;

import lombok.Value;
import org.bukkit.Location;
import org.bukkit.Note;
import org.bukkit.entity.Player;

@Value
public final class PauseBeat implements Beat {
    protected final int ticks;

    @Override
    public boolean hasSound() {
        return false;
    }

    @Override
    public boolean countsAs(Note other) {
        return false;
    }

    @Override
    public String toDisplayString() {
        return "";
    }

    @Override
    public void play(Player player, Location location) { }

    @Override
    public void play(Location location) { }

    @Override
    public String serialize() {
        return "P" + ticks;
    }
}
