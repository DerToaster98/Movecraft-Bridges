package de.dertoaster.movecraft.bridges.listeners;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.jetbrains.annotations.NotNull;

import de.dertoaster.movecraft.bridges.Constants;
import de.dertoaster.movecraft.bridges.bridge.BridgeSignData;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.util.MathUtils;

public class SignInteractListener implements Listener {
	
	@EventHandler
	public final void onSignChange(SignChangeEvent event) {
		if (!event.getLine(0).equalsIgnoreCase(Constants.BRIDGE_SIGN_HEADER)) {
			return;
		}
		
		if (!BridgeSignData.validateSign(event.getLines(), event.getPlayer()::sendMessage)) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public final void onSignClick(@NotNull PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
			return;
		}
		Craft craft = null;
		final MovecraftLocation movecraftLoc = MathUtils.bukkit2MovecraftLoc(event.getClickedBlock().getLocation());
		for(PlayerCraft craftTmp : CraftManager.getInstance().getPlayerCraftsInWorld(event.getClickedBlock().getWorld())) {
			if(craftTmp.getHitBox().contains(movecraftLoc)) {
				craft = craftTmp;
				break;
			}
		}
		if (craft == null) {
			// No craft found => return
			event.setCancelled(true);
		}
		final Craft fcraft = craft;
		
		Optional<BridgeSignData> optBridge = BridgeSignData.tryGetBridgeSignData(event.getClickedBlock(), event.getPlayer()::sendMessage);
		optBridge.ifPresent(bridge -> {
			Optional<BridgeSignData> optTargetBridge = BridgeSignData.tryFindBridgeOnCraft(fcraft, bridge.nextBridge(), event.getPlayer()::sendMessage);
			if(optTargetBridge.isEmpty()) {
				event.setCancelled(true);
			} else {
				BridgeSignData targetBridge = optTargetBridge.get();
				Location exitLoc = targetBridge.getEffectiveLocation();
				
				event.getPlayer().teleport(exitLoc, TeleportCause.PLUGIN);
				// Prevent sign breaking
				event.setCancelled(true);
			}
		});
	}
	
}
