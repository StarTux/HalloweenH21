package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Cuboid;
import com.cavetale.area.struct.Vec3i;
import com.cavetale.core.font.Unicode;
import com.cavetale.halloween.Booth;
import com.cavetale.halloween.HalloweenPlugin;
import com.cavetale.halloween.Session;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public final class OpenChestAttraction extends Attraction<OpenChestAttraction.SaveTag> {
    protected static final Duration OPEN_TIME = Duration.ofSeconds(30);
    Set<Vec3i> chestBlockSet = new HashSet<>();
    protected int secondsLeft;

    protected OpenChestAttraction(final HalloweenPlugin plugin, final String name, final List<Cuboid> areaList, final Booth booth) {
        super(plugin, name, areaList, booth, SaveTag.class, SaveTag::new);
        for (Cuboid cuboid : areaList) {
            if ("chest".equals(cuboid.name)) {
                chestBlockSet.addAll(cuboid.enumerate());
            }
        }
        this.displayName = Component.text("Chest Game", NamedTextColor.DARK_RED);
        this.description = Component.text("Choose one of my chests and keep"
                                          + " what you find inside!");
    }

    @Override
    public boolean isPlaying() {
        return saveTag.state != State.IDLE;
    }

    @Override
    public void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        placeChests(player);
        startingGun(player);
        changeState(State.OPEN);
    }

    @Override
    protected void stop() {
        changeState(State.IDLE);
    }

    @Override
    public void onTick() {
        State newState = saveTag.state.tick(this);
        if (newState != null) changeState(newState);
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (saveTag.state != State.OPEN) return;
        switch (event.getAction()) {
        case RIGHT_CLICK_BLOCK:
        case LEFT_CLICK_BLOCK:
            break;
        default: return;
        }
        Player player = getCurrentPlayer();
        if (player == null) return;
        if (!event.getPlayer().equals(player)) return;
        Vec3i clickedBlock = Vec3i.of(event.getClickedBlock());
        for (Vec3i chestBlock : chestBlockSet) {
            if (clickedBlock.equals(chestBlock)) {
                event.setCancelled(true);
                openChest(player, event.getClickedBlock());
                return;
            }
        }
    }

    public void openChest(Player player, Block block) {
        Location location = block.getLocation().add(0.5, 0.5, 0.5);
        confetti(player, location);
        progress(player);
        double roll = random.nextDouble();
        boolean bingo = roll < 0.15;
        giveReward(player, bingo);
        Session session = plugin.sessionOf(player);
        if (bingo) {
            session.setCooldown(this, completionCooldown);
        } else {
            session.setCooldown(this, (session.isUniqueLocked(this)
                                       ? completionCooldown
                                       : Duration.ofSeconds(30)));
        }
        changeState(State.IDLE);
    }

    private void placeChests(Player player) {
        Vec3i playerVector = Vec3i.of(player.getLocation());
        for (Vec3i vec : chestBlockSet) {
            Chest blockData = (Chest) Material.CHEST.createBlockData();
            blockData.setFacing(npcVector.subtract(vec).horizontalBlockFace());
            vec.toBlock(plugin.getWorld()).setBlockData(blockData);
        }
    }

    private void clearChests() {
        for (Vec3i vec : chestBlockSet) {
            vec.toBlock(plugin.getWorld()).setType(Material.AIR);
        }
    }

    protected State tickOpen() {
        Player player = getCurrentPlayer();
        if (player == null) return State.IDLE;
        long now = System.currentTimeMillis();
        long timeout = saveTag.openStarted + OPEN_TIME.toMillis();
        if (now > timeout) {
            timeout(player);
            return State.IDLE;
        }
        int seconds = (int) ((timeout - now - 1L) / 1000L) + 1;
        if (seconds != secondsLeft) {
            secondsLeft = seconds;
            player.sendActionBar(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                        Component.text(Unicode.WATCH.string + seconds, NamedTextColor.GOLD),
                        Component.text(" Pick a chest!", NamedTextColor.WHITE),
                    }));
            for (Vec3i vec : chestBlockSet) {
                highlight(player, vec.toLocation(player.getWorld()).add(0, 0.5, 0));
            }
        }
        return null;
    }

    protected void changeState(State newState) {
        State oldState = saveTag.state;
        saveTag.state = newState;
        oldState.exit(this);
        newState.enter(this);
    }

    enum State {
        IDLE,
        OPEN {
            @Override protected State tick(OpenChestAttraction instance) {
                return instance.tickOpen();
            }

            @Override protected void enter(OpenChestAttraction instance) {
                instance.saveTag.openStarted = System.currentTimeMillis();
            }

            @Override protected void exit(OpenChestAttraction instance) {
                instance.clearChests();
            }
        };

        protected void enter(OpenChestAttraction instance) { }

        protected void exit(OpenChestAttraction instance) { }

        protected State tick(OpenChestAttraction instance) {
            return null;
        }
    }

    protected static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected long openStarted;
    }
}
