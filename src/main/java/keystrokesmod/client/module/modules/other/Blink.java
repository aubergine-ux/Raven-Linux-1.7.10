package keystrokesmod.client.module.modules.other;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.ForgeEvent;
import keystrokesmod.client.event.impl.PacketEvent;
import keystrokesmod.client.event.impl.Render2DEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.Module.ModuleCategory;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import keystrokesmod.client.utils.Utils;
import keystrokesmod.client.utils.font.FontUtil;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

public class Blink extends Module {
    
    public static SliderSetting maxPackets;
    public static SliderSetting delay;
    public static TickSetting onlyMovement;
    public static TickSetting showPackets;
    public static TickSetting autoRelease;
    
    private List<Packet> packetBuffer = new ArrayList<>();
    private long lastReleaseTime = 0;
    private boolean shouldRelease = false;

    public Blink() {
        super("Blink", ModuleCategory.other);
        this.registerSetting(maxPackets = new SliderSetting("Max Packets", 50, 10, 200, 5));
        this.registerSetting(delay = new SliderSetting("Auto Release Delay (ms)", 1000, 100, 5000, 100));
        this.registerSetting(onlyMovement = new TickSetting("Only Movement", false));
        this.registerSetting(showPackets = new TickSetting("Show Packets Count", true));
        this.registerSetting(autoRelease = new TickSetting("Auto Release", true));
    }

    @Subscribe
    public void onPacketSend(PacketEvent event) {
        if (!Utils.Player.isPlayerInGame()) {
            return;
        }
        
        Packet packet = event.getPacket();
        
        // Don't buffer certain packets that should always go through
        if (packet instanceof C00PacketKeepAlive) {
            return;
        }
        
        // Only buffer movement packets if enabled
        if (onlyMovement.isToggled() && !isMovementPacket(packet)) {
            return;
        }
        
        // Check if we should buffer this packet
        if (shouldBufferPacket(packet)) {
            event.setCancelled(true);
            packetBuffer.add(packet);
            
            // Auto release if we've hit the max packets
            if (packetBuffer.size() >= maxPackets.getInput()) {
                releasePackets();
            }
        }
    }
    
    @Subscribe
    public void onRender2D(Render2DEvent event) {
        if (!Utils.Player.isPlayerInGame() || !showPackets.isToggled()) {
            return;
        }
        
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int x = sr.getScaledWidth() / 2;
        int y = sr.getScaledHeight() - 30;
        
        // Draw packet count
        String text = "Blink: " + packetBuffer.size() + " packets";
        int color = packetBuffer.size() > maxPackets.getInput() * 0.8 ? 0xFFFF0000 : 0xFF00FF00;
        
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        // Background
        int width = (int) FontUtil.normal.getStringWidth(text) + 10;
        int height = 20;
        
        // Draw background
        GL11.glColor4f(0f, 0f, 0f, 0.7f);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2d(x - width/2, y - height/2);
        GL11.glVertex2d(x + width/2, y - height/2);
        GL11.glVertex2d(x + width/2, y + height/2);
        GL11.glVertex2d(x - width/2, y + height/2);
        GL11.glEnd();
        
        // Draw text
        FontUtil.normal.drawCenteredString(text, x, y - 5, color);
        
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private boolean shouldBufferPacket(Packet packet) {
        // Don't buffer if we're not in a state to blink
        if (!mc.thePlayer.onGround && !mc.thePlayer.capabilities.isFlying) {
            return false;
        }
        
        // Auto release check
        if (autoRelease.isToggled()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastReleaseTime > delay.getInput() && !packetBuffer.isEmpty()) {
                releasePackets();
                lastReleaseTime = currentTime;
                return false;
            }
        }
        
        return true;
    }

    private boolean isMovementPacket(Packet packet) {
        return packet instanceof C03PacketPlayer || 
               packet instanceof C0BPacketEntityAction ||
               packet instanceof C0APacketAnimation;
    }

    private void releasePackets() {
        if (!packetBuffer.isEmpty()) {
            // Create a copy to avoid ConcurrentModificationException
            List<Packet> packetsToSend = new ArrayList<>(packetBuffer);
            
            // Send all buffered packets
            for (Packet packet : packetsToSend) {
                try {
                    mc.thePlayer.sendQueue.addToSendQueue(packet);
                } catch (Exception e) {
                    // Ignore any exceptions during packet sending
                }
            }
            packetBuffer.clear();
        }
    }

    @Override
    public void onEnable() {
        packetBuffer.clear();
        lastReleaseTime = System.currentTimeMillis();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        releasePackets();
        super.onDisable();
    }
    
    // Manual release method (could be bound to a key)
    public void manualRelease() {
        if (isEnabled()) {
            releasePackets();
        }
    }
}
