package io.github.archemedes.knockoutplus;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class PlayerExecuteEvent extends KnockoutPlusEvent
{
  private static final HandlerList handlers = new HandlerList();

  public HandlerList getHandlers() {
    return handlers;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  public PlayerExecuteEvent(Player player, Player target)
  {
    super(player, target);
  }
}

/* Location:           C:\Users\Nick\Desktop\Minecraft\LOTC\LeadDev\plugins\KnockoutPlus.jar
 * Qualified Name:     io.github.archemedes.knockoutplus.PlayerExecuteEvent
 * JD-Core Version:    0.6.2
 */