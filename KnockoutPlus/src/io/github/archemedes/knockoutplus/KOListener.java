package io.github.archemedes.knockoutplus;

import io.github.archemedes.knockoutplus.corpse.Corpse;
import io.github.archemedes.knockoutplus.corpse.CorpseRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitScheduler;

public class KOListener
  implements Listener
{
  static ArrayList<UUID> verdictDelay = new ArrayList();
  static HashMap<UUID, Integer> chants = new HashMap();
  private final Random rnd = new Random();
  private final KnockoutPlus plugin;

  KOListener(Plugin plugin)
  {
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
    this.plugin = ((KnockoutPlus)plugin);
  }

  @EventHandler(ignoreCancelled=true)
  public void blockCommands(PlayerCommandPreprocessEvent e)
  {
    Player p = e.getPlayer();

    interrupt(p);

    if (CorpseRegistry.isKnockedOut(p)) {
      String msg = e.getMessage();
      if ((!p.isOp()) && 
        (!msg.startsWith("/tell ")) && (!msg.startsWith("/damage ")) && (!msg.startsWith("/modreq ")) && (!msg.startsWith("/check ")) && 
        (!msg.startsWith("/t ")) && (!msg.startsWith("/d20")) && (!msg.startsWith("/d48")) && (!msg.startsWith("/msg ")) && (!msg.startsWith("/modlist ")) && 
        (!msg.startsWith("/m ")) && (!msg.startsWith("/s ")) && (!msg.startsWith("/w ")) && (!msg.startsWith("/whisper ")) && 
        (!msg.startsWith("/reply ")) && (!msg.startsWith("/r ")))
        e.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled=true, priority=EventPriority.LOWEST)
  public void onTarget(EntityTargetLivingEntityEvent e)
  {
    if ((e.getTarget() instanceof Player)) {
      Player p = (Player)e.getTarget();
      if ((CorpseRegistry.isKnockedOut(p)) && (KnockoutPlus.mobsUntarget))
        e.setCancelled(true);
    }
  }

  @EventHandler(ignoreCancelled=true)
  public void onBlockBreak(BlockBreakEvent e)
  {
    if (!KnockoutPlus.protectBlocks) return;
    Location loc = e.getBlock().getLocation();

    for (Corpse c : CorpseRegistry.getCorpses()) {
      Location l = c.getLocation();
      if ((l.getWorld().equals(loc.getWorld())) && 
        (l.distance(loc) <= 2.0D)) {
        e.setCancelled(true);
        e.getPlayer().sendMessage("§cSomeone is dying here! Have some respect!");
        return;
      }
    }
  }

  @EventHandler(ignoreCancelled=true)
  public void onBlockBreak(BlockPlaceEvent e)
  {
    if (!KnockoutPlus.protectBlocks) return;
    Location loc = e.getBlock().getLocation();

    for (Corpse c : CorpseRegistry.getCorpses()) {
      Location l = c.getLocation();
      if ((l.getWorld().equals(loc.getWorld())) && 
        (l.distance(loc) <= 2.0D)) {
        e.setCancelled(true);
        e.getPlayer().sendMessage("§cSomeone is dying here! Have some respect!");
        return;
      }
    }
  }

  @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
  public void onPlayerDamage(EntityDamageEvent e) {
    if (!(e.getEntity() instanceof Player)) return;
    Player p = (Player)e.getEntity();

    if (p.getGameMode() == GameMode.CREATIVE) return;

    if ((e.getCause() == EntityDamageEvent.DamageCause.SUICIDE) || (e.getCause() == EntityDamageEvent.DamageCause.VOID)) {
      Corpse c = CorpseRegistry.getCorpse(p);
      if ((c != null) && 
        (e.getDamage() >= p.getHealth())) {
        c.unregister();
      }

      return;
    }

    if (CorpseRegistry.isKnockedOut(p)) {
      e.setCancelled(true);
      return;
    }

    if (KnockoutPlus.wasRecentlyKnockedOut(p)) return;
    if (!KnockoutPlus.nonMobsKO) return;

    if ((e.getCause() == EntityDamageEvent.DamageCause.LAVA) || (e.getCause() == EntityDamageEvent.DamageCause.WITHER)) {
      return;
    }

    if (((e instanceof EntityDamageByEntityEvent)) || (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) || (e.getCause() == EntityDamageEvent.DamageCause.PROJECTILE)) {
      return;
    }
    if (e.getDamage() < p.getHealth()) {
      return;
    }
    double trueDamage = e.getFinalDamage();
    if (trueDamage >= p.getHealth() - 0.1D) {
      this.plugin.koPlayer(p);
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void onPlayerLog(PlayerQuitEvent e)
  {
    Player p = e.getPlayer();
    Corpse c = CorpseRegistry.getCorpse(p);
    chants.remove(p.getUniqueId());

    if (c != null) {
      KnockoutPlus.removePlayer(p);
      c.unregister();
      p.setHealth(0.0D);
    }
  }

  @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
  public void onPlayerHit(EntityDamageByEntityEvent e)
  {
    if (!(e.getEntity() instanceof Player)) return;

    Player p = (Player)e.getEntity();

    if (p.getGameMode() == GameMode.CREATIVE) return;

    if (CorpseRegistry.isKnockedOut(p)) {
      e.setCancelled(true);
      return;
    }

    if (KnockoutPlus.wasRecentlyKnockedOut(p)) return;

    if (e.getDamage() < p.getHealth()) {
      return;
    }
    double trueDamage = e.getFinalDamage();
    if (trueDamage < p.getHealth() - 0.1D) return;

    Entity killer = e.getDamager();

    if ((killer instanceof Player)) {
      if (KnockoutPlus.playersKO) {
        Player k = (Player)killer;
        this.plugin.koPlayer(p, k);
        e.setCancelled(true);
      }
      return;
    }

    if ((killer instanceof Projectile)) {
      Projectile proj = (Projectile)killer;
      ProjectileSource shooter = proj.getShooter();
      if ((shooter instanceof Player)) {
        if (KnockoutPlus.playersKO) {
          Player k = (Player)shooter;
          this.plugin.koPlayer(p, k);
          e.setCancelled(true);
        }
        return;
      }
    }

    if (KnockoutPlus.mobsKO)
    {
      e.setCancelled(true);
      this.plugin.koPlayer(p);
    }
  }

  @EventHandler
  public void onPlayerDeath(PlayerDeathEvent event) {
    Player p = event.getEntity();
    KnockoutPlus.wakeOne(p);
    KnockoutPlus.recentKos.remove(event.getEntity().getUniqueId());
    event.setDeathMessage(null);

    Corpse c = CorpseRegistry.getCorpse(p);
    if (c != null) {
      this.plugin.getLogger().warning("A corpse was left unhandled on the death of player: " + p.getName());

      c.unregister();
    }
  }

  @EventHandler
  public void onIgnite(BlockIgniteEvent e) {
    if (!KnockoutPlus.protectBlocks) return;
    Location loc = e.getBlock().getLocation();
    for (Corpse c : CorpseRegistry.getCorpses()) {
      Location l = c.getLocation();
      if ((l.getWorld().equals(loc.getWorld())) && 
        (l.distance(loc) <= 2.0D)) {
        e.setCancelled(true);
        break;
      }
    }
  }

  @EventHandler(ignoreCancelled=true)
  public void onPush(BlockPistonExtendEvent e) {
    if (!KnockoutPlus.protectBlocks) return;
    Location loc = e.getBlock().getLocation();
    for (Corpse c : CorpseRegistry.getCorpses()) {
      Location l = c.getLocation();
      if ((l.getWorld().equals(loc.getWorld())) && 
        (l.distance(loc) <= 8.0D)) {
        e.setCancelled(true);
        break;
      }
    }
  }

  @EventHandler(ignoreCancelled=true)
  public void onPull(BlockPistonRetractEvent e) {
    if (!KnockoutPlus.protectBlocks) return;
    Location loc = e.getBlock().getLocation();
    for (Corpse c : CorpseRegistry.getCorpses()) {
      Location l = c.getLocation();
      if ((l.getWorld().equals(loc.getWorld())) && 
        (l.distance(loc) <= 3.0D)) {
        e.setCancelled(true);
        break;
      }
    }
  }

  public static Player getPlayer(UUID uuid) {
    for (Player p : Bukkit.getOnlinePlayers()) {
      if (p.getUniqueId().equals(uuid)) {
        return p;
      }
    }
    return null;
  }

  @EventHandler(ignoreCancelled=true)
  public void onInteract(PlayerInteractEvent e) {
    final Player p = e.getPlayer();

    interrupt(p);
    Location loc;
    if ((e.getAction() == Action.RIGHT_CLICK_BLOCK) && 
      (KnockoutPlus.protectBlocks)) {
      Material mat = e.getPlayer().getItemInHand().getType();
      if ((mat == Material.WATER_BUCKET) || (mat == Material.LAVA_BUCKET) || (mat == Material.FLINT_AND_STEEL))
      {
        loc = e.getClickedBlock().getLocation();

        for (Corpse c : CorpseRegistry.getCorpses()) {
          Location l = c.getLocation();
          if ((l.getWorld().equals(loc.getWorld())) && (l.distance(loc) <= 4.0D)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cSomeone is dying here! Have some respect!");
            return;
          }

        }

      }

    }

    if (e.getAction() == Action.PHYSICAL) {
      return;
    }

    if (verdictDelay.contains(p.getUniqueId())) return;

    for (Corpse c : CorpseRegistry.getVictims(p)) {
      Location l = c.getLocation();
      Location ploc = p.getLocation();
      if ((l.getWorld().equals(ploc.getWorld())) && (l.distance(ploc) <= 4.0D)) {
        final Player v = getPlayer(c.getVictim());

        verdictDelay.add(p.getUniqueId());

        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
          public void run() {
            KOListener.verdictDelay.remove(p.getUniqueId());
          }
        }
        , 25L);

        if ((e.getAction() == Action.LEFT_CLICK_BLOCK) || (e.getAction() == Action.LEFT_CLICK_AIR))
        {
          announceKill(p, v);
          p.sendMessage("§7§o(Hold still or your action will be interrupted.)");
          p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 40, 80, true));

          final Location chantSpot = p.getLocation();
          int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable()
          {
            public void run() {
              KOListener.chants.remove(p.getUniqueId());
              if (!CorpseRegistry.isKnockedOut(v)) return;

              if (p.getLocation().getWorld() == chantSpot.getWorld())
                if (p.getLocation().distance(chantSpot) > 0.2D) {
                  p.sendMessage(ChatColor.RED + "You have been interrupted!");
                } else {
                  PlayerExecuteEvent event = new PlayerExecuteEvent(p, v);
                  Bukkit.getPluginManager().callEvent(event);
                  if (event.isCancelled()) return;

                  KnockoutPlus.removePlayer(v);
                  CorpseRegistry.getCorpse(v).unregister();
                  v.damage(1.0D);
                  KnockoutPlus.wakeOne(p);
                  v.setHealth(0.0D);
                }
            }
          }
          , 40L);

          Integer oldTaskId = (Integer)chants.put(p.getUniqueId(), Integer.valueOf(taskId));
          if (oldTaskId == null) break;
          Bukkit.getScheduler().cancelTask(oldTaskId.intValue());

          break; } if ((e.getAction() != Action.RIGHT_CLICK_BLOCK) && (e.getAction() != Action.RIGHT_CLICK_AIR)) break;
        PlayerReviveEvent event = new PlayerReviveEvent(p, v, PlayerReviveEvent.Reason.MERCY);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        p.sendMessage("§6You have allowed " + this.plugin.giveName(v) + "§6 to live.");
        KnockoutPlus.revivePlayer(v, 4.0D);
        c.unregister();

        break;
      }
    }
  }

  private void interrupt(Player p) {
    if (chants.containsKey(p.getUniqueId())) {
      int taskId = ((Integer)chants.remove(p.getUniqueId())).intValue();

      Bukkit.getScheduler().cancelTask(taskId);
      if (p.hasPotionEffect(PotionEffectType.SLOW_DIGGING))
        p.removePotionEffect(PotionEffectType.SLOW_DIGGING);
      p.sendMessage(ChatColor.RED + "Your have been interrupted!");
    }
  }

  private void announceKill(Player p, Player v)
  {
    int dice = this.rnd.nextInt(20);
    String killMsg;
    switch (dice) { case 1:
      killMsg = "§6" + v.getDisplayName() + "§6 was no match for you."; break;
    case 2:
      killMsg = "§6Justice has been brought down upon " + v.getDisplayName() + "§6 this day."; break;
    case 3:
      killMsg = "§6The ground runs red with the blood of " + v.getDisplayName() + "§6."; break;
    case 4:
      killMsg = "§6It's all over for " + v.getDisplayName() + "§6."; break;
    case 5:
      killMsg = "§6" + v.getDisplayName() + "§6 will question your might no longer."; break;
    case 6:
      killMsg = "§6" + v.getDisplayName() + "§6 should've thought twice about challenging you."; break;
    case 7:
      killMsg = "§6It's a one-way trip to the Monks for " + v.getDisplayName(); break;
    case 8:
      killMsg = "§6" + v.getDisplayName() + "§6's weakness will taint the planes no more."; break;
    case 9:
      killMsg = "§6" + v.getDisplayName() + "§6 has met their superior today."; break;
    default:
      killMsg = "§6You have sentenced " + this.plugin.giveName(v) + "§6 to die.";
    }

    dice = this.rnd.nextInt(20);
    String deathMsg;
    switch (dice) { case 1:
      deathMsg = "§9The merciless " + this.plugin.giveName(p) + "§9 has stricken you down."; break;
    case 2:
      deathMsg = "§9The Monks will know that " + this.plugin.giveName(p) + "§9 has sent you."; break;
    case 3:
      deathMsg = "§c" + this.plugin.giveName(p) + "§c deemed you unfit to live."; break;
    case 4:
      deathMsg = "§c" + this.plugin.giveName(p) + "§c sees fit to end your misery."; break;
    case 5:
      deathMsg = "§c" + this.plugin.giveName(p) + "§c deals death this day."; break;
    case 6:
      deathMsg = "§c" + this.plugin.giveName(p) + "§c deals the final blow."; break;
    case 7:
      deathMsg = "§cWoe upon those that dare challenge " + this.plugin.giveName(p); break;
    case 8:
      deathMsg = "§cThe cold-blooded " + this.plugin.giveName(p) + "§c shall show you no mercy."; break;
    case 9:
      deathMsg = "§cThe hand of " + this.plugin.giveName(p) + "§c chose to sever your lifeline."; break;
    default:
      deathMsg = "§c" + this.plugin.giveName(p) + "§c has sentenced you to die.";
    }

    p.sendMessage(killMsg);
    v.sendMessage(deathMsg);
  }
}

/* Location:           C:\Users\Nick\Desktop\Minecraft\LOTC\LeadDev\plugins\KnockoutPlus.jar
 * Qualified Name:     io.github.archemedes.knockoutplus.KOListener
 * JD-Core Version:    0.6.2
 */