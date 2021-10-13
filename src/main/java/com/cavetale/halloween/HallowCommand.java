package com.cavetale.halloween;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.halloween.attraction.Attraction;
import org.bukkit.entity.Player;

public final class HallowCommand extends AbstractCommand<HalloweenPlugin> {
    protected HallowCommand(final HalloweenPlugin plugin) {
        super(plugin, "hallow");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("yes").hidden(true).denyTabCompletion()
            .description("Say yes")
            .playerCaller(this::yes);
        rootNode.addChild("no").hidden(true).denyTabCompletion()
            .description("Say no")
            .playerCaller(this::no);
    }

    protected boolean yes(Player player, String[] args) {
        if (args.length != 1) return true;
        Attraction attraction = plugin.attractionsMap.get(args[0]);
        if (attraction == null) return true;
        attraction.onClickYes(player);
        return true;
    }

    protected boolean no(Player player, String[] args) {
        return true;
    }
}
