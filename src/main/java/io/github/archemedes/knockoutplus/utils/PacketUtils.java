package io.github.archemedes.knockoutplus.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import io.github.archemedes.knockoutplus.KnockoutPlus;
import net.minecraft.server.v1_16_R1.BlockPosition;
import net.minecraft.server.v1_16_R1.EntityPose;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

public class PacketUtils {

	public static void layDown(Player player, Location location) {
		PacketContainer sleepPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);

		sleepPacket.getIntegers().write(0, player.getEntityId());

		WrappedDataWatcher watcher = new WrappedDataWatcher();
		watcher.setEntity(player);
		// TODO: Remove NMS when protocolib updates
		watcher.setObject(6, WrappedDataWatcher.Registry.get(EntityPose.class), EntityPose.SLEEPING);
		watcher.setObject(13, WrappedDataWatcher.Registry.get(BlockPosition.class, true), Optional.of(new BlockPosition(location.getBlockX(), location.getBlockY() - 3, location.getBlockZ())));

		sleepPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

		KnockoutPlus.get().getProtocolManager().broadcastServerPacket(sleepPacket);
	}

	public static void wakeup(Player player) {
		PacketContainer sleepPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
		sleepPacket.getIntegers().write(0, player.getEntityId());

		WrappedDataWatcher watcher = new WrappedDataWatcher();
		watcher.setEntity(player);
		watcher.setObject(6, WrappedDataWatcher.Registry.get(EntityPose.class), EntityPose.STANDING);
		watcher.setObject(13, WrappedDataWatcher.Registry.get(BlockPosition.class, true), Optional.empty());

		sleepPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

		KnockoutPlus.get().getProtocolManager().broadcastServerPacket(sleepPacket);
	}
}
