package de.dertoaster.movecraft.bridges.sign;

import de.dertoaster.movecraft.bridges.Constants;
import de.dertoaster.movecraft.bridges.bridge.BridgeSignData;
import net.countercraft.movecraft.Movecraft;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.sign.AbstractCraftSign;
import net.countercraft.movecraft.sign.SignListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BridgeSign extends AbstractCraftSign {
    public BridgeSign() {
        super(true);
    }

    @Override
    protected void onCraftIsBusy(Player player, Craft craft) {

    }

    @Override
    protected boolean canPlayerUseSignOn(Player player, @Nullable Craft craft) {
        if (super.canPlayerUseSignOn(player, craft)) {
            if (!craft.getType().getBoolProperty(Constants.CraftFileEntries.KEY_BRIDGES_ALLOWED)) {
                player.sendMessage("Bridges are not allowed on this craft!");
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onCraftNotFound(Player player, SignListener.SignWrapper signWrapper) {
    }

    @Override
    protected boolean internalProcessSignWithCraft(Action action, SignListener.SignWrapper signWrapper, Craft craft, Player player) {
        Optional<BridgeSignData> optBridge = BridgeSignData.tryGetBridgeSignData(signWrapper.block(), player::sendMessage);
        if (optBridge.isEmpty()) {
            player.sendMessage("Somehow the bridge sign could not be created but it was validated");
            return false;
        }
        BridgeSignData bridge = optBridge.get();
        Optional<BridgeSignData> optTargetBridge = BridgeSignData.tryFindBridgeOnCraft(craft, bridge.nextBridge(), player::sendMessage);
        if (optTargetBridge.isEmpty()) {
            player.sendMessage("Unable to find target bridge on craft!");
            return false;
        } else {
            BridgeSignData targetBridge = optTargetBridge.get();
            Location exitLoc = targetBridge.getEffectiveLocation();

            exitLoc.setPitch(player.getLocation().getPitch());
            exitLoc.setYaw(player.getLocation().getYaw());

            Movecraft.getInstance().getSmoothTeleport().teleport(player, exitLoc);
            return true;
        }
    }

    @Override
    protected boolean isSignValid(Action action, SignListener.SignWrapper signWrapper, Player player) {
        return BridgeSignData.validateSign(signWrapper.rawLines(), player::sendMessage);
    }

    @Override
    public boolean processSignChange(SignChangeEvent signChangeEvent, SignListener.SignWrapper signWrapper) {
        if (!BridgeSignData.validateSign(signWrapper.rawLines(), signChangeEvent.getPlayer()::sendMessage)) {
            signChangeEvent.setCancelled(true);
            return false;
        }
        return true;
    }

    @Override
    public void onCraftDetect(CraftDetectEvent event, SignListener.SignWrapper sign) {
        // TODO: Path validation
        super.onCraftDetect(event, sign);
    }
}
