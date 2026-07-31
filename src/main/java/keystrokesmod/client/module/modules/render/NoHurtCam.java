package keystrokesmod.client.module.modules.render;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.ForgeEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.utils.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import cpw.mods.fml.common.gameevent.TickEvent;

public class NoHurtCam extends Module {

    public NoHurtCam() {
        super("NoHurtCam", ModuleCategory.render);
    }

    @Subscribe
    public void onTick(ForgeEvent fe) {
        if (fe.getEvent() instanceof TickEvent.ClientTickEvent) {
            if (Utils.Player.isPlayerInGame()) {
                EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
                if (player != null && player.hurtTime > 0) {
                    player.hurtTime = 0;
                }
            }
        }
    }

    @Override
    public void onEnable() {
        if (Utils.Player.isPlayerInGame()) {
            EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
            if (player != null) {
                player.hurtTime = 0;
                player.hurtResistantTime = 0;
            }
        }
    }

    @Override
    public void onDisable() {
        if (Utils.Player.isPlayerInGame()) {
            EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
            if (player != null) {
                player.hurtResistantTime = 0;
            }
        }
    }
}
