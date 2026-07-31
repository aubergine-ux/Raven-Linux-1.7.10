package keystrokesmod.client.module.modules.player;

import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.DescriptionSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.utils.Utils;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C07PacketPlayerDigging;

import java.util.TimerTask;

public class BedAura extends Module {
    public static DescriptionSetting d;
    public static SliderSetting r;
    private java.util.Timer t;
    private boolean hasTarget;
    private int mX, mY, mZ;
    private final long per = 600L;

    public BedAura() {
        super("BedAura", ModuleCategory.player);
        this.registerSetting(d = new DescriptionSetting("Might silent flag on Hypixel."));
        this.registerSetting(r = new SliderSetting("Range", 5.0D, 2.0D, 10.0D, 1.0D));
    }

    public void onEnable() {
        (this.t = new java.util.Timer()).scheduleAtFixedRate(this.t(), 0L, 600L);
    }

    public void onDisable() {
        if (this.t != null) {
            this.t.cancel();
            this.t.purge();
            this.t = null;
        }

        this.hasTarget = false;
    }

    public TimerTask t() {
        return new TimerTask() {
            public void run() {
                int ra = (int) r.getInput();

                for (int y = ra; y >= -ra; --y) {
                    for (int x = -ra; x <= ra; ++x) {
                        for (int z = -ra; z <= ra; ++z) {
                            if (Utils.Player.isPlayerInGame()) {
                                int bx = (int) (Module.mc.thePlayer.posX + (double) x);
                                int by = (int) (Module.mc.thePlayer.posY + (double) y);
                                int bz = (int) (Module.mc.thePlayer.posZ + (double) z);
                                boolean bed = Module.mc.theWorld.getBlock(bx, by, bz) == Blocks.bed;
                                if (BedAura.this.hasTarget && BedAura.this.mX == bx && BedAura.this.mY == by && BedAura.this.mZ == bz) {
                                    if (!bed) {
                                        BedAura.this.hasTarget = false;
                                    }
                                } else if (bed) {
                                    BedAura.this.mi(bx, by, bz);
                                    BedAura.this.mX = bx;
                                    BedAura.this.mY = by;
                                    BedAura.this.mZ = bz;
                                    BedAura.this.hasTarget = true;
                                    break;
                                }
                            }
                        }
                    }
                }

            }
        };
    }

    private void mi(int x, int y, int z) {
        mc.thePlayer.sendQueue
                .addToSendQueue(new C07PacketPlayerDigging(0, x, y, z, 2));
        mc.thePlayer.sendQueue
                .addToSendQueue(new C07PacketPlayerDigging(2, x, y, z, 2));
    }
}
