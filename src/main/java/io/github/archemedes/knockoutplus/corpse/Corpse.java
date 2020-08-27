package io.github.archemedes.knockoutplus.corpse;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.github.archemedes.knockoutplus.KnockoutPlus;
import lombok.Getter;

public class Corpse {
	private final UUID victim;
	private final int id;
	private final UUID killer;
	private final Location where;
	private final long when;
	@Getter private final long bleedoutTime;
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

		this.bleedoutTime = plugin.getBleedoutTime();
		long distance = 2 * this.bleedoutTime;
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
		while (plugin.getCorpseRegistry().victims.containsKey(this.victim) || plugin.getCorpseRegistry().movementStopped.contains(this.victim)) {
			plugin.getCorpseRegistry().victims.remove(this.victim);
			plugin.getCorpseRegistry().movementStopped.remove(this.victim);
		}
		if (this.killer != null) plugin.getCorpseRegistry().kills.remove(this.killer, this);
	}
}