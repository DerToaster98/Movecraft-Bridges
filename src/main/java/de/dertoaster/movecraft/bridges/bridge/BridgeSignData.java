package de.dertoaster.movecraft.bridges.bridge;

import de.dertoaster.movecraft.bridges.Constants;
import de.dertoaster.movecraft.bridges.sign.BridgeSign;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.sign.AbstractCraftSign;
import net.countercraft.movecraft.sign.MovecraftSignRegistry;
import net.countercraft.movecraft.sign.SignListener;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

// Represents the essential bridge data and logic => Finding partner, parsing and so on...
public record BridgeSignData(
		String name,
		String nextBridge,
		// Forward, Vertical, Left/Right
		float offsetX,
		float offsetY,
		float offsetZ,
		SignListener.SignWrapper wrapper
	) {
	
	public static Optional<BridgeSignData> tryFindBridgeOnCraft(final Craft craft, final String bridgeName, Consumer<String> errorMessageHandler) {
		if (craft == null || bridgeName == null || bridgeName.isBlank() || bridgeName.isEmpty()) {
			return Optional.empty();
		}
		
		for (MovecraftLocation ml : craft.getHitBox().asSet()) {
			BlockState state = craft.getWorld().getBlockAt(ml.toBukkit(craft.getWorld())).getState();
			if (state instanceof Sign sign) {
				for (SignListener.SignWrapper wrapperTmp : SignListener.INSTANCE.getSignWrappers(sign, true)) {
					AbstractCraftSign handlerSign = MovecraftSignRegistry.INSTANCE.getCraftSign(wrapperTmp.line(0));
					if (handlerSign == null || !(handlerSign instanceof BridgeSign)) {
						continue;
					}

					// No output for those!
					Optional<BridgeSignData> tempResult = tryGetBridgeSignData(wrapperTmp, (s) -> {});
					if (tempResult.isPresent() && tempResult.get().isExitFor(bridgeName)) {
						return tempResult;
					}
				}
			}
		}
		return Optional.empty();
	}
	
	public boolean isExitFor(final String bridge) {
		return ChatColor.stripColor(this.name).equals(ChatColor.stripColor(bridge));
	}

	public static boolean validateSign(String[] lines, Consumer<String> errorMessageHandler) {
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

	public static Optional<BridgeSignData> tryGetBridgeSignData(SignListener.SignWrapper wrapper, Consumer<String> errorMessageHandler) {
		String[] lines = wrapper.rawLines();
		
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
		
		return Optional.of(new BridgeSignData(lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_NAME], lines[Constants.BridgeSignLineIndizes.BRIDGE_SIGN_INDEX_TARGET_BRIDGE], x, y, z, wrapper));
	}

	@Nullable
	public Location getEffectiveLocation() {
		Location loc = this.wrapper.block().getLocation();
		if (loc == null) {
			return loc;
		}

		Vector vector = this.wrapper.facing().getDirection();
		Location result = this.getEffectiveLocation(vector, loc);
		if (result != null) {
			return result;
		}

		return null;
		
	}

//	protected Location getEffectiveLocation(WallSign wallSignData, Location block) {
//		Vector shift = wallSignData.getFacing().getDirection();
//		return getEffectiveLocation(shift, block);
//	}
//
//	protected Location getEffectiveLocation(Sign signData, Location block) {
//		Vector shift = signData.getRotation().getDirection();
//		return getEffectiveLocation(shift, block);
//	}
	
	protected Location getEffectiveLocation(Vector signDirection, Location block) {
		// Get the centered location at that block
		float x = block.getBlockX() + 0.5F;
		float y = block.getBlockY() + 0.01F;
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
