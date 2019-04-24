package io.github.archemedes.knockoutplus;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import org.bukkit.entity.Player;

public class PacketManager {

	public static void layDown(Player player) {

		PacketContainer sleepPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
		sleepPacket.getIntegers().write(0, player.getEntityId());

		WrappedDataWatcher poseWatcher = new WrappedDataWatcher();
		poseWatcher.setEntity(player);
		poseWatcher.setObject(0, WrappedDataWatcher.Registry.get(byte.class),6 );
		poseWatcher.setObject(1, WrappedDataWatcher.Registry.get(int.class), 18);
		poseWatcher.setObject(2, WrappedDataWatcher.Registry.get(int.class), 2);

		WrappedDataWatcher bedWatcher = new WrappedDataWatcher();
		poseWatcher.setEntity(player);
		poseWatcher.setObject(0, WrappedDataWatcher.Registry.get(byte.class),12 );
		poseWatcher.setObject(1, WrappedDataWatcher.Registry.get(int.class), 10);
		poseWatcher.setObject(2, WrappedDataWatcher.Registry.get(BlockPosition.class), new BlockPosition(player.getLocation().toVector()));

		sleepPacket.getWatchableCollectionModifier().write(0, poseWatcher.getWatchableObjects());
		sleepPacket.getWatchableCollectionModifier().write(1, bedWatcher.getWatchableObjects());

		KnockoutPlus.get().getProtocol().broadcastServerPacket(sleepPacket);
	}

	public static void wakeup(Player player) {
		PacketContainer sleepPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
		sleepPacket.getIntegers().write(0, player.getEntityId());

		WrappedDataWatcher poseWatcher = new WrappedDataWatcher();
		poseWatcher.setEntity(player);
		poseWatcher.setObject(0, WrappedDataWatcher.Registry.get(byte.class),6 );
		poseWatcher.setObject(1, WrappedDataWatcher.Registry.get(int.class), 18);
		poseWatcher.setObject(2, WrappedDataWatcher.Registry.get(int.class), 1);

		sleepPacket.getWatchableCollectionModifier().write(0, poseWatcher.getWatchableObjects());

		KnockoutPlus.get().getProtocol().broadcastServerPacket(sleepPacket);
	}
}
