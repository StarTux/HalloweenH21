package com.cavetale.halloween;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
            .senderCaller(this::reload);
        rootNode.addChild("music").arguments("<melody> [player]")
            .description("Play music")
            .completers(CommandArgCompleter.enumLowerList(Music.class),
                        CommandArgCompleter.NULL)
            .senderCaller(this::music);
    }

    private int requireInt(String arg) {
        try {
            return Integer.parseInt(arg);
        } catch (NumberFormatException nfe) {
            throw new CommandWarn("Number expected: " + arg);
        }
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


    protected boolean music(CommandSender sender, String[] args) {
        if (args.length > 3) return false;
        Music music;
        Player target;
        try {
            music = Music.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException iae) {
            throw new CommandWarn("Music not found: " + args[0]);
        }
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                throw new CommandWarn("Player not found: " + args[1]);
            }
        } else {
            if (!(sender instanceof Player)) {
                throw new CommandWarn("[fam:music] Player expected!");
            }
            target = (Player) sender;
        }
        music.melody.play(target);
        sender.sendMessage(Component.text("Playing " + music + " to " + target.getName(), NamedTextColor.YELLOW));
        return true;
    }
}
