package io.github.archemedes.knockoutplus.corpse;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Corpse
{
	private final UUID victim;
	private final int id;
	private final UUID killer;
	private final Location where;
	private final long when;
	boolean warned = false;

	public Corpse(Player victim, Location l) {
		this(victim, null, l);
	}

	public Corpse(Player victim, UUID killer, Location l) {
		this.victim = victim.getUniqueId();
		this.id = victim.getEntityId();
		this.killer = killer;
		this.where = l;
		this.when = System.currentTimeMillis();
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
		CorpseRegistry.victims.remove(this.victim);
		if (this.killer != null) CorpseRegistry.kills.remove(this.killer, this);
	}
}

/* Location:           C:\Users\Nick\Desktop\Minecraft\LOTC\LeadDev\plugins\KnockoutPlus.jar
 * Qualified Name:     io.github.archemedes.knockoutplus.corpse.Corpse
 * JD-Core Version:    0.6.2
 */