package com.cavetale.halloween;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.playercache.PlayerCache;
import com.cavetale.halloween.attraction.Attraction;
import com.cavetale.halloween.attraction.AttractionType;
import com.cavetale.halloween.attraction.Festival;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

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
        rootNode.addChild("count").arguments("<world>")
            .description("Count attractions")
            .completers(CommandArgCompleter.supplyList(() -> List.copyOf(plugin.festivalMap.keySet())))
            .senderCaller(this::count);
        CommandNode sessionNode = rootNode.addChild("session")
            .description("Session subcommands");
        sessionNode.addChild("reset").arguments("<player>")
            .description("Reset player session")
            .completers(CommandArgCompleter.PLAYER_CACHE)
            .senderCaller(this::sessionReset);
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
        plugin.clearSessions();
        plugin.clearFestivals();
        plugin.loadFestivals();
        sender.sendMessage(text("Players and attractions reloaded", YELLOW));
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
        music.melody.play(plugin, target);
        sender.sendMessage(text("Playing " + music + " to " + target.getName(), YELLOW));
        return true;
    }

    protected boolean count(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        Festival festival = plugin.festivalMap.get(args[0]);
        if (festival == null) throw new CommandWarn("World not found: " + args[0]);
        Map<AttractionType, Integer> counts = new EnumMap<>(AttractionType.class);
        for (AttractionType type : AttractionType.values()) counts.put(type, 0);
        for (Attraction attraction : festival.getAttractionsMap().values()) {
            AttractionType type = AttractionType.of(attraction);
            counts.put(type, counts.get(type) + 1);
        }
        List<AttractionType> rankings = new ArrayList<>(List.of(AttractionType.values()));
        Collections.sort(rankings, (a, b) -> Integer.compare(counts.get(a), counts.get(b)));
        for (AttractionType type : rankings) {
            sender.sendMessage(counts.get(type) + " " + type);
        }
        sender.sendMessage(festival.getAttractionsMap().size() + " Total");
        return true;
    }

    private boolean sessionReset(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        PlayerCache player = PlayerCache.require(args[0]);
        Session session = plugin.sessionOf(player);
        session.reset();
        session.save();
        plugin.clearSession(player.uuid);
        return true;
    }
}
