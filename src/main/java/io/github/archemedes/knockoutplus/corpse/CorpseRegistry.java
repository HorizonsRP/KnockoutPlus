package io.github.archemedes.knockoutplus.corpse;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import io.github.archemedes.knockoutplus.KnockoutPlus;
import io.github.archemedes.knockoutplus.events.PlayerReviveEvent;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

@Getter
public class CorpseRegistry {
    final Map<UUID, Corpse> victims = Maps.newHashMap();
    final HashMultimap<UUID, Corpse> kills = HashMultimap.create();
    private KnockoutPlus plugin;

    public CorpseRegistry(KnockoutPlus plugin) {
        this.plugin = plugin;
    }


    public Corpse register(Player p, Location l) {
        UUID uuid = p.getUniqueId();
        Corpse c = new Corpse(p, l, plugin);
        victims.put(uuid, c);
        return c;
    }

    public Corpse register(Player player, Player killer, Location l) {
        UUID playerUUID = player.getUniqueId();
        UUID killerUUID = killer.getUniqueId();
        Corpse c = new Corpse(player, killerUUID, l, plugin);
        victims.put(playerUUID, c);
        kills.put(killerUUID, c);
        return c;
    }

    public void reviveAll() {
        Iterator iter = victims.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Corpse c = (Corpse) entry.getValue();
            Player p = plugin.getKoListener().getPlayer(c.getVictim());

            if (p != null) {
                PlayerReviveEvent event = new PlayerReviveEvent(null, p, PlayerReviveEvent.Reason.OPERATOR);
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    plugin.revivePlayer(p, p.getMaxHealth());
                    p.sendMessage(ChatColor.GOLD + "An Aengul smiles upon you.");
                    iter.remove();
                    UUID killer = c.getKiller();
                    if (killer != null) kills.remove(killer, entry.getKey());
                }
            } else {
                throw new NullPointerException("[KO+] Knocked out Player was null");
            }
        }
    }

    public void tick() {
        int t = plugin.bleedoutTime * 1000;
        int u = (int) (t * 0.7D);
        long now = System.currentTimeMillis();
        Iterator iter = victims.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Corpse c = (Corpse) entry.getValue();
            long diff = now - c.whenKnockedOut();
            if (diff > t) {
                iter.remove();
                if (c.getKiller() != null) kills.remove(c.getKiller(), c);
                Player p = plugin.getKoListener().getPlayer(c.getVictim());
                if (p != null) {
                    p.damage(1.0D);
                    plugin.wakeOne(p);
                    p.setHealth(0.0D);
                } else {
                    Bukkit.getLogger().warning("[KO+] Player was null while we tried to kill: " + c.getVictim());
                }
            } else if ((diff > u) && (!c.warned)) {
                c.warned = true;
                Player p = plugin.getKoListener().getPlayer(c.getVictim());

                if (p != null) {
                    p.sendMessage(ChatColor.RED + "You are starting to lose consciousness!");
                    p.sendMessage(ChatColor.GREEN.toString());
                } else {
                    Bukkit.getLogger().warning("[KO+] Player was null while we tried to warn: " + c.getVictim());
                }
            }
        }
    }

    public boolean isKnockedOut(Player p) {
        return victims.containsKey(p.getUniqueId());
    }

    public Corpse getCorpse(Player p) {
        return victims.get(p.getUniqueId());
    }

    public Set<Corpse> getVictims(Player p) {
        return kills.get(p.getUniqueId());
    }

    public Collection<Corpse> getCorpses() {
        return victims.values();
    }
}