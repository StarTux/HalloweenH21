package com.cavetale.halloween;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.halloween.music.Melodies;
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
        rootNode.addChild("melody").arguments("<melody> [speed] [player]")
            .description("Play melody")
            .completers(CommandArgCompleter.enumLowerList(Melodies.class),
                        CommandArgCompleter.integer(i -> i > 0),
                        CommandArgCompleter.NULL)
            .senderCaller(this::melody);
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


    protected boolean melody(CommandSender sender, String[] args) {
        if (args.length > 3) return false;
        Melodies melodies;
        int speed;
        Player target;
        try {
            melodies = Melodies.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException iae) {
            throw new CommandWarn("Melody not found: " + args[0]);
        }
        if (args.length >= 2) {
            speed = requireInt(args[1]);
            if (speed < 1) throw new CommandWarn("Speed must be positive: " + speed);
        } else {
            speed = 2;
        }
        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                throw new CommandWarn("Player not found: " + args[2]);
            }
        } else {
            if (!(sender instanceof Player)) {
                throw new CommandWarn("[fam:melody] Player expected!");
            }
            target = (Player) sender;
        }
        melodies.melody.play(target, speed, () -> {
                sender.sendMessage(Component.text("Done playing!", NamedTextColor.YELLOW));
            });
        sender.sendMessage(Component.text("Playing " + melodies + " to " + target.getName(), NamedTextColor.YELLOW));
        return true;
    }
}
