package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.font.Unicode;
import com.cavetale.core.struct.Vec3i;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class MemoryAttraction extends Attraction<MemoryAttraction.SaveTag> {
    protected static final Duration PLAY_TIME = Duration.ofSeconds(60);
    protected List<Vec3i> memoryBlocks = new ArrayList<>();
    protected int secondsLeft;

    protected MemoryAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        for (Area cuboid : allAreas) {
            if ("block".equals(cuboid.name)) {
                memoryBlocks.addAll(cuboid.enumerate());
            }
        }
        this.displayName = text("Memory Game", DARK_RED);
        this.description = text("Choose one of my chests and keep"
                                          + " what you find inside!");
    }

    @Override
    public boolean isPlaying() {
        return saveTag.state != State.IDLE;
    }

    @Override
    public void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        placeBlocks();
        saveTag.gameStarted = System.currentTimeMillis();
        startingGun(player);
        changeState(State.GAME);
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
        if (saveTag.state != State.GAME && saveTag.state != State.GAME) return;
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
        for (Vec3i vec : memoryBlocks) {
            if (clickedBlock.equals(vec)) {
                event.setCancelled(true);
                // TODO
                return;
            }
        }
    }

    public void revealBlock(Player player, Block block) {
        changeState(State.IDLE);
    }

    private final List<Material> hiddenMaterials = List.of(Material.PUMPKIN,
                                                           Material.CARVED_PUMPKIN,
                                                           Material.JACK_O_LANTERN);

    private void placeBlocks() {
        int count = 0;
        for (Vec3i vec : memoryBlocks) {
            vec.toBlock(world).setType(Material.SMOOTH_STONE);
        }
        saveTag.hiddenBlocks = new ArrayList<>(memoryBlocks.size());
        for (int i = 0; i < memoryBlocks.size(); i += 2) {
            Material mat = hiddenMaterials.get((i / 2) % hiddenMaterials.size());
            saveTag.hiddenBlocks.add(mat);
            saveTag.hiddenBlocks.add(mat);
        }
        Collections.shuffle(saveTag.hiddenBlocks);
    }

    private void clearBlocks() {
        saveTag.hiddenBlocks = null;
    }

    protected State tickGame(boolean second) {
        Player player = getCurrentPlayer();
        if (player == null) return State.IDLE;
        long now = System.currentTimeMillis();
        long timeout = saveTag.gameStarted + PLAY_TIME.toMillis();
        if (now > timeout) {
            timeout(player);
            return State.IDLE;
        }
        int seconds = (int) ((timeout - now - 1L) / 1000L) + 1;
        if (seconds != secondsLeft) {
            secondsLeft = seconds;
            player.sendActionBar(join(noSeparators(),
                                      text(Unicode.WATCH.string + seconds, GOLD),
                                      text(" Pick a chest!", WHITE)));
            for (Vec3i vec : memoryBlocks) {
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
        GAME {
            @Override protected State tick(MemoryAttraction instance) {
                return instance.tickGame(false);
            }
        },
        ;

        protected void enter(MemoryAttraction instance) { }

        protected void exit(MemoryAttraction instance) { }

        protected State tick(MemoryAttraction instance) {
            return null;
        }
    }

    protected static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected long gameStarted;
        protected List<Material> hiddenBlocks;
    }
}
