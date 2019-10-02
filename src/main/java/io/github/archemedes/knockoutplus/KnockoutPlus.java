package io.github.archemedes.knockoutplus;

import co.lotc.core.bukkit.util.ChatBuilder;
import co.lotc.core.bukkit.util.ItemUtil;
import co.lotc.core.bukkit.command.Commands;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import io.github.archemedes.knockoutplus.commands.KnockoutPlusCommand;
import io.github.archemedes.knockoutplus.commands.ReviveCommand;
import io.github.archemedes.knockoutplus.corpse.BleedoutTimer;
import io.github.archemedes.knockoutplus.corpse.Corpse;
import io.github.archemedes.knockoutplus.corpse.CorpseRegistry;
import io.github.archemedes.knockoutplus.corpse.HeadRequestRegistry;
import io.github.archemedes.knockoutplus.utils.PacketUtils;
import io.github.archemedes.knockoutplus.utils.WorldGuardUtils;
import lombok.Getter;
import net.lordofthecraft.arche.ArcheCore;
import net.lordofthecraft.arche.attributes.ArcheAttribute;
import net.lordofthecraft.arche.attributes.AttributeRegistry;
import net.lordofthecraft.omniscience.api.OmniApi;
import net.lordofthecraft.omniscience.api.data.DataWrapper;
import net.lordofthecraft.omniscience.api.entry.OEntry;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static net.lordofthecraft.omniscience.api.data.DataKeys.TARGET;

@Getter
public final class KnockoutPlus extends JavaPlugin {
    private static KnockoutPlus INSTANCE;
    public int bleedoutTime;
    public boolean mobsUntarget;
    public boolean playersKO;
    public boolean mobsKO;
    public boolean nonMobsKO;
    public boolean protectBlocks;
    private ArcheAttribute bleedoutAttribute;
    private Map<UUID, Long> recentKos = new HashMap<>();
    private ProtocolManager protocolManager;

    private CorpseRegistry corpseRegistry;
    private HeadRequestRegistry headRequestRegistry;
    private KOListener koListener;
    private BukkitTask bleedoutTask;

    private boolean worldGuardEnabled = false;

    public static KnockoutPlus get() {
        return INSTANCE;
    }

    public static boolean isAllowed(Player p, String flagName) {
        return KnockoutPlus.get().worldGuardEnabled && WorldGuardUtils.isAllowed(p, flagName);
    }

    @Override
    public void onLoad() {
        INSTANCE = this;

        try {
            WorldGuardUtils.init();
            worldGuardEnabled = true;
        } catch (NoClassDefFoundError ignored) {
            worldGuardEnabled = false;
        }
    }

    @Override
    public void onEnable() {
        Commands.build(getCommand("revive"), () -> new ReviveCommand(this));
        Commands.build(getCommand("knockoutplus"), () -> new KnockoutPlusCommand(this));

        if (getServer().getPluginManager().isPluginEnabled("Omniscience")) {
            OmniApi.registerEvent("down", "downed");
            OmniApi.registerEvent("revive", "revived");
            OmniApi.registerEvent("decapitate", "decapitated");
        }

        corpseRegistry = new CorpseRegistry(this);
        headRequestRegistry = new HeadRequestRegistry(this);
        koListener = new KOListener(this);
        saveDefaultConfig();

        bleedoutTask = new BleedoutTimer(this).runTaskTimer(this, 0L, 133L);

        bleedoutTime = getConfig().getInt("bleedout.time");
        mobsUntarget = getConfig().getBoolean("mobs.untarget.knockout");
        playersKO = getConfig().getBoolean("players.cause.knockout");
        mobsKO = getConfig().getBoolean("mobs.cause.knockout");
        nonMobsKO = getConfig().getBoolean("nonmobs.cause.knockout");
        protectBlocks = getConfig().getBoolean("protect.ko.blocks");

        bleedoutAttribute = new ArcheAttribute("bleedout-time", bleedoutTime);
        if (AttributeRegistry.getInstance().getAttribute(bleedoutAttribute.getName()) == null)
            AttributeRegistry.getInstance().register(bleedoutAttribute);

        protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.removePacketListeners(this);

        protocolManager.addPacketListener(new PacketAdapter(this, PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                final int id = packet.getIntegers().read(0);


                for (final Corpse c : corpseRegistry.getCorpses())
                    if (c.getEntityId() == id) {
                        Player p = getServer().getPlayer(c.getVictim());
                        if (p != null) {
                            Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> {
                                        Location l = c.getLocation().add(0, 1, 0);
                                        PacketUtils.layDown(p, l);
                                    }
                                    , 2L);

                            break;
                        }
                    }
            }
        });
    }

    @Override
    public void onDisable() {
        for (Corpse c : corpseRegistry.getCorpses()) {
            Player p = koListener.getPlayer(c.getVictim());
            p.sendMessage(ChatColor.RED + "An evil entity has condemned you.");
            wake(p, p.getLocation(), false);
            removePlayer(p);
            p.damage(1.0D);
            p.setHealth(0.0D);
        }
        bleedoutTask.cancel();
    }

    public void removePlayer(Player p) {
        Corpse c = corpseRegistry.getCorpse(p);
        Location l = c.getLocation();
        p.sendBlockChange(l, l.getBlock().getBlockData());
    }

    boolean wasRecentlyKnockedOut(Player p) {
        UUID u = p.getUniqueId();
        if (recentKos.containsKey(u)) {
            long time = System.currentTimeMillis();
            return time < recentKos.get(u) + 600000L;
        }

        return false;
    }

    public void wake(Player v, Location l, boolean updateBlock) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.ANIMATION);
        packet.getIntegers().write(0, v.getEntityId()).write(1, 2);
        protocolManager.broadcastServerPacket(packet, v, true);

        if (updateBlock) {
            for (Player t : v.getWorld().getPlayers()) {
                t.sendBlockChange(new Location(l.getWorld(), l.getBlockX(), 0, l.getBlockZ()), Material.BEDROCK.createBlockData());
            }
        }

    }

    /**
     * Revived a player from the knockdown state
     *
     * @param player Player being revived
     * @param helper Person doing the reviving
     * @param hp     Amount of health to revive the player with
     */
    public void revivePlayer(Player player, CommandSender helper, double hp) {
        wake(player, player.getLocation(), true);
        removePlayer(player);

        player.setHealth(Math.min(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue(), hp));
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 400, 5, true), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 400, 1, true), true);

        Location l = player.getLocation();
        if (l.getBlock().isLiquid()) { //adds water breathing if player dies in some form of liquid
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 400, 100));
        }
        player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation(), 1);

        if (getServer().getPluginManager().isPluginEnabled("Omniscience")) {
            DataWrapper wrapper = DataWrapper.createNew();
            wrapper.set(TARGET, player.getName());
            OEntry.create().source(helper).customWithLocation("revive", wrapper, player.getLocation()).save();
        }
    }

    @Override
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
        if (cmd.getName().equalsIgnoreCase("reviveall")) {
            if ((sender.hasPermission("nexus.moderator")) || (!(sender instanceof Player)))
                corpseRegistry.reviveAll(sender);
            else
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this!");
            return true;
        } else if (cmd.getName().equalsIgnoreCase("togglerevive")) {
            if ((args.length == 1) && ((sender.hasPermission("nexus.moderator")))) {
                if (args[0].equalsIgnoreCase("players")) {
                    playersKO = !playersKO;
                    sender.sendMessage("Player knockout toggled to: " + ChatColor.AQUA + nonMobsKO);
                } else if (args[0].equalsIgnoreCase("mobs")) {
                    mobsKO = !mobsKO;
                    sender.sendMessage("Mob knockout toggled to: " + ChatColor.AQUA + nonMobsKO);
                } else if (args[0].equalsIgnoreCase("environment")) {
                    nonMobsKO = !nonMobsKO;
                    sender.sendMessage("Environment knockout toggled to: " + ChatColor.AQUA + nonMobsKO);
                } else if (args[0].equalsIgnoreCase("all")) {
                    playersKO = !playersKO;
                    mobsKO = !mobsKO;
                    nonMobsKO = !nonMobsKO;
                    sender.sendMessage("All forms of Knockout have been toggled.");
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /togglerevive [players/mobs/environment]");
                }
            }
        } else if (cmd.getLabel().equalsIgnoreCase("knockout")) {
            if (args.length < 1) return false;
            if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("l")) {
                sender.sendMessage(ChatColor.YELLOW + "Current knockouts: ");
                String gr = ChatColor.GRAY + "";
                String it = ChatColor.ITALIC + "";
                for (Corpse c : corpseRegistry.getCorpses()) {
                    sender.sendMessage(gr + it + this.getServer().getOfflinePlayer(c.getVictim()).getName()
                            + gr + " killed by " + it + this.getServer().getOfflinePlayer(c.getKiller()).getName()
                            + gr + " at " + it + c.getLocation().getWorld() + ": " + c.getLocation().getBlockX() + ", " + c.getLocation().getBlockY() + ", " + c.getLocation().getBlockZ());
                }
                if (corpseRegistry.getCorpses().size() < 1) sender.sendMessage(gr + it + "Nobody");
                return true;

            } else if (args[0].equalsIgnoreCase("status")) {
                sender.sendMessage(ChatColor.YELLOW + "Players: " + (playersKO ? ChatColor.GREEN : ChatColor.RED) + playersKO);
                sender.sendMessage(ChatColor.YELLOW + "Mobs: " + (mobsKO ? ChatColor.GREEN : ChatColor.RED) + mobsKO);
                sender.sendMessage(ChatColor.YELLOW + "Environment: " + (nonMobsKO ? ChatColor.GREEN : ChatColor.RED) + nonMobsKO);
                return true;
            }
        } else if (cmd.getName().equalsIgnoreCase("koplushead")) {
            if (!(sender instanceof Player))
                sender.sendMessage(ChatColor.RED + "This command is only useful to players.");
            else {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /koplushead [request/send] [player]");
                } else if (Bukkit.getPlayer(args[1]) == null) {
                    sender.sendMessage(ChatColor.RED + "Player is offline or doesn't exist.");
                } else if (args[0].equalsIgnoreCase("request")) {
                    headRequestRegistry.requestHead((Player) sender, Bukkit.getPlayer(args[1]));
                } else if (args[0].equalsIgnoreCase("send")) {
                    Player winner = Bukkit.getPlayer(args[1]);
                    if (headRequestRegistry.sendHead(winner, (Player) sender)) {
                        if (getServer().getPluginManager().isPluginEnabled("Omniscience")) {
                            DataWrapper wrapper = DataWrapper.createNew();
                            wrapper.set(TARGET, sender.getName());
                            OEntry.create().source(winner).custom("decapitate", wrapper).save();
                        }
                    }
                }
            }
            return true;
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
        logKill(p, null);
    }

    void koPlayer(Player p, Entity killer) {
        p.sendMessage(ChatColor.RED + "You have been knocked out and will die if not aided!");
        p.sendMessage(String.valueOf(ChatColor.DARK_GREEN));

        if (p.getFoodLevel() < 3) {
            p.setFoodLevel(3);
        }
        Location l = layPlayerDown(p);
        corpseRegistry.register(p, l);
        if (mobsUntarget) stopTarget(p);
        logKill(p, killer);
    }

    void koPlayer(Player p, final Player killer) {
        if(ArcheCore.getPersona(killer) == null)
            return;

        ItemStack weapon = killer.getInventory().getItemInMainHand();
        ChatBuilder chatBuilder = new ChatBuilder("");
        chatBuilder.append("You were defeated by ").color(ChatColor.BOLD).color(ChatColor.RED)
                .append(ArcheCore.getPersona(killer).getName()).color(ChatColor.GOLD).hover(killer.getName())
                .append(" using ").color(ChatColor.RED);
        if(weapon.getType() != Material.AIR)
            chatBuilder.append("[").color(ChatColor.RED)
                    .append(ChatColor.RESET + ChatColor.stripColor(ItemUtil.getDisplayName(weapon))).hoverItem(weapon)
                    .append("]").color(ChatColor.RED);
        else
            chatBuilder.append("their fists!");
        chatBuilder.send(p);

        killer.sendMessage(ChatColor.GOLD + "You have defeated " + ChatColor.BOLD + p.getDisplayName());
        killer.sendMessage(String.valueOf(ChatColor.BLUE) + ChatColor.BOLD + "RIGHT CLICK to show mercy, or LEFT CLICK to send them to the Monks.");

        koListener.verdictDelay.add(killer.getUniqueId());
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> koListener.verdictDelay.remove(killer.getUniqueId())
                , 50L);

        if (mobsUntarget) stopTarget(p);
        Location l = layPlayerDown(p);

        corpseRegistry.register(p, killer, l);
        logKill(p, killer);
    }

    private void logKill(Player player, Entity killer) {
        if (getServer().getPluginManager().isPluginEnabled("Omniscience")) {
            DataWrapper wrapper = DataWrapper.createNew();
            wrapper.set(TARGET, player.getName());
            OEntry.create().source(killer).customWithLocation("down", wrapper, player.getLocation()).save();
        }
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

        PacketUtils.layDown(p, l);


        return l;
    }

    public String giveName(Player p) {
        return p.getDisplayName() + ChatColor.GRAY + ChatColor.ITALIC + " (" + p.getName() + ")" + ChatColor.RESET;
    }

    public CorpseRegistry getCorpseRegistry() {
        return this.corpseRegistry;
    }

    public HeadRequestRegistry getHeadRequestRegistry(){
        return this.headRequestRegistry;
    }

    public Map<UUID, Long> getRecentKos() {
        return this.recentKos;
    }

    public KOListener getKoListener() {
        return this.koListener;
    }
}