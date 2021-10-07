package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Cuboid;
import com.cavetale.halloween.HalloweenPlugin;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Location;
import org.bukkit.Note.Tone;
import org.bukkit.Note;
import org.bukkit.entity.Player;

public final class ShootTargetAttraction extends Attraction<ShootTargetAttraction.SaveTag> {
    protected ShootTargetAttraction(final HalloweenPlugin plugin, final String name, final List<Cuboid> areaList) {
        super(plugin, name, areaList, SaveTag.class);
    }

    @Override
    protected void start(Player player) {
    }

    @Override
    protected boolean isPlaying() {
        return saveTag.state != State.IDLE;
    }

    enum State {
        IDLE;
    }

    static final class SaveTag {
        protected State state = State.IDLE;
    }
}
