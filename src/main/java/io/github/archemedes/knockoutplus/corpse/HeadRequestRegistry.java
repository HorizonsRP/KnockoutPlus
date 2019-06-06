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

    public void requestHeads(Player winner) {
        requestHeads(winner.getUniqueId());
    }

    public void requestHeads(UUID winner) {
        Player winnerPlayer = Bukkit.getPlayer(winner);
        Set<HeadRequest> reqs = HeadRequests.get(winner);
        int headsRequested = 0;

        for (HeadRequest headRequest : reqs) {
            if (Bukkit.getPlayer(headRequest.getLoser()) == null) {
                winnerPlayer.sendMessage(ChatColor.RED + "Unable to request a player head from an offline player.");
            } else if (headRequest.getClaimed()) {

            } else {
                if ((headRequest.getDownedTime() + TimeUnit.MINUTES.toMillis(60L)) < System.currentTimeMillis()) {
                    headRequest.unregister();
                } else if ((headRequest.getLastRequestTime() + TimeUnit.MINUTES.toMillis(5L)) > System.currentTimeMillis()) {
                    winnerPlayer.sendMessage(ChatColor.RED + "You've already sent a request for someone's head recently.");
                } else {
                    headRequest.setLastRequestTime(System.currentTimeMillis());
                    headRequest.askForPlayerHead();
                    headsRequested++;
                }
            }
        }
        winnerPlayer.sendMessage(ChatColor.BLUE + "Requested player heads from " + ChatColor.GOLD + headsRequested + ChatColor.BLUE + " player" + (headsRequested == 1 ? "" : "s") + ".");
    }

    public boolean sendHead(Player winner, Player loser) {
        HeadRequest h = getHeadRequest(winner, loser);
        if (h != null) {
            if ((h.getDownedTime() + TimeUnit.MINUTES.toMillis(60L)) < System.currentTimeMillis()) {
                h.unregister();
                loser.sendMessage(ChatColor.RED + "Time expired. Unable to send your player head to " + winner.getName() + ".");
            } else if (!h.getClaimed()) {
                h.sendPlayerHead();
                return true;
            } else {
                loser.sendMessage(ChatColor.RED + "You've already sent your player head to " + winner.getName() + ".");
            }
        } else {
            loser.sendMessage(ChatColor.RED + winner.getName() + " hasn't defeated you in PvP.");
        }
        return false;
    }

}
