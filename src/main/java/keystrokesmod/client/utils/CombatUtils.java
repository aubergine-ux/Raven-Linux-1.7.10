package keystrokesmod.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;

public class CombatUtils {
    public static boolean canTarget(Entity entity, boolean idk) {
        if (entity != null && entity != Minecraft.getMinecraft().thePlayer) {
            EntityLivingBase entityLivingBase = null;

            if (entity instanceof EntityLivingBase) {
                entityLivingBase = (EntityLivingBase) entity;
            }

            boolean isTeam = isTeam(Minecraft.getMinecraft().thePlayer, entity);
            boolean isVisible = (!entity.isInvisible());

            return isVisible
                    && (entity instanceof EntityPlayer && !isTeam && !idk || entity instanceof EntityAnimal
                            || entity instanceof EntityMob
                            || entity instanceof EntityLivingBase && entityLivingBase.isEntityAlive());
        } else {
            return false;
        }
    }

    public static boolean isTeam(EntityPlayer player, Entity entity) {
        if (!(entity instanceof EntityPlayer)) return false;
        EntityPlayer other = (EntityPlayer) entity;

        // 1. Vanilla scoreboard team check
        if (player.getTeam() != null && other.getTeam() != null) {
            return player.isOnSameTeam(other);
        }

        // 2. Display name color code check
        try {
            String playerDisplay = player.getDisplayName();
            String otherDisplay = other.getDisplayName();
            if (playerDisplay.length() > 1 && otherDisplay.length() > 1) {
                if (playerDisplay.charAt(1) == otherDisplay.charAt(1)) {
                    return true;
                }
            }
        } catch (Exception ignored) {}

        return false;
    }

}
