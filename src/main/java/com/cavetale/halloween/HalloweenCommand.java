package com.cavetale.halloween;

import com.cavetale.core.command.AbstractCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public final class HalloweenCommand extends AbstractCommand<HalloweenPlugin> {
    protected HalloweenCommand(final HalloweenPlugin plugin) {
        super(plugin, "halloween");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("info").denyTabCompletion()
            .description("Info Command")
            .senderCaller(this::info);
        rootNode.addChild("reload").denyTabCompletion()
            .description("Reload areas")
            .senderCaller(this::info);
    }

    protected boolean info(CommandSender sender, String[] args) {
        return false;
    }

    protected boolean reload(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        plugin.clearAttractions();
        plugin.clearSessions();
        plugin.loadAttractions();
        sender.sendMessage(Component.text("Players and attractions reloaded",
                                          NamedTextColor.YELLOW));
        return true;
    }
}
