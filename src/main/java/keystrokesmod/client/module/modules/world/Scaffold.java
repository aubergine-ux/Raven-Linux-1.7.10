package keystrokesmod.client.module.modules.world;



import com.google.common.eventbus.Subscribe;



import keystrokesmod.client.event.impl.GameLoopEvent;

import keystrokesmod.client.event.impl.LookEvent;

import keystrokesmod.client.event.impl.MoveInputEvent;

import keystrokesmod.client.event.impl.Render2DEvent;

import keystrokesmod.client.event.impl.UpdateEvent;

import keystrokesmod.client.module.Module;

import keystrokesmod.client.module.Module.ModuleCategory;

import keystrokesmod.client.module.setting.impl.SliderSetting;

import keystrokesmod.client.module.setting.impl.TickSetting;

import keystrokesmod.client.utils.Utils;

import keystrokesmod.client.utils.font.FontUtil;

import net.minecraft.client.gui.ScaledResolution;

import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

public class Scaffold extends Module {

    private TickSetting eagle;
    private SliderSetting rps;
    private TickSetting render;
    private SliderSetting renderColorR;
    private SliderSetting renderColorG;
    private SliderSetting renderColorB;
    private SliderSetting renderAlpha;

    private float yaw, pitch;
    private int blockCount;
    private int targetBlockX, targetBlockY, targetBlockZ;
    private boolean hasTargetBlock;

    public Scaffold() {
        super("Scaffold", ModuleCategory.world);
        this.registerSetting(eagle = new TickSetting("Shift", false));
        this.registerSetting(rps = new SliderSetting("Rotation speed", 80, 0, 300, 1));
        this.registerSetting(render = new TickSetting("Render", true));
        this.registerSetting(renderColorR = new SliderSetting("RenderColor-R", 255, 0, 255, 1));
        this.registerSetting(renderColorG = new SliderSetting("RenderColor-G", 0, 0, 255, 1));
        this.registerSetting(renderColorB = new SliderSetting("RenderColor-B", 0, 0, 255, 1));
        this.registerSetting(renderAlpha = new SliderSetting("RenderAlpha", 100, 0, 255, 1));
    }

    @Subscribe
    public void gameLoopEvent(GameLoopEvent e) {
        if (!Utils.Player.isPlayerInGame()) return;

        updateBlockCount();

        int underX = (int) Math.floor(mc.thePlayer.posX);
        int underY = (int) Math.floor(mc.thePlayer.posY - 1.0D);
        int underZ = (int) Math.floor(mc.thePlayer.posZ);

        if (mc.theWorld.isAirBlock(underX, underY, underZ)) {
            int neighborX = -1, neighborY = -1, neighborZ = -1;
            int side = -1;

            if (!mc.theWorld.isAirBlock(underX, underY - 1, underZ)) {
                neighborX = underX; neighborY = underY - 1; neighborZ = underZ;
                side = 1; // UP
            } else if (!mc.theWorld.isAirBlock(underX, underY, underZ - 1)) {
                neighborX = underX; neighborY = underY; neighborZ = underZ - 1;
                side = 3; // SOUTH
            } else if (!mc.theWorld.isAirBlock(underX, underY, underZ + 1)) {
                neighborX = underX; neighborY = underY; neighborZ = underZ + 1;
                side = 2; // NORTH
            } else if (!mc.theWorld.isAirBlock(underX + 1, underY, underZ)) {
                neighborX = underX + 1; neighborY = underY; neighborZ = underZ;
                side = 4; // WEST
            } else if (!mc.theWorld.isAirBlock(underX - 1, underY, underZ)) {
                neighborX = underX - 1; neighborY = underY; neighborZ = underZ;
                side = 5; // EAST
            }

            if (side != -1) {
                targetBlockX = underX;
                targetBlockY = underY;
                targetBlockZ = underZ;
                hasTargetBlock = true;
                float[] rots = getRotations(neighborX, neighborY, neighborZ, side);
                yaw = rots[0];
                pitch = rots[1];

                int blockSlot = Utils.Player.getBlockSlot();
                if (blockSlot != -1) {
                    int prevSlot = mc.thePlayer.inventory.currentItem;
                    mc.thePlayer.inventory.currentItem = blockSlot;

                    if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getHeldItem(), neighborX, neighborY, neighborZ, side, Vec3.createVectorHelper(neighborX + 0.5, neighborY + 0.5, neighborZ + 0.5))) {
                        mc.thePlayer.swingItem();
                    }

                    mc.thePlayer.inventory.currentItem = prevSlot;
                }
            } else {
                hasTargetBlock = false;
            }
        } else {
            yaw = mc.thePlayer.rotationYaw;
            pitch = mc.thePlayer.rotationPitch;
            hasTargetBlock = false;
        }

        if (eagle.isToggled()) {
            if (Utils.Player.playerOverAir()) {
                net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
            } else {
                net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
            }
        }
    }

    @Subscribe
    public void updateEvent(UpdateEvent e) {
        e.setPitch(pitch);
        e.setYaw(yaw);
    }

    @Subscribe
    public void lookEvent(LookEvent e) {
        e.setPitch(pitch);
        e.setYaw(yaw);
    }

    @Subscribe
    public void moveEvent(MoveInputEvent e) {
        e.setYaw(yaw);
    }

    @Subscribe
    public void onRender2D(Render2DEvent e) {
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        FontUtil.normal.drawCenteredSmoothString(blockCount + " blocks", (int) (sr.getScaledWidth() / 2f + 8), (int) (sr.getScaledHeight() / 2f - 4), blockCount <= 16 ? 0xff0000 : -1);
    }

    @Subscribe
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (!render.isToggled() || !hasTargetBlock) return;
        if (!Utils.Player.isPlayerInGame()) return;

        // Render target block
        renderBlock(targetBlockX, targetBlockY, targetBlockZ, event.partialTicks);
    }

    private void renderBlock(int posX, int posY, int posZ, float partialTicks) {
        double x = posX - RenderManager.renderPosX;
        double y = posY - RenderManager.renderPosY;
        double z = posZ - RenderManager.renderPosZ;

        float r = (float) renderColorR.getInput() / 255.0f;
        float g = (float) renderColorG.getInput() / 255.0f;
        float b = (float) renderColorB.getInput() / 255.0f;
        float a = (float) renderAlpha.getInput() / 255.0f;

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(r, g, b, a);

        // Render filled box
        renderFilledBox(0, 0, 0, 1, 1, 1);

        // Render outline
        GL11.glColor4f(r, g, b, 1.0f);
        renderOutlineBox(0, 0, 0, 1, 1, 1);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void renderFilledBox(double x, double y, double z, double width, double height, double depth) {
        GL11.glBegin(GL11.GL_QUADS);

        // Bottom
        GL11.glVertex3d(x, y, z);
        GL11.glVertex3d(x + width, y, z);
        GL11.glVertex3d(x + width, y, z + depth);
        GL11.glVertex3d(x, y, z + depth);

        // Top
        GL11.glVertex3d(x, y + height, z);
        GL11.glVertex3d(x, y + height, z + depth);
        GL11.glVertex3d(x + width, y + height, z + depth);
        GL11.glVertex3d(x + width, y + height, z);

        // Front
        GL11.glVertex3d(x, y, z);
        GL11.glVertex3d(x, y + height, z);
        GL11.glVertex3d(x + width, y + height, z);
        GL11.glVertex3d(x + width, y, z);

        // Back
        GL11.glVertex3d(x, y, z + depth);
        GL11.glVertex3d(x + width, y, z + depth);
        GL11.glVertex3d(x + width, y + height, z + depth);
        GL11.glVertex3d(x, y + height, z + depth);

        // Left
        GL11.glVertex3d(x, y, z);
        GL11.glVertex3d(x, y, z + depth);
        GL11.glVertex3d(x, y + height, z + depth);
        GL11.glVertex3d(x, y + height, z);

        // Right
        GL11.glVertex3d(x + width, y, z);
        GL11.glVertex3d(x + width, y + height, z);
        GL11.glVertex3d(x + width, y + height, z + depth);
        GL11.glVertex3d(x + width, y, z + depth);

        GL11.glEnd();
    }

    private void renderOutlineBox(double x, double y, double z, double width, double height, double depth) {
        GL11.glLineWidth(2.0f);
        GL11.glBegin(GL11.GL_LINES);

        // Bottom edges
        GL11.glVertex3d(x, y, z);
        GL11.glVertex3d(x + width, y, z);

        GL11.glVertex3d(x + width, y, z);
        GL11.glVertex3d(x + width, y, z + depth);

        GL11.glVertex3d(x + width, y, z + depth);
        GL11.glVertex3d(x, y, z + depth);

        GL11.glVertex3d(x, y, z + depth);
        GL11.glVertex3d(x, y, z);

        // Top edges
        GL11.glVertex3d(x, y + height, z);
        GL11.glVertex3d(x + width, y + height, z);

        GL11.glVertex3d(x + width, y + height, z);
        GL11.glVertex3d(x + width, y + height, z + depth);

        GL11.glVertex3d(x + width, y + height, z + depth);
        GL11.glVertex3d(x, y + height, z + depth);

        GL11.glVertex3d(x, y + height, z + depth);
        GL11.glVertex3d(x, y + height, z);

        // Vertical edges
        GL11.glVertex3d(x, y, z);
        GL11.glVertex3d(x, y + height, z);

        GL11.glVertex3d(x + width, y, z);
        GL11.glVertex3d(x + width, y + height, z);

        GL11.glVertex3d(x + width, y, z + depth);
        GL11.glVertex3d(x + width, y + height, z + depth);

        GL11.glVertex3d(x, y, z + depth);
        GL11.glVertex3d(x, y + height, z + depth);

        GL11.glEnd();
    }

    private float[] getRotations(int posX, int posY, int posZ, int facing) {
        int offsetX = getFrontOffsetX(facing);
        int offsetY = getFrontOffsetY(facing);
        int offsetZ = getFrontOffsetZ(facing);
        double x = posX + 0.5 - mc.thePlayer.posX + (double) offsetX / 2.0;
        double z = posZ + 0.5 - mc.thePlayer.posZ + (double) offsetZ / 2.0;
        double y = posY + 0.5 - (mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight()) + (double) offsetY / 2.0;
        double dist = MathHelper.sqrt_double(x * x + z * z);
        float yaw = (float) (Math.atan2(z, x) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) (-(Math.atan2(y, dist) * 180.0 / Math.PI));
        return new float[]{mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw), mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch)};
    }

    private static int getFrontOffsetX(int side) {
        switch (side) {
            case 4: return -1; // WEST
            case 5: return 1;  // EAST
            default: return 0;
        }
    }

    private static int getFrontOffsetY(int side) {
        switch (side) {
            case 0: return -1; // DOWN
            case 1: return 1;  // UP
            default: return 0;
        }
    }

    private static int getFrontOffsetZ(int side) {
        switch (side) {
            case 2: return -1; // NORTH
            case 3: return 1;  // SOUTH
            default: return 0;
        }
    }

    private void updateBlockCount() {
        blockCount = 0;
        for (int i = 0; i < 9; i++) {
            net.minecraft.item.ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof net.minecraft.item.ItemBlock) {
                blockCount += stack.stackSize;
            }
        }
    }
}
