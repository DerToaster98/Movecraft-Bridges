package de.dertoaster.movecraft.bridges;

import org.bukkit.plugin.java.JavaPlugin;

import de.dertoaster.movecraft.bridges.listeners.SignInteractListener;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.property.BooleanProperty;

public class MovecraftBridges extends JavaPlugin {

	@Override
	public void onEnable() {
		super.onEnable();
		
		// Register event listener
		this.getServer().getPluginManager().registerEvents(new SignInteractListener(), this);
	}
	
	@Override
	public void onLoad() {
		super.onLoad();
		// Register the crafttype entry
		CraftType.registerProperty(new BooleanProperty("allowBridgeSign", Constants.CraftFileEntries.KEY_BRIDGES_ALLOWED, type -> false));
	}
	
}
