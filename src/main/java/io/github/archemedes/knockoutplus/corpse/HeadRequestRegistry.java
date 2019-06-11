package io.github.archemedes.knockoutplus.corpse;

import com.google.common.collect.HashMultimap;
import io.github.archemedes.knockoutplus.KnockoutPlus;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
public class HeadRequestRegistry {
    final HashMultimap<UUID, HeadRequest> HeadRequests = HashMultimap.create();
    private KnockoutPlus plugin;

    public HeadRequestRegistry(KnockoutPlus plugin) {
        this.plugin = plugin;
    }

    public void register(Player winner, Player loser) {
        HeadRequest h = getHeadRequest(winner, loser);
        if (h == null) {
            h = new HeadRequest(winner.getUniqueId(), loser.getUniqueId(), plugin);
            HeadRequests.put(winner.getUniqueId(), h);
        } else {
            if (!h.getClaimed()) {
                h.setDownedTime(System.currentTimeMillis());
            } else if ((h.getDownedTime() + TimeUnit.MINUTES.toMillis(60L)) < System.currentTimeMillis()) {
                h.setDownedTime(System.currentTimeMillis());
                h.setClaimed(false);
            }
        }
    }

    public HeadRequest getHeadRequest(Player winner, Player loser) {
        Set<HeadRequest> reqs = HeadRequests.get(winner.getUniqueId());
        for (HeadRequest headRequest : reqs) {
            if (headRequest.getLoser().equals(loser.getUniqueId())) {
                return headRequest;
            }
        }
        return null;
    }

    public void requestHead(Player winner, Player loser) {
        Set<HeadRequest> reqs = HeadRequests.get(winner.getUniqueId());

        for (HeadRequest headRequest : reqs) {
            if (Bukkit.getPlayer(headRequest.getLoser()).equals(loser.getUniqueId()) && !headRequest.getClaimed()) {
                if ((headRequest.getDownedTime() + TimeUnit.MINUTES.toMillis(60L)) < System.currentTimeMillis()) {
                    headRequest.unregister();
                    winner.sendMessage(ChatColor.RED + "Time expired. Unable to request " + loser.getName() + "'s head.");
                } else if ((headRequest.getLastRequestTime() + TimeUnit.MINUTES.toMillis(5L)) > System.currentTimeMillis()) {
                    winner.sendMessage(ChatColor.RED + "You've already sent a request for " + loser.getName() + "'s head recently.");
                } else {
                    headRequest.setLastRequestTime(System.currentTimeMillis());
                    headRequest.askForPlayerHead();
                }
            }
        }
    }

    public boolean sendHead(Player winner, Player loser) {
        HeadRequest h = getHeadRequest(winner, loser);
        if (h != null) {
            if ((h.getDownedTime() + TimeUnit.MINUTES.toMillis(60L)) < System.currentTimeMillis()) {
                h.unregister();
                loser.sendMessage(ChatColor.RED + "Time expired. Unable to send your player head to " + winner.getName() + ".");
            } else if (!h.getClaimed()) {
                return h.sendPlayerHead();
            } else {
                loser.sendMessage(ChatColor.RED + "You've already sent your player head to " + winner.getName() + ".");
            }
        } else {
            loser.sendMessage(ChatColor.RED + winner.getName() + " hasn't defeated you in PvP.");
        }
        return false;
    }

}
