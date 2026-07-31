package keystrokesmod.client.module.modules.render;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.ForgeEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.RGBSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import keystrokesmod.client.utils.Utils;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Breadcrumbs extends Module {
    public static SliderSetting maxPoints, lineWidth, fadeTime;
    public static RGBSetting color;
    public static TickSetting fade;

    private final List<double[]> points = new ArrayList<>();
    private final List<Long> times = new ArrayList<>();
    private long lastPoint = 0;

    public Breadcrumbs() {
        super("Breadcrumbs", ModuleCategory.render);
        this.registerSetting(maxPoints = new SliderSetting("Max points", 500.0D, 50.0D, 2000.0D, 50.0D));
        this.registerSetting(lineWidth = new SliderSetting("Line Width", 2.0D, 0.5D, 6.0D, 0.5D));
        this.registerSetting(fadeTime = new SliderSetting("Fade time (s)", 5.0D, 1.0D, 30.0D, 1.0D));
        this.registerSetting(fade = new TickSetting("Fade", true));
        this.registerSetting(color = new RGBSetting("Color", 255, 100, 0));
    }

    @Override
    public void onEnable() {
        points.clear();
        times.clear();
    }

    @Override
    public void onDisable() {
        points.clear();
        times.clear();
    }

    @Subscribe
    public void onRender(ForgeEvent fe) {
        if (!(fe.getEvent() instanceof RenderWorldLastEvent)) return;
        if (!Utils.Player.isPlayerInGame() || mc.thePlayer == null) return;

        long now = System.currentTimeMillis();

        // add current position every 50ms
        if (now - lastPoint > 50) {
            points.add(new double[]{mc.thePlayer.posX, mc.thePlayer.posY + 0.1, mc.thePlayer.posZ});
            times.add(now);
            lastPoint = now;
        }

        // clean up old points
        while (points.size() > maxPoints.getInput()) {
            points.remove(0);
            times.remove(0);
        }

        long fadeMs = (long) (fadeTime.getInput() * 1000);
        while (!times.isEmpty() && now - times.get(0) > fadeMs) {
            points.remove(0);
            times.remove(0);
        }

        if (points.size() < 2) return;

        int rgbColor = this.color.getRGB();
        float r = ((rgbColor >> 16) & 0xFF) / 255.0f;
        float g = ((rgbColor >> 8) & 0xFF) / 255.0f;
        float b = (rgbColor & 0xFF) / 255.0f;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glLineWidth((float) lineWidth.getInput());

        Tessellator tessellator = Tessellator.instance;

        if (fade.isToggled()) {
            int total = points.size();
            for (int i = 0; i < total - 1; i++) {
                double[] p1 = points.get(i);
                double[] p2 = points.get(i + 1);
                float alpha = (float) i / total;
                tessellator.startDrawing(GL11.GL_LINES);
                tessellator.setColorRGBA_F(r, g, b, alpha * 0.8f);
                tessellator.addVertex(p1[0] - RenderManager.renderPosX,
                       p1[1] - RenderManager.renderPosY,
                       p1[2] - RenderManager.renderPosZ);
                tessellator.setColorRGBA_F(r, g, b, alpha * 0.8f);
                tessellator.addVertex(p2[0] - RenderManager.renderPosX,
                       p2[1] - RenderManager.renderPosY,
                       p2[2] - RenderManager.renderPosZ);
                tessellator.draw();
            }
        } else {
            tessellator.startDrawing(GL11.GL_LINE_STRIP);
            for (double[] p : points) {
                tessellator.setColorRGBA_F(r, g, b, 0.8f);
                tessellator.addVertex(p[0] - RenderManager.renderPosX,
                       p[1] - RenderManager.renderPosY,
                       p[2] - RenderManager.renderPosZ);
            }
            tessellator.draw();
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
    }
}
