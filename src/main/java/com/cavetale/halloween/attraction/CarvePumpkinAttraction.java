package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.font.VanillaItems;
import com.cavetale.core.struct.Vec3i;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class CarvePumpkinAttraction extends Attraction<CarvePumpkinAttraction.SaveTag> {
    protected static final Duration GAME_TIME = Duration.ofSeconds(90);
    protected static final int MAX_BLOCKS = 12;
    protected final Set<Vec3i> blockSet = new HashSet<>();
    protected int secondsLeft;
    protected int ticks;

    protected CarvePumpkinAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        for (Area area : allAreas) {
            if ("pumpkin".equals(area.name)) {
                blockSet.addAll(area.enumerate());
            }
        }
        this.displayName = booth.format("Pumpkin Carving");
        this.description = text("It's pumpkin season. Let's turn them all into Jack o Lanterns!");
    }

    @Override
    public boolean isPlaying() {
        return saveTag.state != State.IDLE;
    }

    @Override
    protected void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        placeBlocks();
        startingGun(player);
        changeState(State.CARVE);
        player.sendActionBar(text("Carve all the pumpkins", GOLD));
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

    private void placeBlocks() {
        clearBlocks();
        List<Vec3i> blockList = new ArrayList<>(blockSet);
        blockList.removeIf(v -> !v.toBlock(world).getType().isEmpty());
        Collections.shuffle(blockList, random);
        final int max = Math.min(blockList.size(), MAX_BLOCKS);
        for (int i = 0; i < max; i += 1) {
            Vec3i vector = blockList.get(i);
            saveTag.blockList.add(vector);
            saveTag.materialList.add(Material.PUMPKIN);
        }
        saveTag.total = saveTag.blockList.size();
        updateBlocks();
    }

    private static final List<BlockFace> FACES = List.of(BlockFace.NORTH,
                                                         BlockFace.EAST,
                                                         BlockFace.SOUTH,
                                                         BlockFace.WEST);

    private void updateBlock(int index) {
        final Vec3i vector = saveTag.blockList.get(index);
        final Material material = saveTag.materialList.get(index);
        BlockData blockData = material.createBlockData();
        if (blockData instanceof Directional directional) {
            BlockFace face = FACES.get((ticks / 4) % FACES.size());
            directional.setFacing(face);
        }
        Block block = vector.toBlock(world);
        if (!block.getBlockData().equals(blockData)) {
            block.setBlockData(blockData, false);
        }
    }

    private void updateBlocks() {
        for (int i = 0; i < saveTag.blockList.size(); i += 1) {
            updateBlock(i);
        }
    }

    private void clearBlocks() {
        for (Vec3i vector : saveTag.blockList) {
            vector.toBlock(world).setType(Material.AIR);
        }
        saveTag.blockList.clear();
        saveTag.materialList.clear();
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!isPlaying()) return;
        if (!event.hasBlock()) return;
        Player player = getCurrentPlayer();
        if (player == null || !player.equals(event.getPlayer())) return;
        final Block block = event.getClickedBlock();
        final Vec3i vector = Vec3i.of(block);
        final int index = saveTag.blockList.indexOf(vector);
        if (index < 0) return;
        final Material material = saveTag.materialList.get(index);
        if (saveTag.state == State.LIGHT) {
            Material to = Material.JACK_O_LANTERN;
            if (material == to) return;
            saveTag.materialList.set(index, to);
            world.playSound(block.getLocation(), Sound.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1.0f, 1.0f);
        } else {
            Material to = Material.CARVED_PUMPKIN;
            if (material == to) return;
            saveTag.materialList.set(index, to);
            world.playSound(block.getLocation(), Sound.BLOCK_PUMPKIN_CARVE, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
        updateBlock(index);
        event.setCancelled(true);
        confetti(block.getRelative(event.getBlockFace()).getLocation().add(0.5, 0.5, 0.5));
        saveTag.score += 1;
        if (saveTag.score >= saveTag.total) {
            if (saveTag.state == State.LIGHT) {
                victory(player);
                prepareReward(player, true);
                plugin.sessionOf(player).setCooldown(this, completionCooldown);
                changeState(State.IDLE);
            } else {
                player.sendActionBar(text("Light all the carved pumpkins", GOLD));
                progress(player);
                changeState(State.LIGHT);
            }
        } else {
            progress(player);
        }
    }

    protected State tickGame() {
        Player player = getCurrentPlayer();
        if (player == null) return State.IDLE;
        long now = System.currentTimeMillis();
        long searchTime = now - saveTag.gameStarted;
        if (searchTime > GAME_TIME.toMillis()) {
            timeout(player);
            return State.IDLE;
        }
        secondsLeft = (int) ((GAME_TIME.toMillis() - searchTime - 1) / 1000L + 1L);
        updateBlocks();
        ticks += 1;
        return null;
    }

    protected void changeState(State newState) {
        State oldState = saveTag.state;
        saveTag.state = newState;
        oldState.exit(this);
        newState.enter(this);
    }

    enum State {
        IDLE {
            @Override protected void enter(CarvePumpkinAttraction instance) {
                instance.clearBlocks();
            }
        },
        CARVE {
            @Override protected State tick(CarvePumpkinAttraction instance) {
                return instance.tickGame();
            }
            @Override protected void enter(CarvePumpkinAttraction instance) {
                instance.ticks = 0;
                instance.saveTag.gameStarted = System.currentTimeMillis();
                instance.saveTag.score = 0;
            }
        },
        LIGHT {
            @Override protected State tick(CarvePumpkinAttraction instance) {
                return instance.tickGame();
            }
            @Override protected void enter(CarvePumpkinAttraction instance) {
                instance.saveTag.score = 0;
            }
        },
        ;

        protected void enter(CarvePumpkinAttraction instance) { }

        protected void exit(CarvePumpkinAttraction instance) { }

        protected State tick(CarvePumpkinAttraction instance) {
            return null;
        }
    }

    protected static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected List<Vec3i> blockList = new ArrayList<>();
        protected List<Material> materialList = new ArrayList<>();
        protected long gameStarted;
        protected int score;
        protected int total;
    }

    @Override
    public void onPlayerHud(PlayerHudEvent event) {
        VanillaItems icon = saveTag.state == State.LIGHT
            ? VanillaItems.JACK_O_LANTERN
            : VanillaItems.CARVED_PUMPKIN;
        event.bossbar(PlayerHudPriority.HIGHEST,
                      makeProgressComponent(secondsLeft, icon, saveTag.score, saveTag.total),
                      BossBar.Color.RED, BossBar.Overlay.PROGRESS,
                      (float) secondsLeft / (float) GAME_TIME.toSeconds());
    }
}
