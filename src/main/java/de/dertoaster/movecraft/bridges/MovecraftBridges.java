package de.dertoaster.movecraft.bridges;

import org.bukkit.plugin.java.JavaPlugin;

import de.dertoaster.movecraft.bridges.listeners.SignInteractListener;

public class MovecraftBridges extends JavaPlugin {

	@Override
	public void onEnable() {
		super.onEnable();
		
		// Register event listener
		this.getServer().getPluginManager().registerEvents(new SignInteractListener(), this);
	}
	
}
