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
import net.lordofthecraft.arche.ArcheCore;
import net.lordofthecraft.arche.interfaces.Persona;

public class Corpse {
	private final UUID victim;
	private final int id;
	private final UUID killer;
	private final Location where;
	private final long when;
	@Getter private final long bleedoutTime;
	boolean warned = false;
	private KnockoutPlus plugin;
	
	private final Set<UUID> allowedRevives;

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
		
		Persona ps = ArcheCore.getPersona(victim);
		this.bleedoutTime = ps != null? (long) ps.attributes().getAttributeValue(plugin.getBleedoutAttribute()) : plugin.getBleedoutTime();
		long distance = 2 * this.bleedoutTime;
		
		allowedRevives = Bukkit.getOnlinePlayers().stream()
			.filter(p->p.getWorld() == l.getWorld())
			.filter(p->p.getLocation().distance(where) < distance)
			.filter(victim::canSee)
			.filter(p-> p != victim)
			.map(Player::getUniqueId)
			.collect(Collectors.toSet());
	}

	public boolean allowedToRevive(CommandSender sender) {
		if(!(sender instanceof Player)) return true;
		if(sender.hasPermission("archecore.admin")) return true;

		return allowedRevives.contains(((Player) sender).getUniqueId());
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