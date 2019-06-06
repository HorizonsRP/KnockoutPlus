package io.github.archemedes.knockoutplus.corpse;

import io.github.archemedes.knockoutplus.KnockoutPlus;
import net.lordofthecraft.arche.ArcheCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class HeadRequest {
    private final UUID winner;
    private final UUID loser;
    private long downedTime;
    private long lastRequestTime;
    private boolean claimed;
    private KnockoutPlus plugin;

    public HeadRequest(UUID winner, UUID loser, KnockoutPlus plugin) {
        this.winner = winner;
        this.loser = loser;
        this.downedTime = System.currentTimeMillis();
        this.lastRequestTime = 0L;
        this.claimed = false;
        this.plugin = plugin;
    }

    public void askForPlayerHead() {
        if (Bukkit.getPlayer(this.loser) != null && !this.claimed) {
            Bukkit.getPlayer(this.loser).sendMessage(ChatColor.GOLD + Bukkit.getPlayer(this.winner).getName() + ChatColor.BLUE + " has requested for your player head. Use " + ChatColor.GOLD + "/sendhead [player]" + ChatColor.BLUE + " if you want to send your player head to them.");
        }
    }

    public boolean sendPlayerHead() {
        Player winner = Bukkit.getPlayer(this.winner);
        Player loser = Bukkit.getPlayer(this.loser);
        Map<Integer, ItemStack> map = winner.getInventory().addItem(ArcheCore.getPersona(loser).getSkin().getHeadItem());
        if (map.isEmpty()) {
            this.downedTime = System.currentTimeMillis();
            this.claimed = true;
            loser.sendMessage(ChatColor.BLUE + "Sent player head to " + ChatColor.GOLD + winner.getName());
            winner.sendMessage(ChatColor.GOLD + loser.getName() + ChatColor.BLUE + " has sent their player head to you.");
            return true;
        } else {
            loser.sendMessage(ChatColor.RED + winner.getName() + "'s inventory is full.");
            winner.sendMessage(ChatColor.RED + loser.getName() + " tried sending their player head to you, but your inventory is full.");
        }
        return false;
    }

    public UUID getWinner() { return this.winner; }

    public UUID getLoser() { return this.loser; }

    public long getDownedTime() { return this.downedTime; }

    public void setDownedTime(long time) { this.downedTime = time; }

    public long getLastRequestTime() {return this.lastRequestTime; }

    public void setLastRequestTime(long time) { this.lastRequestTime = time; }

    public boolean getClaimed() { return this.claimed; }

    public void setClaimed(boolean claim) { this.claimed = claim; }

    public void unregister() {
        plugin.getHeadRequestRegistry().HeadRequests.remove(this.winner, this);
    }
}
