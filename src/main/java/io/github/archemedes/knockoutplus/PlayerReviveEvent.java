package io.github.archemedes.knockoutplus;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class PlayerReviveEvent extends KnockoutPlusEvent
{
	private static final HandlerList handlers = new HandlerList();
	private final Reason reason;

	public PlayerReviveEvent(Player player, Player target, Reason reason) {
		super(player, target);
		this.reason = reason;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public HandlerList getHandlers() {
		return handlers;
	}

	public Reason getReason() {
		return this.reason;
	}

	public enum Reason {
		MERCY, 
		COMMAND_REVIVE, 
		OPERATOR
	}
}

/* Location:           C:\Users\Nick\Desktop\Minecraft\LOTC\LeadDev\plugins\KnockoutPlus.jar
 * Qualified Name:     io.github.archemedes.knockoutplus.PlayerReviveEvent
 * JD-Core Version:    0.6.2
 */