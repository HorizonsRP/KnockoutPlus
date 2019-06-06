package io.github.archemedes.knockoutplus.corpse;

import io.github.archemedes.knockoutplus.KnockoutPlus;
import net.lordofthecraft.arche.ArcheCore;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

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

    public void sendPlayerHead() {
        Bukkit.getPlayer(this.winner).getInventory().addItem(ArcheCore.getPersona(Bukkit.getPlayer(this.loser)).getSkin().getHeadItem());
        this.downedTime = System.currentTimeMillis();
        this.claimed = true;
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
