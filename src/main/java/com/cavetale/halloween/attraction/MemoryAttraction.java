package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.core.struct.Vec3i;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import static net.kyori.adventure.text.Component.text;

public final class MemoryAttraction extends Attraction<MemoryAttraction.SaveTag> {
    protected static final int REVEAL_TICKS = 30;
    protected Duration playTime;
    protected List<Vec3i> memoryBlocks = new ArrayList<>();
    protected int secondsLeft;

    protected MemoryAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        for (Area cuboid : allAreas) {
            if ("block".equals(cuboid.name)) {
                memoryBlocks.addAll(cuboid.enumerate());
            }
        }
        this.displayName = booth.format("Memory Game");
        this.description = text("Find matching pairs of blocks. Reveal 2 blocks at a time.");
        this.playTime = Duration.ofSeconds(memoryBlocks.size() * 5);
    }

    @Override
    public boolean isPlaying() {
        return saveTag.state != State.IDLE;
    }

    @Override
    public void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        generateBlocks();
        saveTag.gameStarted = System.currentTimeMillis();
        startingGun(player);
        changeState(State.PICK_1);
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
        if (!isPlaying()) return;
        switch (event.getAction()) {
        case RIGHT_CLICK_BLOCK:
        case LEFT_CLICK_BLOCK:
            break;
        default: return;
        }
        Player player = getCurrentPlayer();
        if (player == null) return;
        if (!event.getPlayer().equals(player)) return;
        final Block block = event.getClickedBlock();
        final Vec3i clickedBlock = Vec3i.of(event.getClickedBlock());
        final int clickedIndex = memoryBlocks.indexOf(clickedBlock);
        if (clickedIndex < 0) return;
        final Material clickedMaterial = saveTag.hiddenBlocks.get(clickedIndex);
        final boolean clickedAlreadyFound = saveTag.foundBlocks.get(clickedIndex);
        if (clickedAlreadyFound) return;
        if (saveTag.state == State.PICK_1) {
            block.setBlockData(clickedMaterial.createBlockData(), false);
            confetti(player, block.getRelative(event.getBlockFace()).getLocation().add(0.5, 0.5, 0.5));
            saveTag.revealedIndex = clickedIndex;
            changeState(State.PICK_2);
        } else if (saveTag.state == State.PICK_2) {
            if (saveTag.revealedIndex == clickedIndex) return;
            block.setBlockData(clickedMaterial.createBlockData(), false);
            confetti(player, block.getRelative(event.getBlockFace()).getLocation().add(0.5, 0.5, 0.5));
            if (clickedMaterial == saveTag.hiddenBlocks.get(saveTag.revealedIndex)) {
                saveTag.foundBlocks.set(clickedIndex, true);
                saveTag.foundBlocks.set(saveTag.revealedIndex, true);
                progress(player);
                saveTag.complete = true;
                for (boolean v : saveTag.foundBlocks) {
                    if (!v) {
                        saveTag.complete = false;
                        break;
                    }
                }
                if (saveTag.complete) {
                    perfect(player);
                    prepareReward(player, true);
                    plugin.sessionOf(player).setCooldown(this, completionCooldown);
                } else {
                    changeState(State.PICK_1);
                }
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_SNARE, SoundCategory.MASTER, 1.0f, 0.5f);
            }
            changeState(State.REVEAL);
        }
    }

    public void revealBlock(Player player, Block block) {
        changeState(State.IDLE);
    }

    private final List<Material> hiddenMaterials = List
        .of(Material.PUMPKIN,
            Material.MELON,
            Material.DIAMOND_ORE,
            Material.GOLD_ORE,
            Material.IRON_ORE,
            Material.COPPER_ORE,
            Material.GOLD_BLOCK,
            Material.EMERALD_BLOCK,
            Material.REDSTONE_BLOCK,
            Material.DIAMOND_BLOCK);

    private void generateBlocks() {
        List<Material> hidden = new ArrayList<>();
        hidden.addAll(hiddenMaterials);
        Collections.shuffle(hidden);
        saveTag.hiddenBlocks.clear();
        saveTag.foundBlocks.clear();
        for (int i = 0; i < memoryBlocks.size() / 2; i += 1) {
            Material mat = hidden.get(i % hidden.size());
            saveTag.hiddenBlocks.add(mat);
            saveTag.hiddenBlocks.add(mat);
            saveTag.foundBlocks.add(false);
            saveTag.foundBlocks.add(false);
        }
        Collections.shuffle(saveTag.hiddenBlocks);
        saveTag.originalBlockData.clear();
        for (Vec3i vector : memoryBlocks) {
            Block block = vector.toBlock(world);
            saveTag.originalBlockData.add(block.getBlockData().getAsString());
        }
    }

    private void hideBlocks() {
        for (int i = 0; i < memoryBlocks.size(); i += 1) {
            Vec3i vector = memoryBlocks.get(i);
            boolean found = saveTag.foundBlocks.get(i);
            Block block = vector.toBlock(world);
            final Material material = found
                ? saveTag.hiddenBlocks.get(i)
                : Material.SMOOTH_STONE;
            block.setBlockData(material.createBlockData(), false);
        }
    }

    private void clearBlocks() {
        for (int i = 0; i < saveTag.originalBlockData.size(); i += 1) {
            Vec3i vector = memoryBlocks.get(i);
            Block block = vector.toBlock(world);
            BlockData original = Bukkit.createBlockData(saveTag.originalBlockData.get(i));
            block.setBlockData(original, false);
        }
        saveTag.hiddenBlocks.clear();
        saveTag.foundBlocks.clear();
        saveTag.originalBlockData.clear();
        saveTag.revealedIndex = -1;
        saveTag.revealTicks = 0;
        saveTag.complete = false;
    }

    protected State tickGame() {
        final Player player = getCurrentPlayer();
        if (player == null) return State.IDLE;
        final long now = System.currentTimeMillis();
        final long timeout = saveTag.gameStarted + playTime.toMillis();
        if (!saveTag.complete) {
            if (now > timeout) {
                timeout(player);
                return State.IDLE;
            }
        }
        secondsLeft = (int) ((timeout - now - 1L) / 1000L) + 1;
        if (saveTag.state == State.REVEAL) {
            saveTag.revealTicks += 1;
            if (saveTag.revealTicks >= REVEAL_TICKS) {
                return saveTag.complete
                    ? State.IDLE
                    : State.PICK_1;
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
        IDLE {
            @Override protected void enter(MemoryAttraction instance) {
                instance.clearBlocks();
                instance.saveTag.tries = 0;
            }
        },
        PICK_1 {
            @Override protected State tick(MemoryAttraction instance) {
                return instance.tickGame();
            }
            @Override protected void enter(MemoryAttraction instance) {
                instance.saveTag.complete = false;
                instance.saveTag.revealedIndex = -1;
                instance.hideBlocks();
            }
        },
        PICK_2 {
            @Override protected State tick(MemoryAttraction instance) {
                return instance.tickGame();
            }
        },
        REVEAL {
            @Override protected State tick(MemoryAttraction instance) {
                return instance.tickGame();
            }
            @Override protected void enter(MemoryAttraction instance) {
                instance.saveTag.revealTicks = 0;
                instance.saveTag.tries += 1;
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
        protected List<Material> hiddenBlocks = new ArrayList<>();
        protected List<Boolean> foundBlocks = new ArrayList<>();
        protected List<String> originalBlockData = new ArrayList<>();
        protected int revealedIndex = -1;
        protected int revealTicks;
        protected boolean complete;
        protected int tries;
    }

    @Override
    public void onPlayerHud(PlayerHudEvent event) {
        event.bossbar(PlayerHudPriority.HIGHEST,
                      makeProgressComponent(secondsLeft),
                      BossBar.Color.RED, BossBar.Overlay.PROGRESS,
                      (float) secondsLeft / (float) playTime.toSeconds());
    }
}
