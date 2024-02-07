package de.dertoaster.movecraft.bridges.bridge;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import de.dertoaster.movecraft.bridges.Constants;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.md_5.bungee.api.ChatColor;

// Represents the essential bridge data and logic => Finding partner, parsing and so on...
public record BridgeSignData(
		String name,
		String nextBridge,
		// Forward, Vertical, Left/Right
		Vector locationOffset,
		TileState blockState
	) {
	
	public static Optional<BridgeSignData> tryFindBridgeOnCraft(final Craft craft, final String bridgeName) {
		if (craft == null || bridgeName == null || bridgeName.isBlank() || bridgeName.isEmpty()) {
			return Optional.empty();
		}
		
		for (MovecraftLocation ml : craft.getHitBox().asSet()) {
			BlockState state = craft.getWorld().getBlockAt(ml.toBukkit(craft.getWorld())).getState();
			if (state instanceof org.bukkit.block.Sign sign) {
				Optional<BridgeSignData> tempResult = tryGetBridgeSignData(sign);
				if (tempResult.isPresent() && tempResult.get().isExitFor(bridgeName)) {
					return tempResult;
				}
			}
		}
		return Optional.empty();
	}
	
	public boolean isExitFor(final String bridge) {
		return ChatColor.stripColor(this.name).equals(ChatColor.stripColor(bridge));
	}
	
	public static Optional<BridgeSignData> tryGetBridgeSignData(final Location location, final World world) {
		return tryGetBridgeSignData(world.getBlockAt(location));
	}
	
	public static Optional<BridgeSignData> tryGetBridgeSignData(Block blockAt) {
		if (blockAt.getState() instanceof org.bukkit.block.Sign sign) {
			return tryGetBridgeSignData(sign);
		}
		return Optional.empty();
	}

	public static Optional<BridgeSignData> tryGetBridgeSignData(org.bukkit.block.Sign sign) {
		String[] lines = sign.getLines();
		if (!lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_HEADER].equals(Constants.BRIDGE_SIGN_HEADER)) {
			return Optional.empty();
		}
		if (lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_NAME].isBlank() || lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_NAME].isEmpty()) {
			return Optional.empty();
		}
		Vector v = new Vector(0, 0, 0);
		if (!(lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_COORD_MODIFIER].isBlank() || lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_COORD_MODIFIER].isEmpty())) {
			String vec = lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_COORD_MODIFIER];
			String[] vecArr = vec.split(",");
			if (vecArr.length == 3) {
				for (int i = 0; i < vecArr.length; i++) {
					String entry = vecArr[i];
					try {
						int tmp = Integer.parseInt(entry);
						switch(i) {
							case 0:
								v.setX(tmp);
								break;
							case 1:
								v.setY(tmp);
								break;
							case 2:
								v.setZ(tmp);
								break;
							default:
								break;
						}
					} catch(NumberFormatException nfe) {
						return Optional.empty();
					}
				}
			}
		}
		if ((lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_TARGET_BRIDGE].isBlank() || lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_TARGET_BRIDGE].isEmpty())) {
			return Optional.empty();
		}
		if (ChatColor.stripColor(lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_NAME]).equals(ChatColor.stripColor(lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_TARGET_BRIDGE]))) {
			return Optional.empty();
		} 
		return Optional.of(new BridgeSignData(lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_NAME], lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_TARGET_BRIDGE], v, sign));
	}

	@Nullable
	public Location getEffectiveLocation() {
		Location loc = this.blockState.getLocation();
		if (loc == null) {
			return loc;
		}
		
		if (this.blockState.getBlockData() instanceof WallSign ws) {
			return this.getEffectiveLocation(ws, loc);
		} else if (this.blockState.getBlockData() instanceof Sign s) {
			return this.getEffectiveLocation(s, loc);
		}
		return null;
		
	}

	protected Location getEffectiveLocation(WallSign wallSignData, Location block) {
		Vector shift = wallSignData.getFacing().getDirection();
		return getEffectiveLocation(shift, block);
	}
	
	protected Location getEffectiveLocation(Sign signData, Location block) {
		Vector shift = signData.getRotation().getDirection();
		return getEffectiveLocation(shift, block);
	}
	
	protected Location getEffectiveLocation(Vector signDirection, Location block) {
		int x = block.getBlockX();
		int y = block.getBlockY();
		int z = block.getBlockZ();
		
		y += this.locationOffset.getBlockY();
		
		// In facing direction
		Vector shift = signDirection.normalize().multiply(this.locationOffset.getBlockX());
		
		x += shift.getBlockX();
		y += shift.getBlockY();
		z += shift.getBlockZ();
		
		// Sideways
		shift = shift.normalize().rotateAroundY(Math.PI / 2);
		shift = shift.multiply(this.locationOffset.getBlockZ());
		
		x += shift.getBlockX();
		y += shift.getBlockY();
		z += shift.getBlockZ();
		
		return new Location(block.getWorld(), x, y, z);
	}
	
	
}
