package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.item.PlayerReceiveItemsEvent;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.font.DefaultFont;
import com.cavetale.core.font.GuiOverlay;
import com.cavetale.core.font.Unicode;
import com.cavetale.core.item.ItemKinds;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.core.util.Json;
import com.cavetale.halloween.Booth;
import com.cavetale.halloween.HalloweenPlugin;
import com.cavetale.halloween.Music;
import com.cavetale.halloween.Session;
import com.cavetale.halloween.util.Gui;
import com.cavetale.mytems.Mytems;
import com.cavetale.mytems.MytemsCategory;
import com.cavetale.resident.PluginSpawn;
import com.cavetale.resident.ZoneType;
import com.cavetale.resident.save.Loc;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Note;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import static com.cavetale.halloween.HalloweenPlugin.plugin;
import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

/**
 * Base class for all attractions.
 * @param <T> the save tag class
 */
@Getter
public abstract class Attraction<T extends Attraction.SaveTag> {
    protected final HalloweenPlugin plugin;
    protected final Festival festival;
    protected final World world;
    protected final String name;
    protected final List<Area> allAreas;
    protected final File saveFile;
    protected final Cuboid mainArea;
    protected final Class<T> saveTagClass;
    protected PluginSpawn mainVillager;
    protected final Random random = ThreadLocalRandom.current();
    protected T saveTag;
    protected Supplier<T> saveTagSupplier;
    protected Vec3i npcVector;
    protected boolean doesRequireInstrument;
    protected Duration completionCooldown = Duration.ofMinutes(10);
    protected Component displayName = empty();
    protected Component description = empty();
    protected final Booth booth;

    public static Attraction of(final Festival festival,
                                @NonNull final String name,
                                @NonNull final List<Area> areaList,
                                @NonNull final Booth booth) {
        if (areaList.isEmpty()) throw new IllegalArgumentException(name + ": area list is empty");
        if (areaList.get(0).name == null) throw new IllegalArgumentException(name + ": first area has no name!");
        String typeName = areaList.get(0).name;
        AttractionType attractionType = booth.getType() != null
            ? booth.getType()
            : AttractionType.forName(typeName);
        if (attractionType == null) return null;
        Attraction result =  attractionType.make(new AttractionConfiguration(festival, name, areaList, booth));
        if (booth.getDisplayName() != null) result.displayName = booth.getDisplayName();
        if (booth.getDescription() != null) result.description = booth.getDescription();
        //if (booth.getReward() != null) result.firstCompletionReward = booth.getReward().createItemStack();
        booth.apply(result);
        return result;
    }

    protected Attraction(final AttractionConfiguration config, final Class<T> saveTagClass, final Supplier<T> saveTagSupplier) {
        this.plugin = plugin();
        this.festival = config.festival;
        this.world = festival.getWorld();
        this.name = config.name;
        this.allAreas = config.areaList;
        this.saveFile = new File(festival.getSaveFolder(), name + ".json");
        this.mainArea = allAreas.get(0).toCuboid();
        this.saveTagClass = saveTagClass;
        this.saveTagSupplier = saveTagSupplier;
        for (Area area : allAreas) {
            if ("npc".equals(area.name)) {
                npcVector = area.min;
            }
        }
        if (npcVector != null) {
            Location location = npcVector.toCenterFloorLocation(world);
            mainVillager = PluginSpawn.register(plugin, ZoneType.HALLOWEEN, Loc.of(location));
            mainVillager.setOnPlayerClick(this::clickMainVillager);
            mainVillager.setOnMobSpawning(mob -> {
                    mob.setCollidable(false);
                });
        }
        this.booth = config.booth;
    }

    public final boolean isInArea(Location location) {
        return world.equals(location.getWorld()) && mainArea.contains(location);
    }

    public final void load() {
        saveTag = Json.load(saveFile, saveTagClass, saveTagSupplier);
        onLoad();
    }

    public final void save() {
        onSave();
        if (saveTag != null) {
            Json.save(saveFile, saveTag, true);
        }
    }

    public final void enable() {
        onEnable();
    }

    public final void disable() {
        if (mainVillager != null) {
            mainVillager.unregister();
            mainVillager = null;
        }
        onDisable();
    }

    public final void tick() {
        onTick();
    }

    protected final void startingGun(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 2.0f);
    }

    protected final void timeout(Player player) {
        player.showTitle(Title.title(text("Timeout", DARK_RED),
                                     text("Try Again", DARK_RED)));
        Music.DECKED_OUT.melody.play(plugin, player);
        plugin.sessionOf(player).setCooldown(this, Duration.ofSeconds(10));
    }

    protected final void fail(Player player) {
        player.showTitle(Title.title(text("Wrong", DARK_RED),
                                     text("Try Again", DARK_RED)));
        Music.DECKED_OUT.melody.play(plugin, player);
        plugin.sessionOf(player).setCooldown(this, Duration.ofSeconds(10));
    }

    protected final void progress(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.3f, 2.0f);
    }

    protected final void victory(Player player) {
        Component message = text("Complete", GOLD);
        player.showTitle(Title.title(message, text("Good Job!", GOLD)));
        player.sendMessage(message);
        Music.TREASURE.melody.play(HalloweenPlugin.getInstance(), player);
    }

    public static final void perfect(Player player, boolean withMusic) {
        Component message = join(noSeparators(),
                                 text("P", GOLD),
                                 text("E", RED),
                                 text("R", YELLOW),
                                 text("F", GOLD),
                                 text("E", RED),
                                 text("C", YELLOW),
                                 text("T", GOLD),
                                 text("!", DARK_RED, BOLD));
        player.showTitle(Title.title(message, empty()));
        player.sendMessage(message);
        if (withMusic) {
            Music.TREASURE.melody.play(HalloweenPlugin.getInstance(), player);
        }
    }

    protected final void perfect(Player player) {
        perfect(player, true);
    }

    protected final void countdown(Player player, int seconds) {
        player.sendActionBar(text(seconds, GOLD));
        List<Note.Tone> tones = List.of(Note.Tone.D, Note.Tone.A, Note.Tone.G);
        if ((int) seconds <= tones.size()) {
            player.playNote(player.getLocation(), Instrument.PLING, Note.natural(0, tones.get((int) seconds - 1)));
        }
    }

    protected final Component makeProgressComponent(int seconds, ComponentLike prefix, int has, int max) {
        return join(noSeparators(),
                    text(Unicode.WATCH.string + seconds, GOLD),
                    space(),
                    prefix,
                    text(has + "/" + max, DARK_RED));
    }

    protected final Component makeProgressComponent(int seconds) {
        return join(noSeparators(),
                    text(Unicode.WATCH.string + seconds, GOLD),
                    space());
    }

    /**
     * Override me to tell if this attraction is currently playing!
     */
    public abstract boolean isPlaying();

    public final Player getCurrentPlayer() {
        Player player = saveTag.currentPlayer != null
            ? Bukkit.getPlayer(saveTag.currentPlayer)
            : null;
        if (player == null) return null;
        if (!isInArea(player.getLocation())) {
            plugin.sessionOf(player).setCooldown(this, Duration.ofMinutes(1));
            stop();
            return null;
        }
        return player;
    }

    public final boolean isCurrentPlayer(Player player) {
        return isPlaying() && player.getUniqueId().equals(saveTag.currentPlayer);
    }

    protected abstract void onTick();

    protected void onEnable() { }

    protected void onDisable() { }

    protected void onLoad() { }

    protected void onSave() { }

    public void onEntityDamage(EntityDamageEvent event) { }

    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) { }

    public void onPlayerInteract(PlayerInteractEvent event) { }

    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) { }

    public final void onPlayerQuit(PlayerQuitEvent event) {
        if (isPlaying() && event.getPlayer().equals(getCurrentPlayer())) {
            plugin.sessionOf(event.getPlayer()).setCooldown(this, Duration.ofMinutes(1));
            stop();
        }
    }

    protected final ItemStack getRegularCompletionReward(Player player) {
        List<List<ItemStack>> list = booth.getPrizePool();
        List<ItemStack> list2 = list.get(random.nextInt(list.size()));
        ItemStack item = list2.get(random.nextInt(list2.size()));
        return item.clone();
    }

    /**
     * Give unique prize in a chest and update the session.
     */
    protected final void giveFirstCompletionReward(Player player) {
        Session session = plugin.sessionOf(player);
        session.clearPrizeWaiting(this);
        session.lockUnique(this);
        session.save();
        giveInGui(player, booth.getFirstCompletionReward().clone(),
                  booth.getEntryFee().clone(),
                  new ItemStack(Material.DIAMOND),
                  new ItemStack(Material.EMERALD),
                  new ItemStack(Material.EMERALD));
    }

    /**
     * Give regular prize in a chest and update the session.
     */
    protected final void giveRegularCompletionReward(Player player) {
        Session session = plugin.sessionOf(player);
        session.clearPrizeWaiting(this);
        session.save();
        giveInGui(player, getRegularCompletionReward(player));
    }

    /**
     * Prepare unique prize for when they next click the NPC.
     * Also update the session.
     */
    protected final void prepareFirstCompletionReward(Player player) {
        Session session = plugin.sessionOf(player);
        session.setFirstCompletionPrizeWaiting(this);
        session.lockUnique(this);
        session.save();
    }

    /**
     * Prepare regular prize for when they next click the NPC.
     * Also update the session.
     */
    protected final void prepareRegularCompletionReward(Player player) {
        Session session = plugin.sessionOf(player);
        session.setRegularCompletionPrizeWaiting(this);
        session.save();
    }

    /**
     * Give the appropriate reward and update the session.
     */
    protected final void giveReward(Player player, boolean appliesForFirstCompletion) {
        Session session = plugin.sessionOf(player);
        if (appliesForFirstCompletion && !session.isUniqueLocked(this)) {
            giveFirstCompletionReward(player);
        } else {
            giveRegularCompletionReward(player);
        }
    }

    /**
     * Prepare the appropriate reward for when they next click the
     * NPC, and update the session.
     */
    protected final void prepareReward(Player player, boolean appliesForFirstCompletion) {
        Session session = plugin.sessionOf(player);
        if (appliesForFirstCompletion && !session.isUniqueLocked(this)) {
            prepareFirstCompletionReward(player);
        } else {
            prepareRegularCompletionReward(player);
        }
    }

    protected final void giveInGui(Player player, ItemStack prize, ItemStack... extras) {
        final int size = 27;
        Component title = GuiOverlay.BLANK.builder(size, DARK_RED).title(displayName).build();
        Gui gui = new Gui(plugin);
        gui.size(size);
        gui.setItem(13, prize);
        int[] indexes = {
            22, 4, 12, 14,
        };
        for (int i = 0; i < extras.length; i += 1) {
            if (i >= indexes.length) {
                throw new IllegalStateException("Index exceeded: " + i + "/" + indexes.length);
            }
            gui.setItem(indexes[i], extras[i]);
        }
        gui.setEditable(true);
        gui.title(title);
        gui.onClose(evt -> {
                PlayerReceiveItemsEvent.receiveInventory(player, gui.getInventory());
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, SoundCategory.BLOCKS, 1.0f, 1.0f);
            });
        gui.open(player);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }

    protected final boolean checkCooldown(Player player) {
        if (player.hasPermission("halloween.nocooldown")) return true;
        Session session = plugin.sessionOf(player);
        Duration cooldown = session.getCooldown(this);
        if (cooldown != null) {
            long minutes = cooldown.toMinutes();
            long seconds = cooldown.toSeconds() % 60L;
            Component message = text("Give others a chance and wait "
                                               + minutes + "m "
                                               + seconds + "s",
                                               RED);
            player.sendMessage(message);
            player.sendActionBar(message);
            return false;
        }
        return true;
    }

    protected final boolean checkSomebodyPlaying(Player player) {
        if (!isPlaying()) return true;
        Player somebody = getCurrentPlayer();
        if (player.equals(somebody)) return false; // fail silently
        Component somebodyName = somebody != null
            ? somebody.displayName()
            : text("Somebody");
        Component message = join(noSeparators(),
                                 text("Please wait: "),
                                 somebodyName,
                                 text(" is playing this right now"))
            .color(RED);
        player.sendMessage(message);
        player.sendActionBar(message);
        return false;
    }

    protected final boolean checkInstrument(Player player) {
        for (ItemStack itemStack : player.getInventory()) {
            Mytems mytems = Mytems.forItem(itemStack);
            if (mytems != null && mytems.category == MytemsCategory.MUSIC) {
                return true;
            }
        }
        Component message = join(noSeparators(),
                                 text("You don't have a "),
                                 Mytems.ANGELIC_HARP.component,
                                 text("musical instrument!"))
            .color(RED);
        player.sendMessage(message);
        player.sendActionBar(message);
        return false;
    }

    protected final boolean takeEntryFee(Player player) {
        ItemStack entryFee = booth.getEntryFee();
        for (ItemStack itemStack : player.getInventory()) {
            if (entryFee.isSimilar(itemStack)) {
                itemStack.subtract(1);
                return true;
            }
        }
        Component message = join(noSeparators(),
                                 text("You don't have "),
                                 ItemKinds.chatDescription(booth.getEntryFee()),
                                 text("!"))
            .color(RED);
        player.sendMessage(message);
        player.sendActionBar(message);
        return false;
    }

    /**
     * Override me when a player starts the game!
     */
    protected abstract void start(Player player);

    protected abstract void stop();

    protected final void clickMainVillager(Player player) {
        Session session = plugin.sessionOf(player);
        int prizeWaiting = session.getPrizeWaiting(this);
        if (prizeWaiting > 0) {
            session.clearPrizeWaiting(this);
            session.save();
            if (prizeWaiting == 2) {
                giveFirstCompletionReward(player);
            } else {
                giveRegularCompletionReward(player);
            }
            return;
        }
        if (!checkCooldown(player)) return;
        if (!checkSomebodyPlaying(player)) return;
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.editMeta(m -> {
                BookMeta meta = (BookMeta) m;
                Component page = join(noSeparators(),
                                      displayName,
                                      newline(),
                                      description,
                                      (doesRequireInstrument
                                       ? join(noSeparators(),
                                              newline(),
                                              Mytems.ANGELIC_HARP.component,
                                              text("Musical Instrument Required", RED))
                                       : empty()),
                                      newline(),
                                      (!session.isUniqueLocked(this)
                                       ? text(Unicode.CHECKBOX.character + " Not yet finished", DARK_GRAY)
                                       : text(Unicode.CHECKED_CHECKBOX.character + " Finished", BLUE)),
                                      newline(),
                                      newline(),
                                      text("Play game for "),
                                      ItemKinds.chatDescription(booth.getEntryFee()),
                                      text("?"),
                                      newline(),
                                      (DefaultFont.START_BUTTON.component
                                       .clickEvent(ClickEvent.runCommand("/hallow yes " + name))
                                       .hoverEvent(HoverEvent.showText(text("Play this Game", GREEN)))),
                                      space(),
                                      (DefaultFont.CANCEL_BUTTON.component
                                       .clickEvent(ClickEvent.runCommand("/hallow no " + name))
                                       .hoverEvent(HoverEvent.showText(text("Goodbye!", RED)))));
                meta.setAuthor("Cavetale");
                meta.title(text("Halloween"));
                meta.pages(List.of(page));
            });
        player.openBook(book);
    }

    public final void onClickYes(Player player) {
        if (!checkCooldown(player)) return;
        if (!checkSomebodyPlaying(player)) return;
        if (doesRequireInstrument && !checkInstrument(player)) return;
        if (!takeEntryFee(player)) return;
        start(player);
    }

    public void onPluginPlayer(PluginPlayerEvent event) { }

    protected abstract static class SaveTag {
        protected UUID currentPlayer = null;
    }

    protected final void subtitle(Player player, Component component) {
        player.showTitle(Title.title(empty(), component));
    }

    protected final void confetti(Player player, Location location) {
        player.spawnParticle(Particle.SPELL_MOB, location, 16, 0.25, 0.25, 0.25, 1.0);
    }

    protected final void confetti(Location location) {
        location.getWorld().spawnParticle(Particle.SPELL_MOB, location, 16, 0.25, 0.25, 0.25, 1.0);
    }

    protected final void highlight(Player player, Location location) {
        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.WHITE, 4.0f);
        player.spawnParticle(Particle.REDSTONE, location, 4, 1.0, 1.0, 1.0, 1.0, dustOptions);
    }

    public final List<Player> getPlayersIn(Cuboid area) {
        List<Player> result = new ArrayList<>();
        for (Player player : world.getPlayers()) {
            if (area.contains(player.getLocation())) {
                result.add(player);
            }
        }
        return result;
    }

    public final String getUniqueKey() {
        return festival.getWorldName() + "." + name;
    }

    /**
     * Print to the hud.
     */
    public void onPlayerHud(PlayerHudEvent event) { }
}
