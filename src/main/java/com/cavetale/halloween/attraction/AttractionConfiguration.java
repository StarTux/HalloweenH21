package com.cavetale.halloween.attraction;

import com.cavetale.area.struct.Area;
import com.cavetale.halloween.Booth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.World;

@RequiredArgsConstructor
public final class AttractionConfiguration {
    protected final World world;
    protected final String name;
    protected final List<Area> areaList;
    protected final Booth booth;
}
