package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Cuboid;
import com.cavetale.area.struct.Vec3i;
import com.cavetale.core.font.Unicode;
import com.cavetale.core.font.VanillaItems;
import com.cavetale.halloween.HalloweenPlugin;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class DummyAttraction extends Attraction<DummyAttraction.SaveTag> {
    protected static final Duration PLAY_TIME = Duration.ofSeconds(30);
    protected int secondsLeft;
    protected final Set<Vec3i> blocks = new HashSet<>();;

    protected DummyAttraction(final HalloweenPlugin plugin, final String name, final List<Cuboid> areaList) {
        super(plugin, name, areaList, SaveTag.class, SaveTag::new);
        for (Cuboid area : areaList) {
            if ("name".equals(area.name)) {
                blocks.addAll(area.enumerate());
            }
        }
        this.displayName = Component.text("Dummy", NamedTextColor.RED);
    }

    @Override
    public boolean isPlaying() {
        return saveTag.state != State.IDLE;
    }

    @Override
    public void start(Player player) {
        saveTag.currentPlayer = player.getUniqueId();
        startingGun(player);
        changeState(State.PLAY);
    }

    @Override
    protected void stop() {
        changeState(State.IDLE);
    }

    @Override
    public void onTick() {
        if (saveTag.state == State.IDLE) return;
        Player player = getCurrentPlayer();
        if (player == null) return;
        State newState = saveTag.state.tick(this, player);
        if (newState != null) changeState(newState);
    }

    protected State tickPlay(Player player) {
        long now = System.currentTimeMillis();
        long timeout = saveTag.playStarted + PLAY_TIME.toMillis();
        if (now > timeout) {
            timeout(player);
            return State.IDLE;
        }
        int seconds = (int) ((timeout - now - 1) / 1000L) + 1;
        if (seconds != secondsLeft) {
            secondsLeft = seconds;
            String progress = 0 + "/" + 1;
            player.sendActionBar(Component.join(JoinConfiguration.noSeparators(), new Component[] {
                        Component.text(Unicode.WATCH.string + seconds, NamedTextColor.GOLD),
                        Component.space(),
                        VanillaItems.DIAMOND.component,
                        Component.text(progress, NamedTextColor.DARK_RED),
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
        PLAY {
            @Override protected State tick(DummyAttraction instance, Player player) {
                return instance.tickPlay(player);
            }

            @Override protected void enter(DummyAttraction instance) {
                instance.saveTag.playStarted = System.currentTimeMillis();
            }

            @Override protected void exit(DummyAttraction instance) {
            }
        };

        protected void enter(DummyAttraction instance) { }

        protected void exit(DummyAttraction instance) { }

        protected State tick(DummyAttraction instance, Player player) {
            return null;
        }
    }

    protected static final class SaveTag extends Attraction.SaveTag {
        protected State state = State.IDLE;
        protected long playStarted;
    }
}
