package keystrokesmod.client.module.modules.player;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.UpdateEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import keystrokesmod.client.utils.Utils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.Vec3;

import java.util.Iterator;

public class ChestAura extends Module {
    public static SliderSetting range;
    public static TickSetting chest, enderChest;
    private long lastOpenTime = 0;

    public ChestAura() {
        super("ChestAura", ModuleCategory.player);
        this.registerSetting(range = new SliderSetting("Range", 5.0, 1.0, 8.0, 0.5));
        this.registerSetting(chest = new TickSetting("Chest", true));
        this.registerSetting(enderChest = new TickSetting("Ender chest", true));
    }

    @Subscribe
    public void onUpdate(UpdateEvent e) {
        if (!Utils.Player.isPlayerInGame() || mc.currentScreen != null) return;
        if (System.currentTimeMillis() - lastOpenTime < 200) return;

        double r = range.getInput();
        Iterator iterator = mc.theWorld.loadedTileEntityList.iterator();

        while (iterator.hasNext()) {
            TileEntity te = (TileEntity) iterator.next();
            if (!shouldOpen(te)) continue;

            int x = te.xCoord;
            int y = te.yCoord;
            int z = te.zCoord;
            double dist = mc.thePlayer.getDistance(x + 0.5, y + 0.5, z + 0.5);
            if (dist > r) continue;

            mc.thePlayer.swingItem();
            mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(),
                    x, y, z, 1, Vec3.createVectorHelper(x + 0.5, y + 0.5, z + 0.5));
            lastOpenTime = System.currentTimeMillis();
            break;
        }
    }

    private boolean shouldOpen(TileEntity te) {
        if (te instanceof TileEntityChest && chest.isToggled()) return true;
        if (te instanceof TileEntityEnderChest && enderChest.isToggled()) return true;
        return false;
    }

    @Override
    public void onDisable() {
        lastOpenTime = 0;
        super.onDisable();
    }
}
