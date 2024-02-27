package de.dertoaster.movecraft.bridges.bridge;

import java.util.Optional;
import java.util.function.Consumer;

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
		float offsetX,
		float offsetY,
		float offsetZ,
		TileState blockState
	) {
	
	public static Optional<BridgeSignData> tryFindBridgeOnCraft(final Craft craft, final String bridgeName, Consumer<String> errorMessageHandler) {
		if (craft == null || bridgeName == null || bridgeName.isBlank() || bridgeName.isEmpty()) {
			return Optional.empty();
		}
		
		for (MovecraftLocation ml : craft.getHitBox().asSet()) {
			BlockState state = craft.getWorld().getBlockAt(ml.toBukkit(craft.getWorld())).getState();
			if (state instanceof org.bukkit.block.Sign sign) {
				Optional<BridgeSignData> tempResult = tryGetBridgeSignData(sign, errorMessageHandler);
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
	
	public static Optional<BridgeSignData> tryGetBridgeSignData(final Location location, final World world, Consumer<String> errorMessageHandler) {
		return tryGetBridgeSignData(world.getBlockAt(location), errorMessageHandler);
	}
	
	public static Optional<BridgeSignData> tryGetBridgeSignData(Block blockAt, Consumer<String> errorMessageHandler) {
		if (blockAt.getState() instanceof org.bukkit.block.Sign sign) {
			return tryGetBridgeSignData(sign, errorMessageHandler);
		}
		return Optional.empty();
	}
	
	public static boolean validateSign(org.bukkit.block.Sign sign, Consumer<String> errorMessageHandler) {
		return validateSign(sign.getLines(), errorMessageHandler);
	}
	
	public static boolean validateSign(String[] lines, Consumer<String> errorMessageHandler) {
		if (!lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_HEADER].equals(Constants.BRIDGE_SIGN_HEADER)) {
			return false;
		}
		if (lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_NAME].isBlank() || lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_NAME].isEmpty()) {
			errorMessageHandler.accept("Bridge must have a name!");
			return false;
		}
		if (!(lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_COORD_MODIFIER].isBlank() || lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_COORD_MODIFIER].isEmpty())) {
			String vec = lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_COORD_MODIFIER];
			String[] vecArr = vec.split(",");
			
			if (vecArr.length == 3) {
				for (int i = 0; i < vecArr.length; i++) {
					String entry = vecArr[i];
					try {
						// Validate that it is a byte...
						float byteTmp = Float.parseFloat(entry);
						if (Math.abs(byteTmp) > 8) {
							// too big => log!
							errorMessageHandler.accept("Entered value " + entry + " is too big, allowed range is -8 to 8");
							return false;
						}
					} catch(NumberFormatException nfe) {
						errorMessageHandler.accept("Could not decipher value " + entry);
						return false;
					}
				}
			}
		}
		if ((lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_TARGET_BRIDGE].isBlank() || lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_TARGET_BRIDGE].isEmpty())) {
			errorMessageHandler.accept("A target bridge must be specified!");
			return false;
		}
		if (ChatColor.stripColor(lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_NAME]).equals(ChatColor.stripColor(lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_TARGET_BRIDGE]))) {
			errorMessageHandler.accept("Bridge name and target bridge name must not be the same!");
			return false;
		}
		return true;
	}

	public static Optional<BridgeSignData> tryGetBridgeSignData(org.bukkit.block.Sign sign, Consumer<String> errorMessageHandler) {
		String[] lines = sign.getLines();
		
		if(!validateSign(lines, errorMessageHandler)) {
			return Optional.empty();
		}
		
		float x = 0;
		float y = 0;
		float z = 0;
		String[] vecArr = lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_COORD_MODIFIER].split(",");
		
		if (vecArr.length == 3) {
			try {
				x = Float.parseFloat(vecArr[0]);
				y = Float.parseFloat(vecArr[1]);
				z = Float.parseFloat(vecArr[2]);
			} catch(NumberFormatException nfe) {
				// it should never call this as it has been validated before!
				errorMessageHandler.accept("Specified vector " + lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_COORD_MODIFIER] + " is not valid!");
				return Optional.empty();
			}
		}
		
		return Optional.of(new BridgeSignData(lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_NAME], lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_TARGET_BRIDGE], x, y, z, sign));
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
		// Get the centered location at that block
		float x = block.getBlockX() + 0.5F;
		float y = block.getBlockY() + 0.5F;
		float z = block.getBlockZ() + 0.5F;
		
		if (!(this.offsetX == 0 && this.offsetY == 0 && this.offsetZ == 0)) {
			y += this.offsetY;
			
			// In facing direction
			Vector shift = signDirection.normalize().multiply(this.offsetX);
			
			x += shift.getBlockX();
			y += shift.getBlockY();
			z += shift.getBlockZ();
			
			// Sideways
			shift = shift.normalize().rotateAroundY(Math.PI / 2);
			shift = shift.multiply(this.offsetZ);
			
			x += shift.getBlockX();
			y += shift.getBlockY();
			z += shift.getBlockZ();
		}
		
		return new Location(block.getWorld(), x, y, z);
	}
	
	
}
