package com.cavetale.halloween;

import com.cavetale.halloween.attraction.Attraction;
import com.cavetale.halloween.attraction.AttractionType;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;

/**
 * Static information for Attractions.
 */
public enum Booth {
    COFFEE_SONG(AttractionType.REPEAT_MELODY, null, null, null, null),
    SHOOTING_PYRAMID(AttractionType.SHOOT_TARGET,
                     Component.text("Pyramid Shooting Gallery", NamedTextColor.DARK_RED),
                     Component.text("Targets will appear on each level of this pyramid."
                                    + " Use the Televator to hit them all with an arrow."
                                    + " Don't forget about the ghasts!"),
                     null, null);

    public final String name; // Corresponds with area.name
    public final AttractionType type;
    public final Component displayName;
    public final Component description;
    public final ItemStack reward;
    public final Consumer<Attraction> consumer;

    Booth(final AttractionType type,
          final Component displayName,
          final Component description,
          final ItemStack reward,
          final Consumer<Attraction> consumer) {
        this.name = Stream.of(name().split("_"))
            .map(s -> s.substring(0, 1) + s.substring(1).toLowerCase())
            .collect(Collectors.joining(""));
        this.type = type;
        this.displayName = displayName;
        this.description = description;
        this.reward = reward;
        this.consumer = consumer;
    }

    public static Booth forName(String n) {
        for (Booth booth : Booth.values()) {
            if (n.equals(booth.name)) return booth;
        }
        return null;
    }
}
