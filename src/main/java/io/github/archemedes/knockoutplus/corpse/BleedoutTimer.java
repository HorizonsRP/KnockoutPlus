package io.github.archemedes.knockoutplus.corpse;

import io.github.archemedes.knockoutplus.KnockoutPlus;
import org.bukkit.scheduler.BukkitRunnable;

public class BleedoutTimer extends BukkitRunnable {
    public KnockoutPlus plugin;

    public BleedoutTimer(KnockoutPlus plugin) {
        this.plugin = plugin;
    }

    public void run() {
        plugin.getCorpseRegistry().tick();
    }
}