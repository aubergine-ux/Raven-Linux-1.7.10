package keystrokesmod.client.module.modules.client;

import java.util.Comparator;
import java.util.List;

import com.google.common.eventbus.Subscribe;

import keystrokesmod.client.event.impl.ForgeEvent;
import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.modules.combat.AimAssist;
import keystrokesmod.client.module.modules.world.AntiBot;
import keystrokesmod.client.module.setting.Setting;
import keystrokesmod.client.module.setting.impl.ComboSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import keystrokesmod.client.utils.Utils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

public class Targets extends Module {

    private static TickSetting friends, teams, invis, bots, naked, debug;
    private static SliderSetting fov, distance, lockDist;
    private static ComboSetting<SortMode> sortMode;

    public static EntityPlayer lockedTarget;

    public Targets() {
        super("Targets", ModuleCategory.client);
        this.registerSettings(
                        friends = new TickSetting("Target friends", false),
                        teams = new TickSetting("Target teammates", false),
                        invis = new TickSetting("Target invis", false),
                        bots = new TickSetting("Target bots", false),
                        naked = new TickSetting("Target naked", false),
                        fov = new SliderSetting("Fov", 30, 0, 360, 1),
                        distance = new SliderSetting("Distance", 3.5, 0, 10, 0.1),
                        sortMode = new ComboSetting("Sort mode", SortMode.Distance),
                        lockDist = new SliderSetting("Lock distance", 4, 0, 10, 0.1),
                        debug = new TickSetting("debug", false)
                        );
        onEnable();
    }

    @Override
    public boolean canBeEnabled() {
        return false;
    }

    @Override
    public void postApplyConfig() {
        guiButtonToggled(sortMode);
    }

    @Override
    public void guiButtonToggled(Setting b) {
        if(b == sortMode) {
            lockDist.hideComponent(sortMode.getMode() == SortMode.Lock);
        }
    }

    @Subscribe
    public void onForgeEvent(ForgeEvent fe) {
        if (fe.getEvent() instanceof AttackEntityEvent) {
            AttackEntityEvent e = ((AttackEntityEvent) fe.getEvent());
            lockedTarget = e.target instanceof EntityPlayer ? (EntityPlayer) e.target : lockedTarget;
        }
    }

    public static EntityPlayer getTarget() {
        List<EntityPlayer> en = Utils.Player.getClosePlayers(distance.getInput());
        en.removeIf(player -> !isValidTarget(player));
        if(debug.isToggled()) en.forEach(target -> Utils.Player.sendMessageToSelf(sortMode.getMode().sv.value(target) + " " ));
        return en.isEmpty() ? null : en.stream().min(Comparator.comparingDouble(target -> sortMode.getMode().sv.value(target))).get();
    }

    public static boolean isValidTarget(EntityPlayer ep) {
        return (
                (ep != mc.thePlayer)
                && (bots.isToggled() || !AntiBot.bot(ep))
                && (friends.isToggled() || !isAFriend(ep))
                && (teams.isToggled() || !isATeamMate(ep))
                && (invis.isToggled() || !ep.isInvisible())
                && (naked.isToggled() || !Utils.Player.isPlayerNaked(ep))
                && Utils.Player.fov(ep, (float) fov.getInput())
                );
    }

    public static boolean isAFriend(Entity entity) {
        if (entity == mc.thePlayer)
            return true;

        for (Entity wut : AimAssist.friends)
            if (wut.equals(entity))
                return true;
        return false;
    }

    public static boolean isATeamMate(Entity entity) {
        try {
            if (!(entity instanceof EntityPlayer)) return false;
            EntityPlayer bruhentity = (EntityPlayer) entity;

            // 1. Vanilla scoreboard team check (hypixel, etc.)
            if (mc.thePlayer.isOnSameTeam((EntityLivingBase) entity))
                return true;

            // 2. Color code prefix check - players with same team color
            String entityFormatted = bruhentity.getDisplayName();
            String playerFormatted = mc.thePlayer.getDisplayName();
            if (entityFormatted.length() > 1 && playerFormatted.length() > 1) {
                if (entityFormatted.charAt(1) == playerFormatted.charAt(1))
                    return true;
            }

            // 3. Hypixel-style prefix check (same name prefix like "[MVP+]", "TEAM_")
            String entityName = bruhentity.getDisplayName().replaceAll("§.", "");
            String playerName = mc.thePlayer.getDisplayName().replaceAll("§.", "");
            if (entityName.length() >= 2 && playerName.length() >= 2) {
                String entityPrefix = entityName.substring(0, 2);
                String playerPrefix = playerName.substring(0, 2);
                if (entityPrefix.equals(playerPrefix))
                    return true;
            }

            if (Raven.debugger) {
                Utils.Player.sendMessageToSelf("Not teammates with " + bruhentity.getCommandSenderName());
            }
        } catch (Exception e) {
            if (Raven.debugger)
                Utils.Player.sendMessageToSelf(e.getMessage());
        }
        return false;
    }


    public enum SortMode {
        Distance(player -> mc.thePlayer.getDistanceToEntity(player)),
        Hurttime(player -> (float) player.hurtTime),
        Fov(player -> Math.abs(Utils.Player.fovFromEntityf(player))),
        Lock(player -> player == lockedTarget ? 0f : 1f);

        private final SortValue sv;

        private SortMode(SortValue sv) {
            this.sv = sv;
        }

        public SortValue getSortValue() {
            return sv;
        }
    }

    @FunctionalInterface
    private interface SortValue {
        Float value(EntityPlayer player);
    }





}
