package io.github.archemedes.knockoutplus.commands;

import co.lotc.core.command.CommandTemplate;
import co.lotc.core.command.annotate.Cmd;
import io.github.archemedes.knockoutplus.KnockoutPlus;
import io.github.archemedes.knockoutplus.PacketManager;
import org.bukkit.entity.Player;

public class KnockoutPlusCommand extends CommandTemplate {

	KnockoutPlus plugin;

	public KnockoutPlusCommand(KnockoutPlus plugin) {
		this.plugin = plugin;
	}

	@Cmd("Testing command")
	public void test(Player player, Player target) {
		PacketManager.layDown(target);
		player.sendMessage("Caused " + target.getName() + " to lay down");
	}

}
