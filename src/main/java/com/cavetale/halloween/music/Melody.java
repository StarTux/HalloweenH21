package com.cavetale.halloween.music;

import com.cavetale.halloween.HalloweenPlugin;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class Melody {
    public static final int N32 = 1;
    public static final int N16 = 2;
    public static final int N8 = 4;
    public static final int N4 = 8;
    public static final int N2 = 16;
    public static final int N1 = 32;
    private final List<Beat> beats;

    public static Melody of(List<Beat> beats) {
        return new Melody(beats);
    }

    public static Melody of(Beat[] beats) {
        return new Melody(List.of(beats));
    }

    /**
     * Concatenate several melodies.
     */
    public static Melody of(Melody... melodies) {
        List<Beat> newBeats = new ArrayList<>();
        for (Melody melody : melodies) {
            newBeats.addAll(melody.beats);
        }
        return Melody.of(newBeats);
    }

    public int size() {
        return beats.size();
    }

    public Beat getBeat(int index) {
        return index >= 0 && index < beats.size()
            ? beats.get(index)
            : Beat.ZERO;
    }

    public void play(Player player, Location location, int speed, Runnable callback) {
        playInternal(beat -> {
                if (!player.isOnline()) return false;
                beat.play(player, location);
                return true;
            }, speed, callback, 0);
    }

    public void play(Player player, int speed, Runnable callback) {
        playInternal(beat -> {
                if (!player.isOnline()) return false;
                beat.play(player, player.getLocation());
                return true;
            }, speed, callback, 0);
    }

    public void play(Location location, int speed, Runnable callback) {
        playInternal(beat -> {
                if (!location.isChunkLoaded()) return false;
                beat.play(location);
                return true;
            }, speed, callback, 0);
    }

    private void playInternal(Function<Beat, Boolean> consumer, int speed, Runnable callback, int index) {
        if (index >= beats.size()) {
            if (callback != null) callback.run();
            return;
        }
        Beat beat = beats.get(index);
        if (!consumer.apply(beat)) {
            if (callback != null) callback.run();
            return;
        }
        if (beat.getTicks() == 0) {
            playInternal(consumer, speed, callback, index + 1);
        } else {
            long delay = (long) (beat.getTicks() * speed);
            Bukkit.getScheduler().runTaskLater(HalloweenPlugin.getInstance(), () -> {
                    playInternal(consumer, speed, callback, index + 1);
                }, delay);
        }
    }

    public List<String> serialize() {
        List<String> result = new ArrayList<>(beats.size());
        for (Beat beat : beats) {
            result.add(beat.serialize());
        }
        return result;
    }

    public static Melody deserialize(List<String> in) {
        List<Beat> result = new ArrayList<>(in.size());
        for (String it : in) {
            result.add(Beat.deserialize(it));
        }
        return Melody.of(result);
    }
}
