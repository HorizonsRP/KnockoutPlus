package io.github.archemedes.knockoutplus;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.minecraft.server.v1_14_R1.BlockPosition;
import net.minecraft.server.v1_14_R1.EntityPose;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

public class PacketManager {

	public static void layDown(Player player) {
		Location location = player.getLocation();

		PacketContainer sleepPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);

		sleepPacket.getIntegers().write(0, player.getEntityId());

		WrappedDataWatcher watcher = new WrappedDataWatcher();
		watcher.setEntity(player);
		// TODO: Remove NMS when protocolib updates
		watcher.setObject(6, WrappedDataWatcher.Registry.get(EntityPose.class), EntityPose.SLEEPING);
		watcher.setObject(12, WrappedDataWatcher.Registry.get(BlockPosition.class, true), Optional.of(new BlockPosition(location.getBlockX(), location.getBlockY() - 3, location.getBlockZ())));

		sleepPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

		KnockoutPlus.get().getProtocolManager().broadcastServerPacket(sleepPacket);
	}

	public static void wakeup(Player player) {
		PacketContainer sleepPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
		sleepPacket.getIntegers().write(0, player.getEntityId());

		WrappedDataWatcher watcher = new WrappedDataWatcher();
		watcher.setEntity(player);
		watcher.setObject(6, WrappedDataWatcher.Registry.get(EntityPose.class), EntityPose.STANDING);

		sleepPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

		KnockoutPlus.get().getProtocolManager().broadcastServerPacket(sleepPacket);
	}
}
