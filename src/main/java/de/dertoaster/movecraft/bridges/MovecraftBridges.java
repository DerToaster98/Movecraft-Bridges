package de.dertoaster.movecraft.bridges;

import de.dertoaster.movecraft.bridges.sign.BridgeSign;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.property.BooleanProperty;
import net.countercraft.movecraft.sign.MovecraftSignRegistry;
import org.bukkit.plugin.java.JavaPlugin;

public class MovecraftBridges extends JavaPlugin {

	@Override
	public void onEnable() {
		super.onEnable();
		
		// Register event listener
		MovecraftSignRegistry.INSTANCE.register(Constants.BRIDGE_SIGN_HEADER, new BridgeSign(), true);
	}
	
	@Override
	public void onLoad() {
		super.onLoad();
		// Register the crafttype entry
		CraftType.registerProperty(new BooleanProperty("allowBridgeSign", Constants.CraftFileEntries.KEY_BRIDGES_ALLOWED, type -> false));
	}
	
}
