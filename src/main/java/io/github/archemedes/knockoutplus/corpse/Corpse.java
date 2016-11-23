package io.github.archemedes.knockoutplus.corpse;

import io.github.archemedes.knockoutplus.KnockoutPlus;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Corpse {
	private final UUID victim;
	private final int id;
	private final UUID killer;
	private final Location where;
	private final long when;
	boolean warned = false;
	private KnockoutPlus plugin;

	public Corpse(Player victim, Location l, KnockoutPlus plugin) {
		this(victim, null, l, plugin);
	}

	public Corpse(Player victim, UUID killer, Location l, KnockoutPlus plugin) {
		this.victim = victim.getUniqueId();
		this.id = victim.getEntityId();
		this.killer = killer;
		this.where = l;
		this.when = System.currentTimeMillis();
        this.plugin = plugin;
	}

	public UUID getVictim() {
		return this.victim;
	}

	public UUID getKiller() {
		return this.killer;
	}

	public int getEntityId() {
		return this.id;
	}

	public Location getLocation() {
		return this.where.clone();
	}

	public long whenKnockedOut() {
		return this.when;
	}

	public boolean wasPvP() {
		return this.killer != null;
	}

	public void unregister() {
		plugin.getCorpseRegistry().victims.remove(this.victim);
		if (this.killer != null) plugin.getCorpseRegistry().kills.remove(this.killer, this);
	}
}