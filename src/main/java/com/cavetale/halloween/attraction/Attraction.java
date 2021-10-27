package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Cuboid;
import com.cavetale.area.struct.Vec3i;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.font.Unicode;
import com.cavetale.core.font.VanillaItems;
import com.cavetale.core.util.Json;
import com.cavetale.halloween.Booth;
import com.cavetale.halloween.HalloweenPlugin;
import com.cavetale.halloween.Music;
import com.cavetale.halloween.Session;
import com.cavetale.halloween.util.Gui;
import com.cavetale.mytems.Mytems;
import com.cavetale.resident.PluginSpawn;
import com.cavetale.resident.ZoneType;
import com.cavetale.resident.save.Loc;
import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

/**
 * Base class for all attractions.
 * @param <T> the save tag class
 */
@Getter
public abstract class Attraction<T extends Attraction.SaveTag> {
    protected final HalloweenPlugin plugin;
    protected final String name;
    protected final List<Cuboid> allAreas;
    protected final File saveFile;
    protected final Cuboid mainArea;
    protected final Class<T> saveTagClass;
    protected PluginSpawn mainVillager;
    protected final Random random = ThreadLocalRandom.current();
    protected T saveTag;
    protected Supplier<T> saveTagSupplier;
    protected Vec3i npcVector;
    protected Mytems firstCompletionMytems = null;
    protected boolean doesRequireInstrument;
    protected Duration completionCooldown = Duration.ofMinutes(10);
    protected Component displayName = Component.empty();
    protected Component description = Component.empty();
    protected ItemStack firstCompletionReward = Mytems.HALLOWEEN_TOKEN.createItemStack();
    protected static final ItemStack[][] PRIZE_POOL = {
        {
            Mytems.CANDY_CORN.createItemStack(),
            Mytems.CHOCOLATE_BAR.createItemStack(),
            Mytems.LOLLIPOP.createItemStack(),
            Mytems.ORANGE_CANDY.createItemStack(),
        },
        {
            new ItemStack(Material.DIAMOND, 1),
            new ItemStack(Material.DIAMOND, 2),
            new ItemStack(Material.DIAMOND, 4),
            new ItemStack(Material.DIAMOND, 8),
            new ItemStack(Material.DIAMOND, 16),
            new ItemStack(Material.DIAMOND, 32),
            new ItemStack(Material.DIAMOND, 64),
        },
        {
            new ItemStack(Material.EMERALD),
            new ItemStack(Material.COD),
            new ItemStack(Material.POISONOUS_POTATO),
        }
    };

    public static Attraction of(HalloweenPlugin plugin, @NonNull final String name, @NonNull final List<Cuboid> areaList,
                                final Booth booth) {
        if (areaList.isEmpty()) throw new IllegalArgumentException(name + ": area list is empty");
        if (areaList.get(0).name == null) throw new IllegalArgumentException(name + ": first area has no name!");
        String typeName = areaList.get(0).name;
        AttractionType attractionType = booth != null && booth.type != null
            ? booth.type
            : AttractionType.forName(typeName);
        if (attractionType == null) return null;
        Attraction result = makeAttraction(plugin, attractionType, name, areaList, booth);
        if (booth != null) {
            if (booth.displayName != null) result.displayName = booth.displayName;
            if (booth.description != null) result.description = booth.description;
            if (booth.reward != null) result.firstCompletionReward = booth.reward.createItemStack();
            if (booth.consumer != null) booth.consumer.accept(result);
        }
        return result;
    }

    private static Attraction makeAttraction(HalloweenPlugin plugin, AttractionType type, String name, List<Cuboid> areaList, Booth booth) {
        switch (type) {
        case REPEAT_MELODY: return new RepeatMelodyAttraction(plugin, name, areaList, booth);
        case SHOOT_TARGET: return new ShootTargetAttraction(plugin, name, areaList, booth);
        case FIND_SPIDERS: return new FindSpidersAttraction(plugin, name, areaList, booth);
        case OPEN_CHEST: return new OpenChestAttraction(plugin, name, areaList, booth);
        case FIND_BLOCKS: return new FindBlocksAttraction(plugin, name, areaList, booth);
        case RACE: return new RaceAttraction(plugin, name, areaList, booth);
        case MUSIC_HERO: return new MusicHeroAttraction(plugin, name, areaList, booth);
        default:
            throw new IllegalArgumentException(type + ": Not implemented!");
        }
    }

    protected Attraction(final HalloweenPlugin plugin, final String name, final List<Cuboid> areaList, final Booth booth,
                         final Class<T> saveTagClass, final Supplier<T> saveTagSupplier) {
        this.plugin = plugin;
        this.name = name;
        this.allAreas = areaList;
        this.saveFile = new File(plugin.getAttractionsFolder(), name + ".json");
        this.mainArea = areaList.get(0);
        this.saveTagClass = saveTagClass;
        this.saveTagSupplier = saveTagSupplier;
        for (Cuboid area : areaList) {
            if ("npc".equals(area.name)) {
                npcVector = area.min;
            }
        }
        if (npcVector != null) {
            Location location = npcVector.toLocation(plugin.getWorld());
            mainVillager = PluginSpawn.register(plugin, ZoneType.HALLOWEEN, Loc.of(location));
            mainVillager.setOnPlayerClick(this::clickMainVillager);
            mainVillager.setOnMobSpawning(mob -> {
                    mob.setCollidable(false);
                });
        }
    }


    public final boolean isInArea(Location location) {
        return mainArea.contains(location);
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
        player.showTitle(Title.title(Component.text("Timeout", NamedTextColor.DARK_RED),
                                     Component.text("Try Again", NamedTextColor.DARK_RED)));
        Music.DECKED_OUT.melody.play(plugin, player);
        plugin.sessionOf(player).setCooldown(this, Duration.ofSeconds(10));
    }

    protected final void fail(Player player) {
        player.showTitle(Title.title(Component.text("Wrong", NamedTextColor.DARK_RED),
                                     Component.text("Try Again", NamedTextColor.DARK_RED)));
        Music.DECKED_OUT.melody.play(plugin, player);
        plugin.sessionOf(player).setCooldown(this, Duration.ofSeconds(10));
    }

    protected final void progress(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.3f, 2.0f);
    }

    protected final void victory(Player player) {
        Component message = Component.text("Complete", NamedTextColor.GOLD);
        player.showTitle(Title.title(message, Component.text("Good Job!", NamedTextColor.GOLD)));
        player.sendMessage(message);
        Music.TREASURE.melody.play(HalloweenPlugin.getInstance(), player);
        Music.TREASURE2.melody.play(HalloweenPlugin.getInstance(), player);
    }

    public static final void perfect(Player player, boolean withMusic) {
        Component message = Component.join(JoinConfiguration.noSeparators(), new Component[] {
                Component.text("P", NamedTextColor.GOLD),
                Component.text("E", NamedTextColor.RED),
                Component.text("R", NamedTextColor.YELLOW),
                Component.text("F", NamedTextColor.GOLD),
                Component.text("E", NamedTextColor.RED),
                Component.text("C", NamedTextColor.YELLOW),
                Component.text("T", NamedTextColor.GOLD),
                Component.text("!", NamedTextColor.DARK_RED, TextDecoration.BOLD),
            });
        player.showTitle(Title.title(message, Component.empty()));
        player.sendMessage(message);
        if (withMusic) {
            Music.TREASURE.melody.play(HalloweenPlugin.getInstance(), player);
            Music.TREASURE2.melody.play(HalloweenPlugin.getInstance(), player);
        }
    }

    protected final void perfect(Player player) {
        perfect(player, true);
    }

    protected final void countdown(Player player, int seconds) {
        player.sendActionBar(Component.text(seconds, NamedTextColor.GOLD));
        List<Note.Tone> tones = List.of(Note.Tone.D, Note.Tone.A, Note.Tone.G);
        if ((int) seconds <= tones.size()) {
            player.playNote(player.getLocation(), Instrument.PLING, Note.natural(0, tones.get((int) seconds - 1)));
        }
    }

    protected final Component makeProgressComponent(int seconds, Component prefix, int has, int max) {
        return Component.join(JoinConfiguration.noSeparators(), new Component[] {
                Component.text(Unicode.WATCH.string + seconds, NamedTextColor.GOLD),
                Component.space(),
                prefix,
                Component.text(has + "/" + max, NamedTextColor.DARK_RED),
            });
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
        ItemStack[] list = PRIZE_POOL[random.nextInt(PRIZE_POOL.length)];
        ItemStack itemStack = list[random.nextInt(list.length)];
        return itemStack.clone();
    }

    /**
     * Give unique prize in a chest and update the session.
     */
    protected final void giveFirstCompletionReward(Player player) {
        Session session = plugin.sessionOf(player);
        session.clearPrizeWaiting(this);
        session.lockUnique(this);
        session.save();
        giveInGui(player, firstCompletionReward.clone());
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

    protected final void giveInGui(Player player, ItemStack prize) {
        Gui gui = new Gui(plugin);
        gui.setItem(13, prize);
        gui.setEditable(true);
        gui.title(displayName);
        gui.onClose(evt -> {
                for (ItemStack item : gui.getInventory()) {
                    if (item == null || item.getType() == Material.AIR) continue;
                    for (ItemStack drop : player.getInventory().addItem(item).values()) {
                        player.getWorld().dropItem(player.getEyeLocation(), drop);
                    }
                }
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
            Component message = Component.text("Give others a chance and wait "
                                               + minutes + "m "
                                               + seconds + "s",
                                               NamedTextColor.RED);
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
            : Component.text("Somebody");
        Component message = Component.join(JoinConfiguration.noSeparators(), new Component[] {
                Component.text("Please wait: "),
                somebodyName,
                Component.text(" is playing this right now"),
            }).color(NamedTextColor.RED);
        player.sendMessage(message);
        player.sendActionBar(message);
        return false;
    }

    protected final boolean checkInstrument(Player player) {
        for (ItemStack itemStack : player.getInventory()) {
            Mytems mytems = Mytems.forItem(itemStack);
            if (mytems != null && mytems.category == Mytems.Category.MUSIC) {
                return true;
            }
        }
        Component message = Component.join(JoinConfiguration.noSeparators(), new Component[] {
                Component.text("You don't have a "),
                Mytems.ANGELIC_HARP.component,
                Component.text("musical instrument!"),
            }).color(NamedTextColor.RED);
        player.sendMessage(message);
        player.sendActionBar(message);
        return false;
    }

    protected final boolean takeEntryFee(Player player) {
        ItemStack entryFee = new ItemStack(Material.DIAMOND);
        for (ItemStack itemStack : player.getInventory()) {
            if (entryFee.isSimilar(itemStack)) {
                itemStack.subtract(1);
                return true;
            }
        }
        Component message = Component.join(JoinConfiguration.noSeparators(), new Component[] {
                Component.text("You don't have a "),
                VanillaItems.DIAMOND.component,
                Component.text("diamond!"),
            }).color(NamedTextColor.RED);
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
                Component page = Component.join(JoinConfiguration.noSeparators(), new Component[] {
                        displayName,
                        Component.newline(),
                        description,
                        (doesRequireInstrument
                         ? Component.join(JoinConfiguration.noSeparators(), new Component[] {
                                 Component.newline(),
                                 Mytems.ANGELIC_HARP.component,
                                 Component.text("Musical Instrument Required", NamedTextColor.RED)
                             })
                         : Component.empty()),
                        Component.newline(),
                        (!session.isUniqueLocked(this)
                         ? Component.text(Unicode.CHECKBOX.character + " Not yet finished", NamedTextColor.DARK_GRAY)
                         : Component.text(Unicode.CHECKED_CHECKBOX.character + " Finished", NamedTextColor.BLUE)),
                        Component.newline(),
                        Component.newline(),
                        Component.text("Play game for 1"),
                        VanillaItems.DIAMOND.component,
                        Component.text("Diamond?"),
                        Component.newline(),
                        (Component.text("[Yes]", NamedTextColor.BLUE)
                         .clickEvent(ClickEvent.runCommand("/hallow yes " + name))
                         .hoverEvent(HoverEvent.showText(Component.text("Play this Game", NamedTextColor.GREEN)))),
                        Component.space(),
                        (Component.text("[No]", NamedTextColor.DARK_RED)
                         .clickEvent(ClickEvent.runCommand("/hallow no " + name))
                         .hoverEvent(HoverEvent.showText(Component.text("Goodbye!", NamedTextColor.RED)))),
                    });
                meta.setAuthor("Cavetale");
                meta.title(displayName);
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
        player.showTitle(Title.title(Component.empty(), component));
    }

    protected final void confetti(Player player, Location location) {
        player.spawnParticle(Particle.SPELL_MOB, location, 16, 0.25, 0.25, 0.25, 1.0);
    }

    protected final void highlight(Player player, Location location) {
        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.WHITE, 4.0f);
        player.spawnParticle(Particle.REDSTONE, location, 4, 1.0, 1.0, 1.0, 1.0, dustOptions);
    }
}
