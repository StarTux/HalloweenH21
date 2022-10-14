package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.halloween.Booth;
import com.cavetale.halloween.HalloweenPlugin;
import com.cavetale.poster.PosterPlugin;
import com.cavetale.poster.save.Poster;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public final class PosterAttraction extends Attraction<PosterAttraction.SaveTag> {
    private Poster poster; // set by booth!
    private Vec3i posterBlock;
    private Vec3i posterFace;
    private BlockFace face;
    private BlockFace right; // +x
    private Duration playTime = Duration.ofMinutes(5);
    private long shownTime = -1;

    protected PosterAttraction(final HalloweenPlugin plugin, final String name, final List<Area> areaList, final Booth booth) {
        super(plugin, name, areaList, booth, SaveTag.class, SaveTag::new);
        for (Area area : areaList) {
            if ("block".equals(area.name)) {
                posterBlock = area.min;
            } else if ("face".equals(area.name)) {
                posterFace = area.min;
            }
        }
        if (posterBlock == null || posterFace == null) {
            throw new IllegalStateException("poster=" + posterBlock
                                            + " face=" + posterFace);
        }
        if (posterFace.x > posterBlock.x) {
            face = BlockFace.EAST;
            right = BlockFace.NORTH;
        } else if (posterFace.x < posterBlock.x) {
            face = BlockFace.WEST;
            right = BlockFace.SOUTH;
        } else if (posterFace.z > posterBlock.z) {
            face = BlockFace.SOUTH;
            right = BlockFace.EAST;
        } else if (posterFace.z < posterBlock.z) {
            face = BlockFace.NORTH;
            right = BlockFace.WEST;
        } else {
            throw new IllegalStateException("poster=" + posterBlock
                                            + " face=" + posterFace);
        }
    }

    @Override
    public void start(Player player) {
        if (poster == null) throw new IllegalStateException("poster=null");
        saveTag.currentPlayer = player.getUniqueId();
        startingGun(player);
        changeState(State.PLAY);
    }

    @Override
    public void stop() {
        changeState(State.IDLE);
    }

    @Override
    public boolean isPlaying() {
        return saveTag.state != State.IDLE;
    }

    @Override
    public void onTick() {
        if (saveTag.state == State.IDLE) return;
        Player player = getCurrentPlayer();
        if (player == null) return;
        State newState = saveTag.state.tick(this, player);
        if (newState != null) changeState(newState);
    }

    protected void changeState(State newState) {
        State oldState = saveTag.state;
        saveTag.state = newState;
        oldState.exit(this);
        newState.enter(this);
    }

    protected void rollPoster() {
        saveTag.frames = new ArrayList<>();
        if (poster == null) throw new IllegalStateException("poster=null");
        int i = 0;
        for (int mapId : poster.getMapIds()) {
            Frame frame = new Frame();
            frame.mapId = mapId;
            frame.mapIndex = i++;
            saveTag.frames.add(frame);
        }
        saveTag.frames.get(saveTag.frames.size() - 1).mapId = -1;
        Collections.shuffle(saveTag.frames, random);
    }

    protected void spawnPoster(int index) {
        Frame frame = saveTag.frames.get(index);
        if (frame.mapId <= 0) return;
        if (frame.uuid != null && Bukkit.getEntity(frame.uuid) != null) return;
        int x = index % poster.getWidth();
        int y = index / poster.getWidth();
        Vec3i vec = getPosterBlock(x, y);
        Location location = vec.toLocation(plugin.getWorld()).add(0, 0.5, 0);
        PosterPlugin posterPlugin = (PosterPlugin) Bukkit.getPluginManager().getPlugin("Poster");
        GlowItemFrame entity = location.getWorld().spawn(location, GlowItemFrame.class, e -> {
                e.setPersistent(false);
                e.setFixed(true);
                e.setItem(posterPlugin.createPosterMapItem(frame.mapId));
                e.setFacingDirection(face);
            });
        if (entity != null) {
            frame.uuid = entity.getUniqueId();
        }
    }

    protected Vec3i getPosterBlock(int x, int y) {
        return posterBlock.add(right.getModX() * x,
                               -y,
                               right.getModZ() * x);
    }

    protected void spawnAllPosters() {
        for (int i = 0; i < saveTag.frames.size(); i += 1) {
            spawnPoster(i);
        }
    }

    protected void despawnPoster(int index) {
        Frame frame = saveTag.frames.get(index);
        if (frame.uuid == null) return;
        Entity entity = Bukkit.getEntity(frame.uuid);
        if (entity instanceof GlowItemFrame glowItemFrame) {
            glowItemFrame.remove();
        }
        frame.uuid = null;
    }

    protected void despawnAllPosters() {
        if (saveTag.frames == null) return;
        for (int i = 0; i < saveTag.frames.size(); i += 1) {
            despawnPoster(i);
        }
    }

    protected Frame frameOfEntity(UUID uuid) {
        if (saveTag.frames == null) return null;
        for (Frame frame : saveTag.frames) {
            if (uuid.equals(frame.uuid)) return frame;
        }
        return null;
    }

    protected Frame findEmptyFrame() {
        for (Frame frame : saveTag.frames) {
            if (frame.mapId < 0) return frame;
        }
        throw new IllegalStateException("Empty frame not found!");
    }

    protected static class Frame {
        protected int mapId;
        protected int mapIndex; // index within poster
        protected UUID uuid;
    }

    protected State tickPlay(Player player) {
        Duration timeSpent = Duration.ofMillis(System.currentTimeMillis() - saveTag.playStarted);
        Duration timeLeft = playTime.minus(timeSpent);
        if (timeLeft.isNegative()) {
            timeout(player);
            return State.IDLE;
        }
        long secondsLeft = (timeLeft.toMillis() - 1) / 1000L + 1L;
        if (secondsLeft != shownTime) {
            shownTime = secondsLeft;
            player.sendActionBar(makeProgressComponent((int) secondsLeft));
        }
        return null;
    }

    @Override
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        onPlayerUseEntity(event.getPlayer(), event.getRightClicked());
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            onPlayerUseEntity(player, event.getEntity());
        }
    }

    protected void onPlayerUseEntity(Player player, Entity used) {
        if (saveTag.state != State.PLAY) return;
        if (!player.equals(getCurrentPlayer())) return;
        Frame frame = frameOfEntity(used.getUniqueId());
        if (frame == null) return;
        onClickFrame(player, frame);
    }

    protected void onClickFrame(Player player, Frame frame) {
        int frameIndex = saveTag.frames.indexOf(frame);
        int frameX = frameIndex % poster.getWidth();
        int frameY = frameIndex / poster.getWidth();
        Frame emptyFrame = findEmptyFrame();
        int emptyIndex = saveTag.frames.indexOf(emptyFrame);
        int emptyX = emptyIndex % poster.getWidth();
        int emptyY = emptyIndex / poster.getHeight();
        boolean validX = (Math.abs(frameX - emptyX) == 1) && (Math.abs(frameY - emptyY) == 0);
        boolean validY = (Math.abs(frameX - emptyX) == 0) && (Math.abs(frameY - emptyY) == 1);
        boolean valid = validX ^ validY;
        if (!valid) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.5f, 0.5f);
            return;
        }
        Vec3i vec = getPosterBlock(emptyX, emptyY);
        Entity entity = Bukkit.getEntity(frame.uuid);
        Location location = vec.toLocation(plugin.getWorld());
        location.setDirection(entity.getLocation().getDirection());
        entity.teleport(location);
        // swap
        saveTag.frames.set(emptyIndex, frame);
        saveTag.frames.set(frameIndex, emptyFrame);
        player.playSound(entity.getLocation(), Sound.ENTITY_ITEM_FRAME_ROTATE_ITEM, SoundCategory.MASTER, 0.5f, 1.0f);
        for (int i = 0; i < saveTag.frames.size(); i += 1) {
            if (i != saveTag.frames.get(i).mapIndex) return;
        }
        perfect(player, false);
        prepareReward(player, true);
        changeState(State.WIN);
    }

    enum State {
        IDLE {
            @Override protected void enter(PosterAttraction instance) {
                instance.despawnAllPosters();
                instance.saveTag.frames = null;
            }
        },
        PLAY {
            @Override protected void enter(PosterAttraction instance) {
                instance.saveTag.playStarted = System.currentTimeMillis();
                instance.shownTime = -1;
                instance.rollPoster();
                instance.spawnAllPosters();
            }

            @Override protected State tick(PosterAttraction instance, Player player) {
                return instance.tickPlay(player);
            }
        },
        WIN {
            @Override protected void enter(PosterAttraction instance) {
                instance.saveTag.winStarted = System.currentTimeMillis();
            }

            @Override protected State tick(PosterAttraction instance, Player player) {
                if (System.currentTimeMillis() - instance.saveTag.winStarted > 5000L) {
                    return State.IDLE;
                }
                return null;
            }
        };

        protected void enter(PosterAttraction instance) { }

        protected void exit(PosterAttraction instance) { }

        protected State tick(PosterAttraction instance, Player player) {
            return null;
        }
    }

    protected static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected List<Frame> frames;
        protected long playStarted;
        protected long winStarted;
    }

    public void setPoster(String name) {
        PosterPlugin posterPlugin = (PosterPlugin) Bukkit.getPluginManager().getPlugin("Poster");
        this.poster = Objects.requireNonNull(posterPlugin.findPosterNamed(name));
    }
}
