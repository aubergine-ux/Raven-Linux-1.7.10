package keystrokesmod.client.module.modules.other;

import com.google.common.eventbus.Subscribe;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import keystrokesmod.client.event.impl.PacketEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.ComboSetting;
import keystrokesmod.client.module.setting.impl.DescriptionSetting;
import keystrokesmod.client.module.setting.impl.DoubleSliderSetting;
import keystrokesmod.client.utils.Utils;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.util.EnumChatFormatting;

import java.util.LinkedList;

public class Disabler extends Module {
    public static DescriptionSetting warning, mmcSafeWarning1, mmcSafeWarning2;
    public static ComboSetting mode;
    public static DoubleSliderSetting mmcSafeDelay;

    private LinkedList<Packet<?>> mmcPackets = new LinkedList<>();
    private boolean mmc;

    private boolean c16 = false;
    private boolean c0d = false;
    private boolean transaction = false;
    private boolean isOnCombat = false;
    private int flags = 0;
    private boolean execute = false;
    private boolean jump = false;

    public Disabler() {
        super("Disabler", ModuleCategory.other);

        this.registerSetting(warning = new DescriptionSetting("WILL BAN DONT USE"));
        this.registerSetting(mode = new ComboSetting("Mode", Mode.MMCSafe));
        this.registerSetting(
                mmcSafeWarning1 = new DescriptionSetting(EnumChatFormatting.GRAY + "Difference between min and max"));
        this.registerSetting(
                mmcSafeWarning2 = new DescriptionSetting(EnumChatFormatting.GRAY + "should be less than 5."));
        this.registerSetting(mmcSafeDelay = new DoubleSliderSetting("MMCSafe Delay", 77, 80, 10, 200, 1));

        this.registerSetting(new DescriptionSetting(
                EnumChatFormatting.BOLD.toString() + EnumChatFormatting.AQUA + "GHOST CLIENT WITH DISABLER OP"));
    }

    @Override
    public void onEnable() {
        mmcPackets.clear();
    }

    @Subscribe
    public void onPacket(PacketEvent e) {
        if (mode.getMode() == Mode.MMCSafe) {
            if (e.isOutgoing() && !mmc) {
                if (e.getPacket() instanceof C00PacketKeepAlive) {
                    mmcPackets.add(e.getPacket());
                    e.cancel();
                }

                if (e.getPacket() instanceof C0FPacketConfirmTransaction) {
                    mmcPackets.add(e.getPacket());
                    e.cancel();
                }

                int packetsCap = Utils.Java.randomInt(mmcSafeDelay.getInputMin(), mmcSafeDelay.getMax());

                while (mmcPackets.size() >= packetsCap) {
                    mmc = true;
                    mc.thePlayer.sendQueue.addToSendQueue(mmcPackets.poll());
                }
                mmc = false;
            }
        } else if (mode.getMode() == Mode.Grim) {
            if (e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
                C08PacketPlayerBlockPlacement packet = (C08PacketPlayerBlockPlacement) e.getPacket();
                if (packet.getPlacedBlockDirection() >= 0 && packet.getPlacedBlockDirection() <= 5) {
                    e.cancel();
                    try {
                        int px = (Integer) ObfuscationReflectionHelper.getPrivateValue(C08PacketPlayerBlockPlacement.class, packet, "xPosition", "field_149583_a");
                        int py = (Integer) ObfuscationReflectionHelper.getPrivateValue(C08PacketPlayerBlockPlacement.class, packet, "yPosition", "field_149581_b");
                        int pz = (Integer) ObfuscationReflectionHelper.getPrivateValue(C08PacketPlayerBlockPlacement.class, packet, "zPosition", "field_149582_c");
                        mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(
                                px,
                                py,
                                pz,
                                6 + packet.getPlacedBlockDirection() * 7,
                                packet.getStack(),
                                packet.getPlacedBlockOffsetX(),
                                packet.getPlacedBlockOffsetY(),
                                packet.getPlacedBlockOffsetZ()
                        ));
                    } catch (Exception ex) {
                        // Failed to read packet fields
                    }
                }
            }
        } else if (mode.getMode() == Mode.Watchdog) {
            if (e.getPacket() instanceof net.minecraft.network.play.server.S07PacketRespawn) {
                flags = 0;
                execute = false;
                jump = true;
            } else if (e.getPacket() instanceof net.minecraft.network.play.server.S08PacketPlayerPosLook) {
                if (++flags >= 20) {
                    execute = false;
                    flags = 0;
                }
            } else if (e.getPacket() instanceof net.minecraft.network.play.client.C16PacketClientStatus) {
                if (c16) e.cancel();
                c16 = true;
            } else if (e.getPacket() instanceof net.minecraft.network.play.client.C0DPacketCloseWindow) {
                if (c0d) e.cancel();
                c0d = true;
            }
        } else if (mode.getMode() == Mode.Verus) {
            if (e.getPacket() instanceof net.minecraft.network.play.server.S32PacketConfirmTransaction) {
                e.cancel();
                mc.getNetHandler().addToSendQueue(new C0FPacketConfirmTransaction(
                        transaction ? 1 : -1,
                        (short) (transaction ? -1 : 1),
                        transaction
                ));
                transaction = !transaction;
            }
        }
    }

    @Subscribe
    public void onUpdate(keystrokesmod.client.event.impl.UpdateEvent e) {
        if (mode.getMode() == Mode.Watchdog) {
            if (jump) {
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.jump();
                }
            }
            c16 = false;
            c0d = false;
            if (mc.currentScreen instanceof net.minecraft.client.gui.inventory.GuiInventory) {
                int tickDiv = mc.thePlayer.isPotionActive(net.minecraft.potion.Potion.moveSpeed) ? 3 : 4;
                if (mc.thePlayer.ticksExisted % tickDiv == 0) {
                    mc.getNetHandler().addToSendQueue(new net.minecraft.network.play.client.C0DPacketCloseWindow());
                } else if (mc.thePlayer.ticksExisted % tickDiv == 1) {
                    mc.getNetHandler().addToSendQueue(new net.minecraft.network.play.client.C16PacketClientStatus(net.minecraft.network.play.client.C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
                }
            }
        }
    }

    public enum Mode {
        MMCSafe, Grim, Watchdog, Verus
    }
}
