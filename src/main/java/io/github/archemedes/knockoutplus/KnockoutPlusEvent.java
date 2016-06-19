package io.github.archemedes.knockoutplus;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerEvent;

public abstract class KnockoutPlusEvent extends PlayerEvent
  implements Cancellable
{
  private final Player target;
  private boolean cancel = false;

  protected KnockoutPlusEvent(Player player, Player target)
  {
    super(player);
    this.target = target;
  }

  public Player getTarget() {
    return this.target;
  }

  public boolean isCancelled() {
    return this.cancel;
  }
  public void setCancelled(boolean cancel) {
    this.cancel = cancel;
  }
}

/* Location:           C:\Users\Nick\Desktop\Minecraft\LOTC\LeadDev\plugins\KnockoutPlus.jar
 * Qualified Name:     io.github.archemedes.knockoutplus.KnockoutPlusEvent
 * JD-Core Version:    0.6.2
 */