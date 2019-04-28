package io.github.archemedes.knockoutplus.utils;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import io.github.archemedes.knockoutplus.KnockoutPlus;
import org.bukkit.entity.Player;

public class WorldGuardUtils {
	public final static StateFlag PLAYER_KO = new StateFlag("player-knockout", true);
	public final static StateFlag MOB_KO = new StateFlag("mob-knockout", true);
	public final static StateFlag OTHER_KO = new StateFlag("environment-knockout", true);

	public static void init() {
		FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
		if (registry.get(PLAYER_KO.getName()) == null) {
			registry.register(PLAYER_KO);
			registry.register(MOB_KO);
			registry.register(OTHER_KO);
		} else {
			KnockoutPlus.get().getLogger().info("Skipping flag registry... is the plugin reloading?");
		}
	}

	public static ApplicableRegionSet getSet(LocalPlayer player) {
		return WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().getApplicableRegions(player.getLocation());
	}

	public static boolean isAllowed(Player p, String flagName) {
		LocalPlayer lp = WorldGuardPlugin.inst().wrapPlayer(p);
		Flag flag = WorldGuard.getInstance().getFlagRegistry().get(flagName);

		if (!(flag instanceof StateFlag)) {
			return false;
		}

		return WorldGuardUtils.getSet(lp).testState(lp, (StateFlag) flag);
	}
}
