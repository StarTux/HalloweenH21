package com.cavetale.halloween;

import com.cavetale.halloween.attraction.Attraction;
import com.cavetale.halloween.attraction.AttractionType;
import com.cavetale.halloween.attraction.FindSpidersAttraction;
import java.time.Duration;
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
    // Music Hero
    MOUNTAIN_HERO(AttractionType.MUSIC_HERO,
                  null, Component.text("Thanks for visiting me up here."
                                       + " Want to learn a new melody?"),
                  null, null),
    GLASS_PALADIN_HERO(AttractionType.MUSIC_HERO,
                       null, Component.text("Nothing like a nice tune to"
                                            + " brighten your spirits."
                                            + " Care to learn it?"),
                       null, null),
    MOTH_DEN_MUSIC_HERO(AttractionType.MUSIC_HERO,
                        null, Component.text("I always play this melody for our esteemed guests!"
                                             + " Care to learn it?"),
                        null, null),
    LOVE_CRYME_MUSIC(AttractionType.MUSIC_HERO,
                     null, null,
                     null, null),
    FLARE_DANCER_MUSIC(AttractionType.MUSIC_HERO,
                     null, null,
                     null, null),

    // Repeat Melody
    COFFEE_SONG(AttractionType.REPEAT_MELODY, null, null, null, null),
    DRACO_WOLF_SONG(AttractionType.REPEAT_MELODY,
                    null, null,
                    null, null),
    HAWK_MELODY(AttractionType.REPEAT_MELODY,
                null, null,
                null, null),
    ARCANTOS_MELODY(AttractionType.REPEAT_MELODY,
                    null, null,
                    null, null),
    CRITTER_MELODY(AttractionType.REPEAT_MELODY,
                    null, null,
                    null, null),
    FAST_PUPPY_MELODY(AttractionType.REPEAT_MELODY,
                      null, null,
                      null, null),
    YYONGI_MELODY(AttractionType.REPEAT_MELODY,
                      null, null,
                      null, null),
    ABYSMAL_VOID_MELODY(AttractionType.REPEAT_MELODY,
                        null, null,
                        null, null),

    // Shoot Target
    SHOOTING_PYRAMID(AttractionType.SHOOT_TARGET,
                     Component.text("Pyramid Shooting Gallery", NamedTextColor.DARK_RED),
                     Component.text("Targets will appear on each level of this pyramid."
                                    + " Use the Televator to hit them all with an arrow."
                                    + " Don't forget about the ghasts!"),
                     null, null),
    VERANDA_TARGETS(AttractionType.SHOOT_TARGET,
                    null, Component.text("Go on my backyard veranda and shoot"
                                         + " all the targets with a bow and arrow!"),
                    null, null),
    VIXEN_SHOOTING(AttractionType.SHOOT_TARGET,
                   null, null,
                   null, null),
    CANADIAN_JELLY_SHOOTING(AttractionType.SHOOT_TARGET,
                            null, null,
                            null, null),
    LIVV_SHOOTING(AttractionType.SHOOT_TARGET,
                  null, Component.text("Shoot the targets that appear around"
                                       + " the circle over there"
                                       + " with a bow and arrow!"),
                  null, null),
    KOONTZY_SHOOTING(AttractionType.SHOOT_TARGET,
                     null, Component.text("Shoot the targets that appear in the frontyard"
                                          + " with a bow and arrow!"),
                     null, null),
    KITSUNE_SHOOTING(AttractionType.SHOOT_TARGET,
                     null, Component.text("Ghasts and target blocks spawn in these"
                                          + " halls. Shoot them all with your"
                                          + " bow and arrow!"),
                     null, null),
    // Find Spiders
    SPIDER_MANSION(AttractionType.FIND_SPIDERS,
                   null, null,
                   null, null),
    BLACKOUT_SPIDER_HOUSE(AttractionType.FIND_SPIDERS,
                          null, null,
                          null, null),
    COOKY_SPIDER_HUNT(AttractionType.FIND_SPIDERS,
                      null, null,
                      null, null),
    PEARLESQUE_SPIDERS(AttractionType.FIND_SPIDERS,
                       null, null,
                       null, null),
    ADIS_SPIDER_HOUSE(AttractionType.FIND_SPIDERS,
                      null, null,
                      null, a -> {
                          ((FindSpidersAttraction) a).setSearchTime(Duration.ofSeconds(80));
                      }),
    NOOOMYZ_SPIDER_HOUSE(AttractionType.FIND_SPIDERS,
                         null, null,
                         null, null),
    DMS_SPIDER_MANSION(AttractionType.FIND_SPIDERS,
                       null, null,
                       null, null),

    // Find Blocks
    GHOST_TOWER(AttractionType.FIND_BLOCKS,
                null, null,
                null, null),
    HIDDEN_BLOCKS_TIX(AttractionType.FIND_BLOCKS,
                      null, null,
                      null, null),
    ARNOLD_HAUNTED_BLOCKS(AttractionType.FIND_BLOCKS,
                          null, null,
                          null, null),
    PYRO_GHOST_BLOCKS(AttractionType.FIND_BLOCKS,
                      null, null,
                      null, null),

    // Open Chest
    DRAGON_TOWER(AttractionType.OPEN_CHEST,
                 null, null,
                 null, null),
    CHESTS_ON_HILLS(AttractionType.OPEN_CHEST,
                    null, null,
                    null, null),
    TOAST_CHESTS(AttractionType.OPEN_CHEST,
                 null, null,
                 null, null),
    HENDRIKS_CHESTS(AttractionType.OPEN_CHEST,
                    null, null,
                    null, null),
    BRINDLE_MANSION_CHESTS(AttractionType.OPEN_CHEST,
                           null, null,
                           null, null),

    // Race
    POOL_RACE(AttractionType.RACE,
              null, Component.text("Race me, once aroune the house,"
                                   + " counter clockwise!"
                                   + " We meet back here, haha!"),
              null, null),
    MELANTHIA_GRAVEYARD_RACE(AttractionType.RACE,
                             Component.text("Graveyard Race", NamedTextColor.DARK_RED),
                             Component.text("Let's race once around the graveyard,"
                                            + " counter clockwise,"
                                            + " then back here!"),
                             null, null),
    TECHNOLOGY_TENT_RACE(AttractionType.RACE,
                         null, Component.text("Race me once around the tent,"
                                              + " counter clockwise!"),
                         null, null);
    ;

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
