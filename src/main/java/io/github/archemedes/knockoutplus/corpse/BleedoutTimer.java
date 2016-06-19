package io.github.archemedes.knockoutplus.corpse;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class BleedoutTimer extends BukkitRunnable
{
  public BleedoutTimer(Plugin plugin)
  {
  }

  public void run()
  {
    CorpseRegistry.tick();
  }
}

/* Location:           C:\Users\Nick\Desktop\Minecraft\LOTC\LeadDev\plugins\KnockoutPlus.jar
 * Qualified Name:     io.github.archemedes.knockoutplus.corpse.BleedoutTimer
 * JD-Core Version:    0.6.2
 */