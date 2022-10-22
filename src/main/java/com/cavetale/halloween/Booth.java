package com.cavetale.halloween;

import com.cavetale.halloween.attraction.Attraction;
import com.cavetale.halloween.attraction.AttractionType;
import com.cavetale.halloween.attraction.Festival;
import com.cavetale.mytems.Mytems;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

/**
 * A "booth" is the static, plugin provided part of the Attraction
 * Configuration.
 */
public interface Booth {
    AttractionType getType();

    Component getDisplayName();

    Component getDescription();

    Mytems getReward();

    void apply(Attraction attraction);

    Component format(String txt);

    Festival getFestival();

    ItemStack getFirstCompletionReward();

    List<List<ItemStack>> getPrizePool();
}
