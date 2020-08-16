package io.github.archemedes.knockoutplus.commands;

import co.lotc.core.command.CommandTemplate;
import co.lotc.core.command.annotate.Flag;
import io.github.archemedes.knockoutplus.KnockoutPlus;
import io.github.archemedes.knockoutplus.corpse.Corpse;
import io.github.archemedes.knockoutplus.events.PlayerReviveEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static net.md_5.bungee.api.ChatColor.GOLD;
import static net.md_5.bungee.api.ChatColor.RED;

public class ReviveCommand extends CommandTemplate {

	KnockoutPlus plugin;

	public ReviveCommand(KnockoutPlus plugin) {
		this.plugin = plugin;
	}

	@Flag(name = "gm", permission = "knockoutplus.remoterevive", description = "Allows reviving from remote locations")
	public void invoke(CommandSender sender, Player target) {
		validate(plugin.getCorpseRegistry().isKnockedOut(target), GOLD + target.getName() + RED + " can not be helped");

		final Corpse corpse = plugin.getCorpseRegistry().getCorpse(target);
		Player killer = plugin.getKoListener().getPlayer(corpse.getKiller());
		if (!(sender instanceof Player) || (hasFlag("gm") && sender.hasPermission("knockoutplus.mod"))) {
			PlayerReviveEvent event = new PlayerReviveEvent(null, target, PlayerReviveEvent.Reason.OPERATOR);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				plugin.revivePlayer(target, sender, 4.0D);
				corpse.unregister();
			}
			return;
		}

		Player player = (Player) sender;

		validate(!plugin.getCorpseRegistry().isKnockedOut(player), "You are currently knocked out!");
		validate(!player.equals(target), "You may not revive yourself!");

		if (player.equals(killer)) {
			PlayerReviveEvent event = new PlayerReviveEvent(player, target, PlayerReviveEvent.Reason.MERCY);
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled()) return;

			sender.sendMessage(ChatColor.GOLD + "You have allowed " + plugin.giveName(target) + ChatColor.GOLD + " to live.");
			plugin.revivePlayer(target, sender, 4.0D);
			corpse.unregister();
		}

		validate(player.getLocation().getWorld().equals(target.getLocation().getWorld()), GOLD + target.getName() + RED + " can not be helped!");
		validate(player.getLocation().distanceSquared(target.getLocation()) <= 20 * 20, "You are too far away to help " + GOLD + target.getName());
		validate(player.getLocation().distanceSquared(target.getLocation()) <= 3 * 3, "You must move closer to help " + GOLD + target.getName());

		player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WOOL_BREAK, 3.5F, -1.0F);
		player.sendMessage(ChatColor.YELLOW + "You bend down to try and assist " + plugin.giveName(target));
		player.sendMessage(String.valueOf(ChatColor.GRAY) + ChatColor.BOLD + "(Hold still or your action will be interrupted.)");
		target.sendMessage(ChatColor.YELLOW + "You are being assisted by " + plugin.giveName(player));

		final Location chantSpot = player.getLocation();

		int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
					plugin.getKoListener().chants.remove(player.getUniqueId());
					if (!plugin.getCorpseRegistry().isKnockedOut(target)) return;

					if (player.getLocation().getWorld() == chantSpot.getWorld())
						if (player.getLocation().distance(chantSpot) > 0.2D) {
							player.sendMessage(ChatColor.RED + "You have been interrupted!");
						} else {
							PlayerReviveEvent event = new PlayerReviveEvent(player, target, PlayerReviveEvent.Reason.COMMAND_REVIVE);
							Bukkit.getPluginManager().callEvent(event);
							if (event.isCancelled()) return;

							player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.9F, 0.7F);
							player.sendMessage(ChatColor.GOLD + plugin.giveName(target) + ChatColor.GOLD + " takes your hand, rising to their feet once more.");
							target.sendMessage(ChatColor.GOLD + "You take " + plugin.giveName(player) + ChatColor.GOLD + "'s hand, rising to your feet.");

							plugin.revivePlayer(target, sender, 4.0D);
							corpse.unregister();
						}
				}
				, 100L);

		Integer oldTaskId = plugin.getKoListener().chants.put(player.getUniqueId(), taskId); //TODO  make this a method so we don't have to directly access the map
		if (oldTaskId != null) {
			Bukkit.getScheduler().cancelTask(oldTaskId);
		}
	}
}
