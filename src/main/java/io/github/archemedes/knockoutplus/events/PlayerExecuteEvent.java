package io.github.archemedes.knockoutplus.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class PlayerExecuteEvent extends KnockoutPlusEvent
{
  private static final HandlerList handlers = new HandlerList();

  public PlayerExecuteEvent(Player player, Player target) {
    super(player, target);
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  public HandlerList getHandlers() {
    return handlers;
  }
}