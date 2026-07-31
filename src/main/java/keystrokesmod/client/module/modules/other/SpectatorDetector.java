package keystrokesmod.client.module.modules.other;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.TickEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.utils.Utils;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashSet;
import java.util.Set;

public class SpectatorDetector extends Module {
    private final Set<String> spectators = new HashSet<>();

    public SpectatorDetector() {
        super("SpectatorDetector", ModuleCategory.other);
    }

    @Override
    public void onEnable() {
        spectators.clear();
    }

    @Subscribe
    public void onTick(TickEvent e) {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        
        if (mc.thePlayer.ticksExisted % 20 == 0) {
            for (EntityPlayer player : mc.theWorld.playerEntities) {
                if (player == mc.thePlayer) continue;
                
                boolean isSpectator = player.isInvisible() && player.capabilities.allowFlying && player.posY > mc.thePlayer.posY;
                
                if (isSpectator && !spectators.contains(player.getCommandSenderName())) {
                    spectators.add(player.getCommandSenderName());
                    Utils.Player.sendMessageToSelf("&c[!] &f" + player.getCommandSenderName() + " &cmight be spectating you.");
                } else if (!isSpectator && spectators.contains(player.getCommandSenderName())) {
                    spectators.remove(player.getCommandSenderName());
                }
            }
        }
    }
}
