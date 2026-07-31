package keystrokesmod.client.module.modules.player;

import com.google.common.eventbus.Subscribe;

import keystrokesmod.client.event.impl.UpdateEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.Module.ModuleCategory;
import keystrokesmod.client.module.setting.impl.ComboSetting;
import keystrokesmod.client.module.setting.impl.DescriptionSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import keystrokesmod.client.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.input.Mouse;

public class FastBreak extends Module {

    public static DescriptionSetting desc;
    public static ComboSetting mode;
    public static TickSetting onlyCreative, onlySurvival, instantBreak, checkReach;
    public static SliderSetting breakSpeed, breakDelay, range;
    public static SliderSetting swingSpeed, packetDelay;

    private long lastBreakTime = 0;
    private long lastSwingTime = 0;
    private boolean hasCurrentBlock = false;
    private int currentBlockX, currentBlockY, currentBlockZ;
    private int breakStage = 0;
    private boolean isBreaking = false;

    public enum BreakMode {
        INSTANT,
        PACKET,
        TIMER,
        SWITCH,
        HYBRID
    }

    public FastBreak() {
        super("FastBreak", ModuleCategory.player);
        this.registerSetting(desc = new DescriptionSetting("Break blocks faster"));
        this.registerSetting(mode = new ComboSetting("Mode", BreakMode.PACKET));

        // Conditions
        this.registerSetting(onlyCreative = new TickSetting("Only Creative", false));
        this.registerSetting(onlySurvival = new TickSetting("Only Survival", true));
        this.registerSetting(instantBreak = new TickSetting("Instant Break", false));
        this.registerSetting(checkReach = new TickSetting("Check Reach", true));

        // Speed settings
        this.registerSetting(breakSpeed = new SliderSetting("Break Speed", 1.5, 1.0, 5.0, 0.1));
        this.registerSetting(breakDelay = new SliderSetting("Break Delay (ms)", 50, 0, 500, 10));
        this.registerSetting(range = new SliderSetting("Range", 6.0, 3.0, 10.0, 0.5));

        // Advanced settings
        this.registerSetting(swingSpeed = new SliderSetting("Swing Speed", 1.0, 0.5, 3.0, 0.1));
        this.registerSetting(packetDelay = new SliderSetting("Packet Delay (ms)", 25, 0, 200, 5));
    }

    @Override
    public void onEnable() {
        lastBreakTime = 0;
        lastSwingTime = 0;
        hasCurrentBlock = false;
        breakStage = 0;
        isBreaking = false;
    }

    @Subscribe
    public void onUpdateEvent(UpdateEvent event) {
        if (!Utils.Player.isPlayerInGame()) return;

        // Track breaking state from mouse and objectMouseOver
        if (Mouse.isButtonDown(0) && mc.objectMouseOver != null
                && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            int bx = mc.objectMouseOver.blockX;
            int by = mc.objectMouseOver.blockY;
            int bz = mc.objectMouseOver.blockZ;
            Block block = mc.theWorld.getBlock(bx, by, bz);
            if (block != null && block != Blocks.air) {
                if (!isBreaking || !hasCurrentBlock || bx != currentBlockX || by != currentBlockY || bz != currentBlockZ) {
                    currentBlockX = bx;
                    currentBlockY = by;
                    currentBlockZ = bz;
                    hasCurrentBlock = true;
                    isBreaking = true;
                    breakStage = 0;
                }
            }
        } else {
            if (isBreaking) {
                isBreaking = false;
                hasCurrentBlock = false;
                breakStage = 0;
            }
        }

        // Check game mode conditions
        if (onlyCreative.isToggled() && !mc.thePlayer.capabilities.isCreativeMode) return;
        if (onlySurvival.isToggled() && mc.thePlayer.capabilities.isCreativeMode) return;

        BreakMode currentMode = (BreakMode) mode.getMode();

        switch (currentMode) {
            case INSTANT:
                handleInstantBreak();
                break;
            case PACKET:
                handlePacketBreak();
                break;
            case TIMER:
                handleTimerBreak();
                break;
            case SWITCH:
                handleSwitchBreak();
                break;
            case HYBRID:
                handleHybridBreak();
                break;
        }

        // Handle swinging
        handleSwinging();
    }

    private void handleInstantBreak() {
        if (!isBreaking || !hasCurrentBlock) return;

        if (checkReach.isToggled() && !isInRange()) return;

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastBreakTime >= breakDelay.getInput()) {
            mc.playerController.onPlayerDestroyBlock(currentBlockX, currentBlockY, currentBlockZ, 0);
            mc.thePlayer.swingItem();
            lastBreakTime = currentTime;
        }
    }

    private void handlePacketBreak() {
        if (!isBreaking || !hasCurrentBlock) return;

        if (checkReach.isToggled() && !isInRange()) return;

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastBreakTime >= packetDelay.getInput()) {
            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                1, currentBlockX, currentBlockY, currentBlockZ, 0
            ));

            mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                0, currentBlockX, currentBlockY, currentBlockZ, 0
            ));

            breakStage++;

            if (breakStage >= (int) (10.0f / breakSpeed.getInput())) {
                mc.playerController.onPlayerDestroyBlock(currentBlockX, currentBlockY, currentBlockZ, 0);
                isBreaking = false;
                hasCurrentBlock = false;
                breakStage = 0;
            }

            lastBreakTime = currentTime;
        }
    }

    private void handleTimerBreak() {
        if (!isBreaking || !hasCurrentBlock) return;

        if (checkReach.isToggled() && !isInRange()) return;

        Utils.Client.getTimer().timerSpeed = (float) breakSpeed.getInput();

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSwingTime >= (1000.0f / (20.0f * swingSpeed.getInput()))) {
            mc.thePlayer.swingItem();
            lastSwingTime = currentTime;
        }
    }

    private void handleSwitchBreak() {
        if (!isBreaking || !hasCurrentBlock) return;

        if (checkReach.isToggled() && !isInRange()) return;

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastBreakTime >= breakDelay.getInput()) {
            if (breakStage % 2 == 0) {
                mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                    0, currentBlockX, currentBlockY, currentBlockZ, 0
                ));
            } else {
                mc.getNetHandler().addToSendQueue(new C07PacketPlayerDigging(
                    2, currentBlockX, currentBlockY, currentBlockZ, 0
                ));
            }

            breakStage++;

            if (breakStage >= (int) (10.0f / breakSpeed.getInput())) {
                mc.playerController.onPlayerDestroyBlock(currentBlockX, currentBlockY, currentBlockZ, 0);
                isBreaking = false;
                hasCurrentBlock = false;
                breakStage = 0;
            }

            lastBreakTime = currentTime;
        }
    }

    private void handleHybridBreak() {
        if (!isBreaking || !hasCurrentBlock) return;

        if (checkReach.isToggled() && !isInRange()) return;

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastBreakTime >= packetDelay.getInput()) {
            handlePacketBreak();
        }

        if (Utils.Client.getTimer().timerSpeed < breakSpeed.getInput()) {
            Utils.Client.getTimer().timerSpeed = Math.min(Utils.Client.getTimer().timerSpeed + 0.1f, (float) breakSpeed.getInput());
        }

        handleSwinging();
    }

    private void handleSwinging() {
        if (!isBreaking) return;

        long currentTime = System.currentTimeMillis();
        long swingInterval = (long) (1000.0f / (20.0f * swingSpeed.getInput()));

        if (currentTime - lastSwingTime >= swingInterval) {
            mc.thePlayer.swingItem();
            lastSwingTime = currentTime;
        }
    }

    private boolean isInRange() {
        if (!hasCurrentBlock || mc.thePlayer == null) return false;

        double distance = mc.thePlayer.getDistanceSq(currentBlockX + 0.5, currentBlockY + 0.5, currentBlockZ + 0.5);
        double maxRange = range.getInput() * range.getInput();

        return distance <= maxRange;
    }

    @Override
    public void onDisable() {
        Utils.Client.getTimer().timerSpeed = 1.0f;
        isBreaking = false;
        hasCurrentBlock = false;
        breakStage = 0;
    }

    public boolean isBreakingBlock() {
        return isBreaking;
    }

    public BreakMode getCurrentMode() {
        return (BreakMode) mode.getMode();
    }

    public double getBreakProgress() {
        if (!hasCurrentBlock || !isBreaking) return 0;

        Block block = mc.theWorld.getBlock(currentBlockX, currentBlockY, currentBlockZ);
        float blockHardness = block.getBlockHardness(mc.theWorld, currentBlockX, currentBlockY, currentBlockZ);
        if (blockHardness <= 0) return 1.0;

        return Math.min(1.0, (breakStage * breakSpeed.getInput()) / 10.0);
    }
}
