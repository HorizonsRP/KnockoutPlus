package io.github.archemedes.knockoutplus;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.google.common.collect.Lists;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import io.github.archemedes.knockoutplus.corpse.BleedoutTimer;
import io.github.archemedes.knockoutplus.corpse.Corpse;
import io.github.archemedes.knockoutplus.corpse.CorpseRegistry;
import io.github.archemedes.knockoutplus.events.PlayerReviveEvent;
import lombok.Getter;
import net.lordofthecraft.arche.attributes.ArcheAttribute;
import net.lordofthecraft.arche.attributes.AttributeRegistry;
import net.minecraft.server.v1_12_R1.PacketPlayOutEntity;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

@Getter
public final class KnockoutPlus extends JavaPlugin {
		private ArcheAttribute bleedoutAttribute;
	
    public int bleedoutTime;
    public boolean mobsUntarget;
    public boolean playersKO;
    public boolean mobsKO;
    public boolean nonMobsKO;
    public boolean protectBlocks;
    private Map<UUID, Long> recentKos = new HashMap<>();
    private ProtocolManager protocol;

    private WorldGuardPlugin worldGuardPlugin;
    private CorpseRegistry corpseRegistry;
    private KOListener koListener;
    private BukkitTask bleedoutTask;

    private final StateFlag PLAYER_KO = new StateFlag("player-knockout", true);
    private final StateFlag MOB_KO = new StateFlag("mob-knockout", true);
    private final StateFlag OTHER_KO = new StateFlag("environment-knockout", true);

    public void onEnable() {
        worldGuardPlugin = WGBukkit.getPlugin();
        FlagRegistry registry = worldGuardPlugin.getFlagRegistry();
        try {
            registry.register(PLAYER_KO);
            registry.register(MOB_KO);
            registry.register(OTHER_KO);
        } catch (FlagConflictException e) {
            getLogger().severe("There's a flag conflict!");
        } catch (IllegalStateException e) {
            getLogger().warning("Failed to register flags. You shouldn't be reloading this plugin!");
        }

        corpseRegistry = new CorpseRegistry(this);
        koListener = new KOListener(this);
        saveDefaultConfig();

        bleedoutTask = new BleedoutTimer(this).runTaskTimer(this, 0L, 133L);

        bleedoutTime = getConfig().getInt("bleedout.time");
        mobsUntarget = getConfig().getBoolean("mobs.untarget.knockout");
        playersKO = getConfig().getBoolean("players.cause.knockout");
        mobsKO = getConfig().getBoolean("mobs.cause.knockout");
        nonMobsKO = getConfig().getBoolean("nonmobs.cause.knockout");
        protectBlocks = getConfig().getBoolean("protect.ko.blocks");

        bleedoutAttribute = new ArcheAttribute("Bleedout Time", bleedoutTime);
        AttributeRegistry.getInstance().register(bleedoutAttribute);
        
        
        protocol = ProtocolLibrary.getProtocolManager();

        protocol.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                final int id = packet.getIntegers().read(0);


                for (final Corpse c : corpseRegistry.getCorpses())
                    if (c.getEntityId() == id) {
                        final List<Player> t = Lists.newArrayList(event.getPlayer());
                        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
                                    Location l = c.getLocation().add(0, 1, 0);
                                    sendBedPacket(getServer().getPlayer(c.getVictim()), l, t);
                                }
                                , 2L);

                        break;
                    }
            }
        });
    }

    public void onDisable() {
        for (Corpse c : corpseRegistry.getCorpses()) {
            Player p = koListener.getPlayer(c.getVictim());
            p.sendMessage(ChatColor.RED + "An evil entity has condemned you.");
            wake(p, null, false);
            removePlayer(p);
            p.damage(1.0D);
            p.setHealth(0.0D);
        }
        bleedoutTask.cancel();
    }


    @SuppressWarnings("deprecation")
    public void removePlayer(Player p) {
        Corpse c = corpseRegistry.getCorpse(p);
        Location l = c.getLocation();
        p.sendBlockChange(l, l.getBlock().getType(), l.getBlock().getData());
    }

    boolean wasRecentlyKnockedOut(Player p) {
        UUID u = p.getUniqueId();
        if (recentKos.containsKey(u)) {
            long time = System.currentTimeMillis();
            if (time < recentKos.get(u) + 600000L) return true;
        }

        return false;
    }

    @SuppressWarnings("deprecation")
    void wake(Player v, Location l, boolean updateBlock) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ANIMATION);
        packet.getIntegers().write(0, v.getEntityId()).write(1, 2);
        protocol.broadcastServerPacket(packet, v, true);
        if (updateBlock) {
            for (Player t : v.getWorld().getPlayers()) {
                t.sendBlockChange(new Location(l.getWorld(), l.getBlockX(), 0, l.getBlockZ()), Material.BEDROCK, (byte) 0);
            }
        }

    }

    @SuppressWarnings("deprecation")
    public void wakeOne(Player v) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ANIMATION);
        packet.getIntegers().write(0, v.getEntityId()).write(1, 2);
        Location l = v.getLocation();
        try {
            v.sendBlockChange(new Location(l.getWorld(), l.getBlockX(), 0, l.getBlockZ()), Material.BEDROCK, (byte) 0);
            protocol.sendServerPacket(v, packet);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void revivePlayer(Player player, double hp) {
        wake(player, player.getLocation(), true);
        removePlayer(player);

        player.setHealth(Math.min(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), hp));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 400, 5, true), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 400, 1, true), true);

        Location l = player.getLocation();
        if (l.getBlock().getType().equals(Material.WATER) || l.getBlock().getType().equals(Material.STATIONARY_WATER)) { //adds waterbreathing if player dies in water
        	player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 400, 100));
        }
        player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation(), 1);
    }

    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("damage")) {
            if (!(sender instanceof Player))
                return true;
            if (args.length != 1) {
                return false;
            }
            Player p = (Player) sender;
            int slot;
            try {
                slot = Math.abs(Integer.parseInt(args[0]));
            } catch (NumberFormatException e) {
                return false;
            }
            if (slot > 5) {
                if (slot >= p.getHealth()) {
                    EntityDamageEvent event = new EntityDamageEvent(p, EntityDamageEvent.DamageCause.SUICIDE, slot);
                    Bukkit.getPluginManager().callEvent(event);
                    p.damage(event.getDamage());
                } else {
                    EntityDamageEvent event = new EntityDamageEvent(p, EntityDamageEvent.DamageCause.CUSTOM, slot);
                    Bukkit.getPluginManager().callEvent(event);
                    p.damage(event.getDamage());
                }
            } else {
                if (p.getGameMode() == GameMode.SURVIVAL) {
                    if (slot >= p.getHealth()) {
                        EntityDamageEvent event = new EntityDamageEvent(p, EntityDamageEvent.DamageCause.SUICIDE, slot);
                        Bukkit.getPluginManager().callEvent(event);
                        p.damage(event.getDamage());
                    } else {
                        p.setHealth(p.getHealth() - slot);
                    }
                } else {
                    p.sendMessage("You have to be in survival to do this.");
                    return true;
                }
            }

            return true;
        }
        if (cmd.getName().equalsIgnoreCase("d20")) {
            if (!(sender instanceof Player))
                return true;
            if (args.length != 0)
                return false;
            Player p = (Player) sender;

            EntityDamageEvent event = new EntityDamageEvent(p, EntityDamageEvent.DamageCause.SUICIDE, 1000.0D);
            Bukkit.getPluginManager().callEvent(event);
            p.damage(event.getDamage());

            return true;
        }
        final Player player;
        if (cmd.getName().equalsIgnoreCase("revive")) {
            if (args.length < 1)
                return false;
            final Player target = Bukkit.getServer().getPlayer(args[0]);

            if (target == null) {
                sender.sendMessage(ChatColor.RED + "" + args[0] + " is not online!");
                return false;
            }

            if (!corpseRegistry.isKnockedOut(target)) {
                sender.sendMessage(ChatColor.RED + "" + args[0] + " cannot be helped.");
                return true;
            }


            final Corpse corpse = corpseRegistry.getCorpse(target);
            
            if(!corpse.allowedToRevive(sender)) { //Putting this req here cockblocks GMs
            	sender.sendMessage(ChatColor.RED + "" + args[0] + " cannot be helped by you.");
              return true;
            }
            
            Player killer = koListener.getPlayer(corpse.getKiller());
            if(!(sender instanceof Player) || ( Arrays.stream(args).anyMatch(x->StringUtils.equalsAny(x, "gm","-gm")) && sender.hasPermission("archecore.mod"))) {
                PlayerReviveEvent event = new PlayerReviveEvent(null, target, PlayerReviveEvent.Reason.OPERATOR);
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    revivePlayer(target, 4.0D);
                    corpse.unregister();
                }
                return true;
            }

            player = (Player) sender;

            if (corpseRegistry.isKnockedOut(player)) {
                sender.sendMessage(ChatColor.RED + "You are knocked out!");
                return true;
            }

            if (player.equals(target)) {
                sender.sendMessage(ChatColor.RED + "You cannot revive yourself!");
                return true;
            }

            if (player.equals(killer)) {
                sender.sendMessage(ChatColor.RED + "Use a right click on this player instead!");
                return true;
            }

            if (!player.getLocation().getWorld().equals(target.getLocation().getWorld())) {
                sender.sendMessage(ChatColor.RED + "" + args[0] + " cannot be helped!");
                return true;
            }
            if (player.getLocation().distance(target.getLocation()) > 20.0D) {
                sender.sendMessage(ChatColor.RED + "" + args[0] + " cannot be helped!");
                return true;
            }
            if (player.getLocation().distance(target.getLocation()) > 3.0D) {
                sender.sendMessage(ChatColor.AQUA + "You must move closer to help them.");
                return true;
            }

            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CLOTH_BREAK, 3.5F, -1.0F);
            player.sendMessage(ChatColor.YELLOW + "You bend down to try and assist " + giveName(target));
            player.sendMessage(String.valueOf(ChatColor.GRAY) + ChatColor.BOLD + "(Hold still or your action will be interrupted.)");
            target.sendMessage(ChatColor.YELLOW + "You are being assisted by " + giveName(player));

            final Location chantSpot = player.getLocation();

            int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                        koListener.chants.remove(player.getUniqueId());
                        if (!corpseRegistry.isKnockedOut(target)) return;

                        if (player.getLocation().getWorld() == chantSpot.getWorld())
                            if (player.getLocation().distance(chantSpot) > 0.2D) {
                                player.sendMessage(ChatColor.RED + "You have been interrupted!");
                            } else {
                                PlayerReviveEvent event = new PlayerReviveEvent(player, target, PlayerReviveEvent.Reason.COMMAND_REVIVE);
                                Bukkit.getPluginManager().callEvent(event);
                                if (event.isCancelled()) return;

                                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1.2F, 1.0F);
                                player.sendMessage(ChatColor.GOLD + "You have saved " + giveName(target) + ChatColor.GOLD + " from a grisly fate.");
                                target.sendMessage(ChatColor.GOLD + "You have been saved, but you still feel weak");
                                target.sendMessage(ChatColor.DARK_RED + "Caution: Being incapacitated again shall mean your demise.");

                                revivePlayer(target, 4.0D);
                                corpse.unregister();

                                recentKos.put(target.getUniqueId(), System.currentTimeMillis());
                            }
                    }
                    , 100L);

            Integer oldTaskId = koListener.chants.put(player.getUniqueId(), taskId);
            if (oldTaskId != null) {
                Bukkit.getScheduler().cancelTask(oldTaskId);
            }
            return true;
        } else if (cmd.getName().equalsIgnoreCase("reviveall")) {
            if ((sender.hasPermission("nexus.moderator")) || (!(sender instanceof Player))) corpseRegistry.reviveAll();
            else
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this!");
            return true;
        } else if (cmd.getName().equalsIgnoreCase("togglerevive")) {
            if ((args.length == 1) && ((sender.hasPermission("nexus.moderator")))) {
                if (args[0].equalsIgnoreCase("players")) {
                    playersKO = !playersKO;
                    sender.sendMessage("Player knockout toggled to: " + ChatColor.AQUA + String.valueOf(nonMobsKO));
                } else if (args[0].equalsIgnoreCase("mobs")) {
                    mobsKO = !mobsKO;
                    sender.sendMessage("Mob knockout toggled to: " + ChatColor.AQUA + String.valueOf(nonMobsKO));
                } else if (args[0].equalsIgnoreCase("environment")) {
                    nonMobsKO = !nonMobsKO;
                    sender.sendMessage("Environment knockout toggled to: " + ChatColor.AQUA + String.valueOf(nonMobsKO));
                } else if (args[0].equalsIgnoreCase("all")) {
                    playersKO = !playersKO;
                    mobsKO = !mobsKO;
                    nonMobsKO = !nonMobsKO;
                    sender.sendMessage("All forms of Knockout have been toggled.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /togglerevive [players/mobs/environment]");
                }
            }
        } else if (cmd.getName().equalsIgnoreCase("knockout")) {
            if (args[0].equalsIgnoreCase("list")) {
                String gr = ChatColor.GRAY + "";
                String it = ChatColor.ITALIC + "";
                for (Corpse c : corpseRegistry.getCorpses()) {
                    sender.sendMessage(gr + it + c.getVictim() + gr + " killed by " + it + c.getKiller() + gr + " at " + it + c.getLocation());
                }

            } else if (args[0].equalsIgnoreCase("status")) {
                sender.sendMessage("Players: " + playersKO);
                sender.sendMessage("Mobs: " + mobsKO);
                sender.sendMessage("Environment: " + nonMobsKO);
            }
        }

        return false;
    }

    void koPlayer(Player p) {
        p.sendMessage(ChatColor.RED + "You have been knocked out and will die if not aided!");
        p.sendMessage(String.valueOf(ChatColor.DARK_GREEN));

        if (p.getFoodLevel() < 3) {
            p.setFoodLevel(3);
        }
        Location l = layPlayerDown(p);
        corpseRegistry.register(p, l);
        if (mobsUntarget) stopTarget(p);
    }

    void koPlayer(Player p, final Player k) {
        p.sendMessage(ChatColor.RED + "You were defeated by " + ChatColor.BOLD + k.getDisplayName());

        k.sendMessage(ChatColor.GOLD + "You have defeated " + ChatColor.BOLD + p.getDisplayName());
        k.sendMessage(String.valueOf(ChatColor.BLUE) + ChatColor.BOLD + "RIGHT CLICK to show mercy, or LEFT CLICK to send them to the Monks.");

        koListener.verdictDelay.add(k.getUniqueId());
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> koListener.verdictDelay.remove(k.getUniqueId())
                , 50L);

        if (mobsUntarget) stopTarget(p);
        Location l = layPlayerDown(p);

        corpseRegistry.register(p, k, l);
    }

    private void stopTarget(Player p) {
        p.getNearbyEntities(12.0D, 5.0D, 12.0D).stream().filter(e -> (e instanceof Creature)).forEach(e -> {
            Creature c = (Creature) e;
            if (c.getTarget() == p) c.setTarget(null);
        });
    }

    private Location layPlayerDown(final Player p) {
        p.setBedSpawnLocation(null);
        p.leaveVehicle();
        p.setFireTicks(0);
        p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

        p.setVelocity(new Vector(0, 0, 0));

        final Block b = p.getLocation().getBlock();
        Location l = b.getLocation().getBlock().getLocation();

        int height = 0;
        while (l.getBlock().getRelative(BlockFace.DOWN).isEmpty()
                && l.getBlockY() > 0) {
            l.add(0.0D, -1.0D, 0.0D);
            height++;
        }
        while (l.getBlock().isLiquid()) {
            l.add(0.0D, 1.0D, 0.0D);
        }
        if (!b.getRelative(BlockFace.NORTH).isEmpty()) {
            if (b.getRelative(BlockFace.SOUTH).isEmpty()) {
                l.add(0.0D, 0.0D, 1.0D);
            } else if (b.getRelative(BlockFace.WEST).isEmpty()) {
                if ((b.getRelative(BlockFace.NORTH_WEST).isEmpty()) &&
                        (!b.getRelative(-1, -1, -1).isEmpty()) && (!b.getRelative(-1, -1, 0).isEmpty()))
                    l.add(-1.0D, 0.0D, 0.0D);
                else if ((b.getRelative(BlockFace.SOUTH_WEST).isEmpty()) &&
                        (!b.getRelative(-1, -1, 0).isEmpty()) && (!b.getRelative(-1, -1, 1).isEmpty()))
                    l.add(-1.0D, 0.0D, 1.0D);
            } else if (b.getRelative(BlockFace.EAST).isEmpty()) {
                if ((b.getRelative(BlockFace.NORTH_EAST).isEmpty()) &&
                        (!b.getRelative(1, -1, -1).isEmpty()) && (!b.getRelative(1, -1, 0).isEmpty()))
                    l.add(1.0D, 0.0D, 0.0D);
                else if ((b.getRelative(BlockFace.SOUTH_EAST).isEmpty()) &&
                        (!b.getRelative(1, -1, 0).isEmpty()) && (!b.getRelative(1, -1, 1).isEmpty())) {
                    l.add(1.0D, 0.0D, 1.0D);
                }

            }

        }

        if (height > 4) {
            p.teleport(l);
        }

        final Location bl = l.add(0.0D, 1.0D, 0.0D);


        sendBedPacket(p, bl, p.getWorld().getPlayers());


        return l;
    }

    @SuppressWarnings("deprecation")
    private void sendBedPacket(Player p, Location l, List<Player> targets) {
        for (Player t : targets) {
            t.sendBlockChange(new Location(l.getWorld(), l.getBlockX(), 0, l.getBlockZ()), Material.BED_BLOCK, (byte) 0);
        }

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.BED);
        packet.getIntegers().write(0, p.getEntityId());
        packet.getBlockPositionModifier().write(0, new BlockPosition(l.getBlockX(), 0, l.getBlockZ()));
        protocol.broadcastServerPacket(packet);
        final PacketPlayOutEntity.PacketPlayOutRelEntityMove move = new PacketPlayOutEntity.PacketPlayOutRelEntityMove(p.getEntityId(), (byte) 0, (byte) (l.getBlockY() + 1), (byte) 0, false);
        for (Player t : targets) {
            if (p != t)
                ((CraftPlayer) t).getHandle().playerConnection.sendPacket(move);
        }
    }

    String giveName(Player p) {
        return p.getDisplayName() + ChatColor.GRAY + ChatColor.ITALIC + " (" + p.getName() + ")" + ChatColor.RESET;
    }

    public CorpseRegistry getCorpseRegistry(){
        return this.corpseRegistry;
    }

    public WorldGuardPlugin getWorldGuardPlugin(){
        return this.worldGuardPlugin;
    }

    public StateFlag getOTHER_KO(){
        return this.OTHER_KO;
    }

    public StateFlag getPLAYER_KO(){
        return this.PLAYER_KO;
    }

    public StateFlag getMOB_KO(){
        return this.MOB_KO;
    }

    public Map<UUID,Long> getRecentKos(){
        return this.recentKos;
    }

    public KOListener getKoListener(){
        return this.koListener;
    }

}