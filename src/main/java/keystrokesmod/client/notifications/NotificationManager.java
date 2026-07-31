package keystrokesmod.client.notifications;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public class NotificationManager {
    private static LinkedBlockingQueue<Notification> pendingNotifications = new LinkedBlockingQueue<>();
    private static List<Notification> activeNotifications = new ArrayList<>();
    private static final int MAX_VISIBLE = 3;
    private static final int GAP = 4;

    public static void show(Notification notification) {
        pendingNotifications.add(notification);
    }

    public static void render() {
        activeNotifications.removeIf(n -> !n.isShown());

        while (activeNotifications.size() < MAX_VISIBLE && !pendingNotifications.isEmpty()) {
            Notification n = pendingNotifications.poll();
            n.show();
            activeNotifications.add(n);
        }

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft(), Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
        int baseY = sr.getScaledHeight() - 15;
        int height = 30;

        for (int i = 0; i < activeNotifications.size(); i++) {
            Notification n = activeNotifications.get(i);
            n.targetY = baseY - (i + 1) * (height + GAP);

            n.currentY += (n.targetY - n.currentY) * 0.15f;
            if (Math.abs(n.currentY - n.targetY) < 0.5f) n.currentY = n.targetY;

            long time = n.getTime();
            float alpha = 1f;
            int slideOffset = 0;

            if (time < n.fadedIn) {
                float progress = (float) time / n.fadedIn;
                float eased = 1 - (1 - progress) * (1 - progress);
                slideOffset = (int) (n.getWidth() * (1 - eased));
                alpha = Math.min(1f, progress * 2f);
            } else if (time > n.fadeOut) {
                float progress = (float) (time - n.fadeOut) / (n.end - n.fadeOut);
                alpha = 1f - progress;
                slideOffset = (int) (n.getWidth() * progress * progress);
            }

            int x = sr.getScaledWidth() - n.getWidth() - 5 + slideOffset;
            n.render(x, n.currentY, alpha);
        }
    }
}
