package io.github.archemedes.knockoutplus.corpse;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import io.github.archemedes.knockoutplus.KOListener;
import io.github.archemedes.knockoutplus.KnockoutPlus;
import io.github.archemedes.knockoutplus.PlayerReviveEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class CorpseRegistry
{
	static final Map<UUID, Corpse> victims = Maps.newHashMap();
	static final HashMultimap<UUID, Corpse> kills = HashMultimap.create();

	public static Corpse register(Player p, Location l) {
		UUID uuid = p.getUniqueId();
		Corpse c = new Corpse(p, l);
		victims.put(uuid, c);
		return c;
	}

	public static Corpse register(Player p, Player k, Location l) {
		UUID uuidp = p.getUniqueId();
		UUID uuidk = k.getUniqueId();
		Corpse c = new Corpse(p, uuidk, l);
		victims.put(uuidp, c);
		kills.put(uuidk, c);
		return c;
	}

	public static void reviveAll() {
		Iterator iter = victims.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry)iter.next();
			Corpse c = (Corpse)entry.getValue();
			Player p = KOListener.getPlayer(c.getVictim());

			if (p != null) {
				PlayerReviveEvent event = new PlayerReviveEvent(null, p, PlayerReviveEvent.Reason.OPERATOR);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					KnockoutPlus.revivePlayer(p, p.getMaxHealth());
					p.sendMessage(ChatColor.GOLD+"An Aengul smiles upon you.");
					iter.remove();
					UUID killer = c.getKiller();
					if (killer != null) kills.remove(killer, entry.getKey()); 
				}
			}
			else { throw new NullPointerException("[KO+] Knocked out Player was null"); }
		}
	}

	static void tick()
	{
		int t = KnockoutPlus.bleedoutTime * 1000;
		int u = (int)(t * 0.7D);
		long now = System.currentTimeMillis();
		Iterator iter = victims.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry)iter.next();
			Corpse c = (Corpse)entry.getValue();
			long diff = now - c.whenKnockedOut();
			if (diff > t) {
				iter.remove();
				if (c.getKiller() != null) kills.remove(c.getKiller(), c);
				Player p = KOListener.getPlayer(c.getVictim());
				if (p != null)
				{
					p.damage(1.0D);
					KnockoutPlus.wakeOne(p);
					p.setHealth(0.0D);
				}
				else {
					Bukkit.getLogger().warning("[KO+] Player was null while we tried to kill: " + c.getVictim());
				}
			} else if ((diff > u) && (!c.warned)) {
				c.warned = true;
				Player p = KOListener.getPlayer(c.getVictim());

				if (p != null) {
					p.sendMessage(ChatColor.RED+"You are starting to lose consciousness!");
					p.sendMessage(ChatColor.GREEN.toString());
				} else {
					Bukkit.getLogger().warning("[KO+] Player was null while we tried to warn: " + c.getVictim());
				}
			}
		}
	}

	public static boolean isKnockedOut(Player p)
	{
		return victims.containsKey(p.getUniqueId());
	}

	public static Corpse getCorpse(Player p) {
		return victims.get(p.getUniqueId());
	}

	public static Set<Corpse> getVictims(Player p) {
		return kills.get(p.getUniqueId());
	}

	public static Collection<Corpse> getCorpses() {
		return victims.values();
	}
}

/* Location:           C:\Users\Nick\Desktop\Minecraft\LOTC\LeadDev\plugins\KnockoutPlus.jar
 * Qualified Name:     io.github.archemedes.knockoutplus.corpse.CorpseRegistry
 * JD-Core Version:    0.6.2
 */