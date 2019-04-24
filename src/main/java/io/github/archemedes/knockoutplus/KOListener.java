package io.github.archemedes.knockoutplus;

import co.lotc.core.Tythan;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import io.github.archemedes.knockoutplus.corpse.Corpse;
import io.github.archemedes.knockoutplus.events.PlayerExecuteEvent;
import io.github.archemedes.knockoutplus.events.PlayerReviveEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class KOListener implements Listener {
    ArrayList<UUID> verdictDelay = new ArrayList<>();
    public HashMap<UUID, Integer> chants = new HashMap<>();
    private final Random rnd = new Random();
    private final KnockoutPlus plugin;

    private static final String CORPSE_PRESENT = "Someone's unconscious here. Why not give them a hand?";

    KOListener(KnockoutPlus plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
    }

    public Player getPlayer(UUID uuid) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().equals(uuid)) {
                return p;
            }
        }
        return null;
    }

    @EventHandler(ignoreCancelled = true)
    public void blockCommands(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();

        interrupt(p);

        if (plugin.getCorpseRegistry().isKnockedOut(p)) {
            String msg = e.getMessage();
            if ((!p.isOp()) &&
                    (!msg.startsWith("/tell ")) && (!msg.startsWith("/damage ")) && (!msg.startsWith("/modreq ")) && (!msg.startsWith("/check ")) &&
                    (!msg.startsWith("/t ")) && (!msg.startsWith("/d20")) && (!msg.startsWith("/d48")) && (!msg.startsWith("/msg ")) && (!msg.startsWith("/modlist ")) &&
                    (!msg.startsWith("/m ")) && (!msg.startsWith("/s ")) && (!msg.startsWith("/w ")) && (!msg.startsWith("/whisper ")) &&
                    (!msg.startsWith("/reply ")) && (!msg.startsWith("/r ")))
                e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if ((e.getTarget() instanceof Player)) {
            Player p = (Player) e.getTarget();
            if (plugin.getCorpseRegistry().isKnockedOut(p) && (plugin.mobsUntarget))
                e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (!plugin.protectBlocks) return;
        Location loc = e.getBlock().getLocation();

        for (Corpse c : plugin.getCorpseRegistry().getCorpses()) {
            Location l = c.getLocation();
            if ((l.getWorld().equals(loc.getWorld())) &&
                    (l.distance(loc) <= 2.0D)) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + CORPSE_PRESENT);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockPlaceEvent e) {
        if (!plugin.protectBlocks) return;
        Location loc = e.getBlock().getLocation();

        for (Corpse c : plugin.getCorpseRegistry().getCorpses()) {
            Location l = c.getLocation();
            if ((l.getWorld().equals(loc.getWorld())) &&
                    (l.distance(loc) <= 2.0D)) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + CORPSE_PRESENT);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();

        if (p.getGameMode() == GameMode.CREATIVE) return;

        if ((e.getCause() == EntityDamageEvent.DamageCause.SUICIDE) || (e.getCause() == EntityDamageEvent.DamageCause.VOID)) {
            if (plugin.getCorpseRegistry().isKnockedOut(p) && (e.getDamage() >= p.getHealth())) {
                plugin.getCorpseRegistry().getCorpse(p).unregister();
                p.damage(1.0D);
                plugin.wakeOne(p);
                p.setHealth(0.0D);
            }

            return;
        }

        if (plugin.getCorpseRegistry().isKnockedOut(p)) {
            e.setCancelled(true);
            return;
        }

        LocalPlayer lp = WorldGuardPlugin.inst().wrapPlayer(p);
        if (!plugin.nonMobsKO && !(getSet(lp).testState(plugin.getWgPlugin().wrapPlayer(p), plugin.getOTHER_KO())))
            return;

        if ((e.getCause() == EntityDamageEvent.DamageCause.LAVA) || (e.getCause() == EntityDamageEvent.DamageCause.WITHER)) {
            return;
        }

        if (((e instanceof EntityDamageByEntityEvent))
                || (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK)
                || (e.getCause() == EntityDamageEvent.DamageCause.PROJECTILE)
                || (e.getCause() == EntityDamageEvent.DamageCause.MAGIC)) {
            return;
        }
        
        if(holdingTotem(p))
        	return;
        
        if (e.getDamage() < p.getHealth()) {
            return;
        }
        double trueDamage = e.getFinalDamage();
        if (trueDamage >= p.getHealth() - 0.1D) {
            this.plugin.koPlayer(p);
            e.setCancelled(true);
        }
    }

    private boolean holdingTotem(Player p) {
    	PlayerInventory i = p.getInventory();
    	
    	return i.getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
    			|| i.getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING;
    }
    
    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerLog(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        Corpse c = plugin.getCorpseRegistry().getCorpse(p);
        chants.remove(p.getUniqueId());

        if (c != null) {
            plugin.removePlayer(p);
            c.unregister();
            p.setHealth(0.0D);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerHit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        if (p.getGameMode() == GameMode.CREATIVE) return;
        if (plugin.getCorpseRegistry().isKnockedOut(p)) {
            e.setCancelled(true);
            return;
        }

        if (e.getFinalDamage() < p.getHealth()) {
            return;
        }
        double trueDamage = e.getFinalDamage();
        
        if (trueDamage < p.getHealth() - 0.1D){
            return;
        }
        
        LocalPlayer lp = WorldGuardPlugin.inst().wrapPlayer(p);
        ApplicableRegionSet set = getSet(lp);
        Entity killer = e.getDamager();
        if ((killer instanceof Player || (killer instanceof Projectile && ((Projectile) killer).getShooter() instanceof Player))) {
            if (plugin.playersKO && set.testState(lp, plugin.getPLAYER_KO())) {
            	Player k = killer instanceof Projectile ? (Player) ((Projectile) killer).getShooter() : (Player) killer;
                this.plugin.koPlayer(p, k);
                e.setCancelled(true);
            }
            return;
        }
        
        if (!holdingTotem(p) && plugin.mobsKO && set.testState(lp, plugin.getMOB_KO())) {
            e.setCancelled(true);
            this.plugin.koPlayer(p, e.getDamager());
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        plugin.wakeOne(p);
        plugin.getRecentKos().remove(event.getEntity().getUniqueId());
        event.setDeathMessage(null);

        Corpse c = plugin.getCorpseRegistry().getCorpse(p);
        if (c != null) {
            this.plugin.getLogger().warning("A corpse was left unhandled on the death of player: " + p.getName());

            c.unregister();
        }
    }

    @EventHandler
    public void onIgnite(BlockIgniteEvent e) {
        if (!plugin.protectBlocks) return;
        Location loc = e.getBlock().getLocation();
        for (Corpse c : plugin.getCorpseRegistry().getCorpses()) {
            Location l = c.getLocation();
            if ((l.getWorld().equals(loc.getWorld())) &&
                    (l.distance(loc) <= 2.0D)) {
                e.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPush(BlockPistonExtendEvent e) {
        if (!plugin.protectBlocks) return;
        Location loc = e.getBlock().getLocation();
        for (Corpse c : plugin.getCorpseRegistry().getCorpses()) {
            Location l = c.getLocation();
            if ((l.getWorld().equals(loc.getWorld())) &&
                    (l.distance(loc) <= 8.0D)) {
                e.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPull(BlockPistonRetractEvent e) {
        if (!plugin.protectBlocks) return;
        Location loc = e.getBlock().getLocation();
        for (Corpse c : plugin.getCorpseRegistry().getCorpses()) {
            Location l = c.getLocation();
            if ((l.getWorld().equals(loc.getWorld())) &&
                    (l.distance(loc) <= 3.0D)) {
                e.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        final Player p = e.getPlayer();

        interrupt(p);
        Location loc;
        if ((e.getAction() == Action.RIGHT_CLICK_BLOCK) &&
                (plugin.protectBlocks)) {
            Material mat = e.getPlayer().getEquipment().getItemInMainHand().getType();
            if ((mat == Material.WATER_BUCKET) || (mat == Material.LAVA_BUCKET) || (mat == Material.FLINT_AND_STEEL)) {
                loc = e.getClickedBlock().getLocation();

                for (Corpse c : plugin.getCorpseRegistry().getCorpses()) {
                    Location l = c.getLocation();
                    if ((l.getWorld().equals(loc.getWorld())) && (l.distance(loc) <= 4.0D)) {
                        e.setCancelled(true);
                        e.getPlayer().sendMessage(ChatColor.RED + CORPSE_PRESENT);
                        return;
                    }

                }

            }

        }

        if (e.getAction() == Action.PHYSICAL) {
            return;
        }

        if (verdictDelay.contains(p.getUniqueId())) return;

        for (Corpse c : plugin.getCorpseRegistry().getVictims(p)) {
            Location l = c.getLocation();
            Location ploc = p.getLocation();
            if ((l.getWorld().equals(ploc.getWorld())) && (l.distance(ploc) <= 4.0D)) {
                final Player v = getPlayer(c.getVictim());

                verdictDelay.add(p.getUniqueId());

                Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> plugin.getKoListener().verdictDelay.remove(p.getUniqueId())
                    , 25L);

                if ((e.getAction() == Action.LEFT_CLICK_BLOCK) || (e.getAction() == Action.LEFT_CLICK_AIR)) {
                    announceKill(p, v);
                    p.sendMessage(String.valueOf(ChatColor.GOLD) + ChatColor.BOLD + "(Hold still or your action will be interrupted.)");
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 40, 80, true));

                    final Location chantSpot = p.getLocation();
                    int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
                                plugin.getKoListener().chants.remove(p.getUniqueId());
                                if (!plugin.getCorpseRegistry().isKnockedOut(v)) return;

                                if (p.getLocation().getWorld() == chantSpot.getWorld())
                                    if (p.getLocation().distance(chantSpot) > 0.2D) {
                                        p.sendMessage(ChatColor.RED + "You have been interrupted!");
                                    } else {
                                        PlayerExecuteEvent event = new PlayerExecuteEvent(p, v);
                                        Bukkit.getPluginManager().callEvent(event);
                                        if (event.isCancelled()) return;

                                        plugin.removePlayer(v);
                                        plugin.getCorpseRegistry().getCorpse(v).unregister();
                                        v.damage(1.0D, p);
                                        plugin.wakeOne(p);
                                        v.setHealth(0.0D);
                                        p.sendMessage(Tythan.get().chatBuilder()
                                                            .append(ChatColor.BLUE + "To request the victim's head, click on ")
                                                            .appendButton(ChatColor.GOLD + "Request Player Head", "/koplushead request " + v.getName())
                                                            .append(ChatColor.BLUE + " or run the command " + ChatColor.GOLD + "/koplushead request " + v.getName())
                                                            .build()
                                        );
                                        plugin.getHeadRequestRegistry().register(p, v);
                                    }
                            }
                            , 40L);

                    Integer oldTaskId = chants.put(p.getUniqueId(), taskId);
                    if (oldTaskId == null) break;
                    Bukkit.getScheduler().cancelTask(oldTaskId);

                    break;
                }
                if ((e.getAction() != Action.RIGHT_CLICK_BLOCK) && (e.getAction() != Action.RIGHT_CLICK_AIR)) {
                    break;
                }
                PlayerReviveEvent event = new PlayerReviveEvent(p, v, PlayerReviveEvent.Reason.MERCY);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) return;

                p.sendMessage(ChatColor.GOLD + "You have helped " + this.plugin.giveName(v) + ChatColor.GOLD + " back up.");
                plugin.revivePlayer(v, p, 4.0D);
                c.unregister();

                break;
            }
        }
    }

    private void interrupt(Player p) {
        if (chants.containsKey(p.getUniqueId())) {
            int taskId = chants.remove(p.getUniqueId());

            Bukkit.getScheduler().cancelTask(taskId);
            if (p.hasPotionEffect(PotionEffectType.SLOW_DIGGING))
                p.removePotionEffect(PotionEffectType.SLOW_DIGGING);
            p.sendMessage(ChatColor.RED + "You have been interrupted!");
        }
    }

    private void announceKill(Player p, Player v) {
        int dice = this.rnd.nextInt(20);
        String killMsg;
        switch (dice) {
            case 1:
                killMsg = ChatColor.GOLD + "" + v.getDisplayName() + ChatColor.GOLD + " was no match for you.";
                break;
            case 2:
                killMsg = ChatColor.GOLD + "Justice has been brought down upon " + v.getDisplayName() + ChatColor.GOLD + " this day.";
                break;
            case 3:
                killMsg = ChatColor.GOLD + "The ground runs red with the blood of " + v.getDisplayName() + ChatColor.GOLD + "";
                break;
            case 4:
                killMsg = ChatColor.GOLD + "It's all over for " + v.getDisplayName() + ChatColor.GOLD + "";
                break;
            case 5:
                killMsg = ChatColor.GOLD + "" + v.getDisplayName() + ChatColor.GOLD + " will question your might no longer.";
                break;
            case 6:
                killMsg = ChatColor.GOLD + "" + v.getDisplayName() + ChatColor.GOLD + " should've thought twice about challenging you.";
                break;
            case 7:
                killMsg = ChatColor.GOLD + "It's a one-way trip to Mevvet for " + v.getDisplayName();
                break;
            case 8:
                killMsg = ChatColor.GOLD + "" + v.getDisplayName() + ChatColor.GOLD + "'s weakness will taint the planes no more.";
                break;
            case 9:
                killMsg = ChatColor.GOLD + "" + v.getDisplayName() + ChatColor.GOLD + " has met their superior today.";
                break;
            default:
                killMsg = ChatColor.GOLD + "You have sentenced " + this.plugin.giveName(v) + ChatColor.GOLD + " to die.";
        }

        dice = this.rnd.nextInt(20);
        String deathMsg;
        switch (dice) {
            case 1:
                deathMsg = ChatColor.BLUE + "The merciless " + this.plugin.giveName(p) + ChatColor.BLUE + " has stricken you down.";
                break;
            case 2:
                deathMsg = ChatColor.BLUE + "The Gods will know that " + this.plugin.giveName(p) + ChatColor.BLUE + " has sent you.";
                break;
            case 3:
                deathMsg = ChatColor.RED + "" + this.plugin.giveName(p) + ChatColor.RED + " deemed you unfit to live.";
                break;
            case 4:
                deathMsg = ChatColor.RED + "" + this.plugin.giveName(p) + ChatColor.RED + " sees fit to end your misery.";
                break;
            case 5:
                deathMsg = ChatColor.RED + "" + this.plugin.giveName(p) + ChatColor.RED + " deals death this day.";
                break;
            case 6:
                deathMsg = ChatColor.RED + "" + this.plugin.giveName(p) + ChatColor.RED + " deals the final blow.";
                break;
            case 7:
                deathMsg = ChatColor.RED + "Woe upon those that dare challenge " + this.plugin.giveName(p);
                break;
            case 8:
                deathMsg = ChatColor.RED + "The cold-blooded " + this.plugin.giveName(p) + ChatColor.RED + " shall show you no mercy.";
                break;
            case 9:
                deathMsg = ChatColor.RED + "The hand of " + this.plugin.giveName(p) + ChatColor.RED + " chose to sever your lifeline.";
                break;
            default:
                deathMsg = ChatColor.RED + "" + this.plugin.giveName(p) + ChatColor.RED + " has sentenced you to die.";
        }

        p.sendMessage(killMsg);
        v.sendMessage(deathMsg);
    }

    @EventHandler
    public void entityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();

            if (plugin.getCorpseRegistry().isKnockedOut(player)) {
                event.setCancelled(true);
            }
        }
    }


    private ApplicableRegionSet getSet(LocalPlayer player) {
        return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().getApplicableRegions(player.getLocation());
    }
}