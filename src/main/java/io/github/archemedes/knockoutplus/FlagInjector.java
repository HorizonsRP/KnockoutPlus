package io.github.archemedes.knockoutplus;

import com.sk89q.worldguard.protection.flags.StateFlag;
import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.inventivetalent.regionapi.FlagLoadEvent;

@Getter
public class FlagInjector implements Listener {

    private final StateFlag PLAYER_KO = new StateFlag("player-knockout", true);
    private final StateFlag MOB_KO = new StateFlag("mob-knockout", true);
    private final StateFlag OTHER_KO = new StateFlag("environment-knockout", true);

    @EventHandler
    public void on(FlagLoadEvent event) {
        event.addFlag(PLAYER_KO);
        event.addFlag(MOB_KO);
        event.addFlag(OTHER_KO);
    }

}