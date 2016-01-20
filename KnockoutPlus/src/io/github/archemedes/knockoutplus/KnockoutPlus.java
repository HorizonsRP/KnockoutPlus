package io.github.archemedes.knockoutplus;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.EnumWrappers.Particle;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.github.archemedes.knockoutplus.corpse.BleedoutTimer;
import io.github.archemedes.knockoutplus.corpse.Corpse;
import io.github.archemedes.knockoutplus.corpse.CorpseRegistry;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.v1_8_R3.PacketPlayOutEntity;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public final class KnockoutPlus extends JavaPlugin
{
	FileConfiguration config;
	static boolean mobsUntarget;
	public static int bleedoutTime;
	static boolean playersKO;
	static boolean mobsKO;
	static boolean nonMobsKO;
	static boolean protectBlocks;
	private static ProtocolManager protocol;
	static Map<UUID, Long> recentKos = Maps.newHashMap();

	public void onEnable()
	{
		new KOListener(this);
		this.config = getConfig();
		saveDefaultConfig();

		new BleedoutTimer(this).runTaskTimer(this, 0L, 133L);

		bleedoutTime = this.config.getInt("bleedout.time");
		mobsUntarget = this.config.getBoolean("mobs.untarget.knockout");
		playersKO = this.config.getBoolean("players.cause.knockout");
		mobsKO = this.config.getBoolean("mobs.cause.knockout");
		nonMobsKO = this.config.getBoolean("nonmobs.cause.knockout");
		protectBlocks = this.config.getBoolean("protect.ko.blocks");

		protocol = ProtocolLibrary.getProtocolManager();

		//final List<String> mnames = Lists.newArrayList("getType", "getID", "get", "getHandle", "getClass", "getByteArraySerializer", "getPositionModifer");
		/*
		protocol.addPacketListener(new PacketAdapter(this, new PacketType[] { PacketType.Play.Server.BED, PacketType.Play.Server.REL_ENTITY_MOVE })
		{
			public void onPacketSending(PacketEvent e)
			{
				System.out.println("PacektType: " + e.getPacket().getType().name());
				for (Method o : PacketContainer.class.getMethods()) {
					if (mnames.contains(o.getName())) continue;
					if (!o.getName().startsWith("get")) continue;
					StructureModifier<Object> list = null;
					try {
						if (o.getName().equals("getEntityModifier")) {
							list = (StructureModifier<Object>) o.invoke(e.getPacket(), e.getPlayer().getWorld());
						} else {
							list = (StructureModifier<Object>) o.invoke(e.getPacket(), null);
						}


					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
					}
					if (list == null) continue;
					if (list.size() > 0){
						System.out.println(o.getName() + ".size() returns: " + list.size() + " value(s):");
						for (Object obj : list.getValues()) {
							try {
								System.out.println(obj.toString());
							} catch (Exception e2) {}
						}
					}
				}
			}
		});*/

		protocol.addPacketListener(new PacketAdapter(this, new PacketType[] { PacketType.Play.Server.NAMED_ENTITY_SPAWN })
		{
			public void onPacketSending(PacketEvent event)
			{
				PacketContainer packet = event.getPacket();
				final int id = ((Integer)packet.getIntegers().read(0)).intValue();
				

				for (final Corpse c : CorpseRegistry.getCorpses())
					if (c.getEntityId() == id) {
						final List<Player> t = Lists.newArrayList(event.getPlayer());
						Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable()
						{
							public void run() {
								Location l = c.getLocation().add(0, 1, 0);
								sendBedPacket(getServer().getPlayer(c.getVictim()), l, t);
							}
						}
						, 2L);

						break;
					}
			}
		});
	}

	public void onDisable()
	{
		for (Corpse c : CorpseRegistry.getCorpses()) {
			Player p = KOListener.getPlayer(c.getVictim());
			p.sendMessage("§cAn evil entity has condemned you.");
			wake(p, null, false);
			removePlayer(p);
			p.damage(1.0D);
			p.setHealth(0.0D);
		}
	}

	@SuppressWarnings("deprecation")
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("damage")) {
			if (!(sender instanceof Player))
				return true;
			if (args.length != 1) {
				return false;
			}
			Player p = (Player)sender;
			int slot;
			try
			{
				slot = Math.abs(Integer.parseInt(args[0]));
			}
			catch (NumberFormatException e)
			{
				return false;
			}
			if (slot > 5) {
				EntityDamageEvent event = new EntityDamageEvent(p, EntityDamageEvent.DamageCause.SUICIDE, slot);
				Bukkit.getPluginManager().callEvent(event);
				p.damage(event.getDamage());
			} else {
				if (p.getGameMode() == GameMode.SURVIVAL) p.setHealth(p.getHealth() - slot);
				else {
					p.sendMessage("You have to be in survival to do this.");
					return true;
				}
			}

			return true;
		}if (cmd.getName().equalsIgnoreCase("hunger")) {
			if (!(sender instanceof Player))
				return true;
			if (args.length != 1) {
				return false;
			}
			Player p = (Player)sender;
			int slot;
			try
			{
				slot = Math.abs(Integer.parseInt(args[0]));
			}
			catch (NumberFormatException e)
			{
				return false;
			}
			if (slot >= 20) {
				p.setFoodLevel(0);
			} else {
				p.setFoodLevel(p.getFoodLevel()-slot);
			}

			return true;
		}if (cmd.getName().equalsIgnoreCase("d20")) {
			if (!(sender instanceof Player))
				return true;
			if (args.length != 0)
				return false;
			Player p = (Player)sender;

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
				sender.sendMessage("§c" + args[0] + " is not online!");
				return false;
			}

			if (!CorpseRegistry.isKnockedOut(target)) {
				sender.sendMessage("§c" + args[0] + " cannot be helped.");
				return true;
			}


			final Corpse corpse = CorpseRegistry.getCorpse(target);
			Player killer = KOListener.getPlayer(corpse.getKiller());
			if (!(sender instanceof Player) || (Lists.newArrayList(args).contains("gm") && sender.hasPermission("archecore.mod")))
			{
				PlayerReviveEvent event = new PlayerReviveEvent(null, target, PlayerReviveEvent.Reason.OPERATOR);
				Bukkit.getPluginManager().callEvent(event);
				if (!event.isCancelled()) {
					revivePlayer(target, 4.0D);
					corpse.unregister();
				}
				return true;
			}

			player = (Player)sender;

			if (CorpseRegistry.isKnockedOut(player)) {
				sender.sendMessage("§cYou are knocked out!");
				return true;
			}

			if (player.equals(target)) {
				sender.sendMessage("§cYou cannot revive yourself!");
				return true;
			}

			if (player.equals(killer)) {
				sender.sendMessage("§cUse a right click on this player instead!");
				return true;
			}

			if (!player.getLocation().getWorld().equals(target.getLocation().getWorld())) {
				sender.sendMessage("§c" + args[0] + " cannot be helped!");
				return true;
			}if (player.getLocation().distance(target.getLocation()) > 20.0D) {
				sender.sendMessage("§c" + args[0] + " cannot be helped!");
				return true;
			}if (player.getLocation().distance(target.getLocation()) > 3.0D) {
				sender.sendMessage("§bYou must move closer to help them.");
				return true;
			}

			player.getWorld().playSound(player.getLocation(), Sound.DIG_WOOL, 3.5F, -1.0F);
			player.sendMessage("§eYou bend down to try and assist " + giveName(target));
			player.sendMessage("§7§o(Hold still or your action will be interrupted.)");
			target.sendMessage("§eYou are being assisted by " + giveName(player));

			final Location chantSpot = player.getLocation();

			int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable()
			{
				public void run()
				{
					KOListener.chants.remove(player.getUniqueId());
					if (!CorpseRegistry.isKnockedOut(target)) return;

					if (player.getLocation().getWorld() == chantSpot.getWorld())
						if (player.getLocation().distance(chantSpot) > 0.2D) {
							player.sendMessage(ChatColor.RED + "You have been interrupted!");
						} else {
							PlayerReviveEvent event = new PlayerReviveEvent(player, target, PlayerReviveEvent.Reason.COMMAND_REVIVE);
							Bukkit.getPluginManager().callEvent(event);
							if (event.isCancelled()) return;

							player.getWorld().playSound(player.getLocation(), Sound.ZOMBIE_UNFECT, 1.2F, 1.0F);
							player.sendMessage("§6You have saved " + KnockoutPlus.this.giveName(target) + " §6from a grisly fate.");
							target.sendMessage("§6You have been saved, but you still feel weak");
							target.sendMessage(ChatColor.DARK_RED + "Caution: Being incapacitated again shall mean your demise.");

							KnockoutPlus.revivePlayer(target, 4.0D);
							corpse.unregister();

							KnockoutPlus.recentKos.put(target.getUniqueId(), Long.valueOf(System.currentTimeMillis()));
						}
				}
			}
			, 100L);

			Integer oldTaskId = (Integer)KOListener.chants.put(player.getUniqueId(), Integer.valueOf(taskId));
			if (oldTaskId != null) {
				Bukkit.getScheduler().cancelTask(oldTaskId.intValue());
			}
			return true;
		} else if (cmd.getName().equalsIgnoreCase("reviveall")) {
			if ((sender.hasPermission("nexus.moderator")) || (!(sender instanceof Player))) CorpseRegistry.reviveAll(); else
				sender.sendMessage("§cYou do not have permission to use this!");
			return true;
		} else if (cmd.getName().equalsIgnoreCase("togglerevive")) {
			if ((args.length == 1) && ((sender.hasPermission("nexus.moderator")))) {
				if (args[0].equalsIgnoreCase("players")) {
					if (playersKO) sender.sendMessage("Players will no longer cause knockout!"); else
						sender.sendMessage("Players will now cause knockout!");
					playersKO = !playersKO;

					return true;
				} else if (args[0].equalsIgnoreCase("mobs")) {
					if (mobsKO) sender.sendMessage("Mobs will no longer cause knockout!"); else {
						sender.sendMessage("Mobs will now cause knockout!");
					}
					mobsKO = !mobsKO;
					return true;
				} else if (args[0].equalsIgnoreCase("environment")) {
					if (nonMobsKO) sender.sendMessage("Environment will no longer cause knockout!"); else {
						sender.sendMessage("Environment will now cause knockout!");
					}
					nonMobsKO = !nonMobsKO;
					return true;
				} else if (args[0].equalsIgnoreCase("list")) {
					String gr = ChatColor.GRAY + "";
					String it = ChatColor.ITALIC + "";
					for (Corpse c : CorpseRegistry.getCorpses()) {
						sender.sendMessage(gr + it + c.getVictim() + gr + " killed by " + it + c.getKiller() + gr + " at " + it + c.getLocation());
					}

					return true;
				}
			}
		}

		return false;
	}

	@SuppressWarnings("deprecation")
	public static void removePlayer(Player p)
	{
		Corpse c = CorpseRegistry.getCorpse(p);
		Location l = c.getLocation();
		p.sendBlockChange(l, l.getBlock().getType(), l.getBlock().getData());
	}

	static boolean wasRecentlyKnockedOut(Player p)
	{
		UUID u = p.getUniqueId();
		if (recentKos.containsKey(u)) {
			long time = System.currentTimeMillis();
			if (time < ((Long)recentKos.get(u)).longValue() + 600000L) return true;
		}

		return false;
	}

	void koPlayer(Player p)
	{
		p.sendMessage("§cYou have been knocked out and will die if not aided!");
		p.sendMessage("§2");

		if (p.getFoodLevel() < 3) {
			p.setFoodLevel(3);
		}
		Location l = layPlayerDown(p);
		CorpseRegistry.register(p, l);
		if (mobsUntarget) stopTarget(p);
	}

	void koPlayer(Player p, final Player k)
	{
		p.sendMessage("§cYou were defeated by §o" + k.getDisplayName());

		k.sendMessage("§6You have defeated §o" + p.getDisplayName());
		k.sendMessage("§o§9 RIGHT CLICK to show mercy, or LEFT CLICK to send them to the Monks.");

		KOListener.verdictDelay.add(k.getUniqueId());
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			public void run() {
				KOListener.verdictDelay.remove(k.getUniqueId());
			}
		}
		, 50L);

		if (mobsUntarget) stopTarget(p);
		Location l = layPlayerDown(p);

		CorpseRegistry.register(p, k, l);
	}

	private void stopTarget(Player p) {
		for (Entity e : p.getNearbyEntities(12.0D, 5.0D, 12.0D))
			if ((e instanceof Creature)) {
				Creature c = (Creature)e;
				if (c.getTarget() == p) c.setTarget(null);
			}
	}

	private Location layPlayerDown(final Player p)
	{
		p.setBedSpawnLocation(null);
		p.leaveVehicle();
		p.setFireTicks(0);
		p.setHealth(p.getMaxHealth());

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
			}
			else if (b.getRelative(BlockFace.WEST).isEmpty()) {
				if ((b.getRelative(BlockFace.NORTH_WEST).isEmpty()) && 
						(!b.getRelative(-1, -1, -1).isEmpty()) && (!b.getRelative(-1, -1, 0).isEmpty()))
					l.add(-1.0D, 0.0D, 0.0D);
				else if ((b.getRelative(BlockFace.SOUTH_WEST).isEmpty()) && 
						(!b.getRelative(-1, -1, 0).isEmpty()) && (!b.getRelative(-1, -1, 1).isEmpty()))
					l.add(-1.0D, 0.0D, 1.0D);
			}
			else if (b.getRelative(BlockFace.EAST).isEmpty()) {
				if ((b.getRelative(BlockFace.NORTH_EAST).isEmpty()) && 
						(!b.getRelative(1, -1, -1).isEmpty()) && (!b.getRelative(1, -1, 0).isEmpty()))
					l.add(1.0D, 0.0D, 0.0D);
				else if ((b.getRelative(BlockFace.SOUTH_EAST).isEmpty()) && 
						(!b.getRelative(1, -1, 0).isEmpty()) && (!b.getRelative(1, -1, 1).isEmpty())) {
					l.add(1.0D, 0.0D, 1.0D);
				}

			}

		}

		/*final Material m = l.getBlock().getType();

		if ((m == Material.FLOWER_POT) || (m == Material.SKULL) || (m == Material.STEP) || ((m == Material.SNOW) && (l.getBlock().getData() >= 2))) {
			p.sendBlockChange(l, Material.GLASS, (byte)0);
			l.add(0.0D, 1.0D, 0.0D);
		} else {
			p.sendBlockChange(l, Material.STEP, (byte)0);
		}*/
		
		if (height > 4) {
			p.teleport(l);
		}
		
		final Location bl = l.add(0.0D, 1.0D, 0.0D);

		
		sendBedPacket(p,bl,p.getWorld().getPlayers());
		
		
		return l;
	}
	
	@SuppressWarnings("deprecation")
	private void sendBedPacket(Player p, Location l, List<Player> targets) {
		for (Player t : targets) {
			t.sendBlockChange(new Location(l.getWorld(), l.getBlockX(), 0, l.getBlockZ()), Material.BED_BLOCK, (byte) 0);
		}
		
		PacketContainer packet = new PacketContainer(PacketType.Play.Server.BED);
		packet.getIntegers().write(0, Integer.valueOf(p.getEntityId()));
		packet.getBlockPositionModifier().write(0, new BlockPosition(l.getBlockX(), 0, l.getBlockZ()));
		protocol.broadcastServerPacket(packet);
		final PacketPlayOutEntity.PacketPlayOutRelEntityMove move = new PacketPlayOutEntity.PacketPlayOutRelEntityMove(p.getEntityId(), (byte)0, (byte)(l.getBlockY()+1), (byte)0, false);
		for (Player t : targets) {
			if (p != t)
			((CraftPlayer)t).getHandle().playerConnection.sendPacket(move);
		}
	}

	@SuppressWarnings("deprecation")
	static void wake(Player v, Location l, boolean updateblock)
	{
		PacketContainer packet = new PacketContainer(PacketType.Play.Server.ANIMATION);
		packet.getIntegers().write(0, Integer.valueOf(v.getEntityId())).write(1, Integer.valueOf(2));
		protocol.broadcastServerPacket(packet, v, true);
		if (updateblock) {
			for (Player t : v.getWorld().getPlayers()) {
				t.sendBlockChange(new Location(l.getWorld(), l.getBlockX(), 0, l.getBlockZ()), Material.BEDROCK, (byte)0);
			}
		}

	}

	@SuppressWarnings("deprecation")
	public static void wakeOne(Player v) {
		PacketContainer packet = new PacketContainer(PacketType.Play.Server.ANIMATION);
		packet.getIntegers().write(0, Integer.valueOf(v.getEntityId())).write(1, Integer.valueOf(2));
		Location l = v.getLocation();
		try {
			v.sendBlockChange(new Location(l.getWorld(), l.getBlockX(), 0, l.getBlockZ()), Material.BEDROCK, (byte)0);
			protocol.sendServerPacket(v, packet); } catch (InvocationTargetException e) {
				e.printStackTrace();
			}
	}

	public static void revivePlayer(Player v, double hp)
	{
		wake(v, v.getLocation(), true);
		removePlayer(v);

		v.setHealth(Math.min(v.getMaxHealth(), hp));
		v.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 400, 5, true), true);
		v.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 400, 1, true), true);

		Location l = v.getLocation();

		PacketContainer hearts = new PacketContainer(PacketType.Play.Server.WORLD_PARTICLES);
		hearts.getParticles().write(0, Particle.HEART);
		hearts.getIntegers().write(0, Integer.valueOf(10));
		hearts.getFloat()
		.write(0, Float.valueOf((float)l.getX()))
		.write(1, Float.valueOf((float)l.getY()))
		.write(2, Float.valueOf((float)l.getZ()))
		.write(3, Float.valueOf(0.5F))
		.write(4, Float.valueOf(0.5F))
		.write(5, Float.valueOf(0.5F))
		.write(6, Float.valueOf(4.0F));

		protocol.broadcastServerPacket(hearts, v, true);
	}

	String giveName(Player p)
	{
		return p.getDisplayName() + ChatColor.GRAY + ChatColor.ITALIC + " (" + p.getName() + ")" + ChatColor.RESET;
	}
}

/* Location:           C:\Users\Nick\Desktop\Minecraft\LOTC\LeadDev\plugins\KnockoutPlus.jar
 * Qualified Name:     io.github.archemedes.knockoutplus.KnockoutPlus
 * JD-Core Version:    0.6.2
 */