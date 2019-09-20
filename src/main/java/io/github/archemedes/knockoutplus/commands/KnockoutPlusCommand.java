package io.github.archemedes.knockoutplus.commands;

import co.lotc.core.command.CommandTemplate;
import co.lotc.core.command.annotate.Cmd;
import io.github.archemedes.knockoutplus.KnockoutPlus;
import io.github.archemedes.knockoutplus.utils.PacketUtils;
import org.bukkit.entity.Player;

public class KnockoutPlusCommand extends CommandTemplate {

	KnockoutPlus plugin;

	public KnockoutPlusCommand(KnockoutPlus plugin) {
		this.plugin = plugin;
	}

	@Cmd("Testing command")
	public void down(Player player, Player target) {
        PacketUtils.layDown(target, target.getLocation());
		player.sendMessage("Caused " + target.getName() + " to lay down");
	}

	@Cmd("Testing command")
    public void wakeup(Player player, Player target) {
		PacketUtils.wakeup(target);
		player.sendMessage("Picked up " + target.getName());
	}

}
