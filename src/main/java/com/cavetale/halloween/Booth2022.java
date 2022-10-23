package com.cavetale.halloween;

import com.cavetale.halloween.attraction.Attraction;
import com.cavetale.halloween.attraction.AttractionType;
import com.cavetale.halloween.attraction.Festival;
import com.cavetale.mytems.Mytems;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class Booth2022 implements Booth {
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
        TextComponent.Builder builder = text();
        for (int i = 0; i < txt.length(); i += 1) {
            builder.append(text(txt.charAt(i), i % 2 == 0 ? GOLD : RED));
        }
        return builder.build();
    }

    @Override
    public Festival getFestival() {
        return null;
    }

    @Override
    public ItemStack getFirstCompletionReward() {
        return Mytems.HALLOWEEN_TOKEN_2.createItemStack();
    }

    private static final List<List<ItemStack>> PRIZE_POOL =
        List.of(List.of(Mytems.CANDY_CORN.createItemStack(),
                        Mytems.CHOCOLATE_BAR.createItemStack(),
                        Mytems.LOLLIPOP.createItemStack(),
                        Mytems.ORANGE_CANDY.createItemStack()),
                List.of(Mytems.RUBY.createItemStack(1),
                        Mytems.RUBY.createItemStack(3),
                        Mytems.RUBY.createItemStack(5),
                        Mytems.RUBY.createItemStack(7)),
                List.of(new ItemStack(Material.EMERALD),
                        new ItemStack(Material.COD),
                        new ItemStack(Material.DIAMOND),
                        new ItemStack(Material.POISONOUS_POTATO)));

    @Override
    public List<List<ItemStack>> getPrizePool() {
        return PRIZE_POOL;
    }
}
