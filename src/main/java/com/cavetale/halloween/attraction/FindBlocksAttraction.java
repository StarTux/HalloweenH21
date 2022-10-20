package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.font.Unicode;
import com.cavetale.core.font.VanillaItems;
import com.cavetale.core.struct.Vec3i;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.type.Candle;
import org.bukkit.block.data.type.Chain;
import org.bukkit.block.data.type.Lantern;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public final class FindBlocksAttraction extends Attraction<FindBlocksAttraction.SaveTag> {
    protected static final List<BlockFace> HORIZONTAL_FACES = List.of(BlockFace.NORTH,
                                                                      BlockFace.EAST,
                                                                      BlockFace.SOUTH,
                                                                      BlockFace.WEST);
    protected static final Duration SEARCH_TIME = Duration.ofSeconds(120);
    protected static final int MAX_BLOCKS = 10;
    Set<Vec3i> originBlockSet = new HashSet<>();
    protected int secondsLeft;

    protected FindBlocksAttraction(final AttractionConfiguration config) {
        super(config, SaveTag.class, SaveTag::new);
        for (Area area : allAreas) {
            if ("blocks".equals(area.name)) {
                originBlockSet.addAll(area.enumerate());
            }
        }
        this.displayName = Component.text("Hidden Blocks", NamedTextColor.DARK_RED);
        this.description = Component.text("My place is haunted!"
                                          + " a ghost keeps placing and rearranging blocks."
                                          + " Can you find them all?");
    }

    @Override
    public boolean isPlaying() {
        return saveTag.state != State.IDLE;
    }

    @Override
    protected void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        startingGun(player);
        changeState(State.SEARCH);
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
        if (saveTag.state != State.SEARCH) return;
        if (!event.hasBlock()) return;
        Player player = getCurrentPlayer();
        if (player == null || !player.equals(event.getPlayer())) return;
        int index = saveTag.blockList.indexOf(Vec3i.of(event.getClickedBlock()));
        if (index < 0) return;
        event.setCancelled(true);
        Block block = resetBlock(index);
        confetti(player, block.getLocation()
                 .add(0.5, 0.5, 0.5)
                 .add(event.getBlockFace().getDirection().multiply(0.5)));
        saveTag.blocksFound += 1;
        if (saveTag.blocksFound < saveTag.totalBlocks) {
            progress(player);
        }
    }

    protected static boolean isCeilingBlock(Block block) {
        if (block.isSolid() && block.getType().isOccluding()) return true;
        BlockData bd = block.getBlockData();
        if (bd instanceof Slab) {
            switch (((Slab) bd).getType()) {
            case DOUBLE: case BOTTOM: return true;
            default: return false;
            }
        }
        if (bd instanceof Stairs) {
            switch (((Stairs) bd).getHalf()) {
            case BOTTOM: return true;
            default: return false;
            }
        }
        return false;
    }

    protected static boolean isFloorBlock(Block block) {
        if (block.isSolid() && block.getType().isOccluding()) return true;
        BlockData bd = block.getBlockData();
        if (bd instanceof Slab) {
            switch (((Slab) bd).getType()) {
            case DOUBLE: case TOP: return true;
            default: return false;
            }
        }
        if (bd instanceof Stairs) {
            switch (((Stairs) bd).getHalf()) {
            case TOP: return true;
            default: return false;
            }
        }
        return false;
    }

    protected void onStartSearch() {
        saveTag.blockList = new ArrayList<>();
        saveTag.blockDataList = new ArrayList<>();
        List<Vec3i> possibleBlocks = new ArrayList<>(originBlockSet);
        Collections.shuffle(possibleBlocks, random);
        World w = world;
        for (Vec3i vec : possibleBlocks) {
            if (saveTag.blockList.size() >= MAX_BLOCKS) break;
            Block block = vec.toBlock(w);
            List<List<BlockData>> blockDataList = new ArrayList<>();
            if (block.isEmpty()) {
                if (isFloorBlock(block.getRelative(BlockFace.DOWN))) {
                    blockDataList.add(List.of(new BlockData[] {
                                Material.LANTERN.createBlockData(bd -> {
                                        ((Lantern) bd).setHanging(false);
                                    }),
                                Material.SOUL_LANTERN.createBlockData(bd -> {
                                        ((Lantern) bd).setHanging(false);
                                    }),
                            }));
                    List<BlockData> candleList = new ArrayList<>();
                    for (int candleCount : List.of(1, 2, 3, 4)) {
                        for (Material mat : Tag.CANDLES.getValues()) {
                            candleList.add(mat.createBlockData(bd -> {
                                        ((Candle) bd).setLit(true);
                                        ((Candle) bd).setCandles(candleCount);
                                    }));
                        }
                    }
                    blockDataList.add(candleList);
                    blockDataList.add(List.of(Material.BREWING_STAND.createBlockData(),
                                              Material.END_ROD.createBlockData(bd -> {
                                                      ((Directional) bd).setFacing(BlockFace.DOWN);
                                                  }),
                                              Material.ENCHANTING_TABLE.createBlockData(),
                                              Material.CAKE.createBlockData(),
                                              Material.STONECUTTER.createBlockData(),
                                              Material.CAULDRON.createBlockData(),
                                              Material.LAVA_CAULDRON.createBlockData(),
                                              Material.POWDER_SNOW_CAULDRON.createBlockData(bd -> {
                                                      ((Levelled) bd).setLevel(((Levelled) bd).getMaximumLevel());
                                                  }),
                                              Material.WATER_CAULDRON.createBlockData(bd -> {
                                                      ((Levelled) bd).setLevel(((Levelled) bd).getMaximumLevel());
                                                  })));
                    List<BlockData> flowerPotList = new ArrayList<>();
                    for (Material mat : Tag.FLOWER_POTS.getValues()) {
                        flowerPotList.add(mat.createBlockData());
                    }
                    blockDataList.add(flowerPotList);
                    List<BlockData> amethystList = new ArrayList<>();
                    for (Material mat : List.of(Material.LARGE_AMETHYST_BUD,
                                                Material.MEDIUM_AMETHYST_BUD,
                                                Material.SMALL_AMETHYST_BUD)) {
                        amethystList.add(mat.createBlockData(bd -> {
                                    ((Directional) bd).setFacing(BlockFace.UP);
                                }));
                    }
                    blockDataList.add(amethystList);
                }
                if (isCeilingBlock(block.getRelative(BlockFace.UP))) {
                    blockDataList.add(List.of(new BlockData[] {
                                Material.LANTERN.createBlockData(bd -> {
                                        ((Lantern) bd).setHanging(true);
                                    }),
                                Material.SOUL_LANTERN.createBlockData(bd -> {
                                        ((Lantern) bd).setHanging(true);
                                    }),
                            }));
                    blockDataList.add(List.of(Material.COBWEB.createBlockData(),
                                              Material.END_ROD.createBlockData(bd -> {
                                                      ((Directional) bd).setFacing(BlockFace.DOWN);
                                                  }),
                                              Material.CHAIN.createBlockData(bd -> {
                                                      ((Chain) bd).setAxis(Axis.Y);
                                                  })));
                    List<BlockData> amethystList = new ArrayList<>();
                    for (Material mat : List.of(Material.LARGE_AMETHYST_BUD,
                                                Material.MEDIUM_AMETHYST_BUD,
                                                Material.SMALL_AMETHYST_BUD)) {
                        amethystList.add(mat.createBlockData(bd -> {
                                    ((Directional) bd).setFacing(BlockFace.DOWN);
                                }));
                    }
                    blockDataList.add(amethystList);
                }
            } else if (block.isSolid() && block.getType().isOccluding()) {
                for (BlockFace facing : HORIZONTAL_FACES) {
                    Block nbor = block.getRelative(facing);
                    if (nbor.isEmpty() && nbor.getLightFromBlocks() > 0) {
                        if (block.getType() != Material.SHROOMLIGHT) {
                            blockDataList.add(List.of(Material.SHROOMLIGHT.createBlockData()));
                        }
                        if (block.getType() != Material.JACK_O_LANTERN && block.getType() != Material.CARVED_PUMPKIN) {
                            blockDataList.add(List.of(new BlockData[] {
                                        Material.JACK_O_LANTERN.createBlockData(bd -> {
                                                ((Directional) bd).setFacing(facing);
                                            }),
                                        Material.CARVED_PUMPKIN.createBlockData(bd -> {
                                                ((Directional) bd).setFacing(facing);
                                            }),
                                    }));
                        }
                    }
                }
            } else {
                continue;
            }
            if (blockDataList.isEmpty()) continue;
            List<BlockData> blockDataList2 = blockDataList.get(random.nextInt(blockDataList.size()));
            BlockData blockData = blockDataList2.get(random.nextInt(blockDataList2.size()));
            saveTag.blockList.add(vec);
            saveTag.blockDataList.add(block.getBlockData().getAsString(false));
            block.setBlockData(blockData, false);
        }
        saveTag.searchStarted = System.currentTimeMillis();
        saveTag.blocksFound = 0;
        saveTag.totalBlocks = saveTag.blockList.size();
    }

    protected void onEndSearch() {
        while (!saveTag.blockList.isEmpty()) {
            resetBlock(saveTag.blockList.size() - 1);
        }
        saveTag.blockList = null;
        saveTag.blockDataList = null;
    }

    protected Block resetBlock(int blockIndex) {
        Vec3i vec = saveTag.blockList.remove(blockIndex);
        String string = saveTag.blockDataList.remove(blockIndex);
        BlockData blockData = Bukkit.createBlockData(string);
        Block block = vec.toBlock(world);
        block.setBlockData(blockData, false);
        return block;
    }

    protected State tickSearch() {
        Player player = getCurrentPlayer();
        if (player == null) return State.IDLE;
        if (saveTag.blockList.isEmpty()) {
            victory(player);
            prepareReward(player, true);
            plugin.sessionOf(player).setCooldown(this, completionCooldown);
            return State.IDLE;
        }
        long now = System.currentTimeMillis();
        long searchTime = now - saveTag.searchStarted;
        if (searchTime > SEARCH_TIME.toMillis()) {
            timeout(player);
            return State.IDLE;
        }
        int seconds = (int) ((SEARCH_TIME.toMillis() - searchTime - 1) / 1000L + 1L);
        if (seconds != secondsLeft) {
            secondsLeft = seconds;
            player.sendActionBar(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                        Component.text(Unicode.WATCH.string + seconds, NamedTextColor.GOLD),
                        Component.space(),
                        VanillaItems.JACK_O_LANTERN.component,
                        Component.text(saveTag.blocksFound + "/" + saveTag.totalBlocks, NamedTextColor.DARK_RED),
                    }));
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
        SEARCH {
            @Override protected void enter(FindBlocksAttraction instance) {
                instance.onStartSearch();
            }

            @Override protected void exit(FindBlocksAttraction instance) {
                instance.onEndSearch();
            }

            @Override protected State tick(FindBlocksAttraction instance) {
                return instance.tickSearch();
            }
        };

        protected void enter(FindBlocksAttraction instance) { }

        protected void exit(FindBlocksAttraction instance) { }

        protected State tick(FindBlocksAttraction instance) {
            return null;
        }
    }

    protected static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected List<Vec3i> blockList;
        protected List<String> blockDataList;
        protected int blocksFound;
        protected int totalBlocks;
        protected long searchStarted;
    }
}
