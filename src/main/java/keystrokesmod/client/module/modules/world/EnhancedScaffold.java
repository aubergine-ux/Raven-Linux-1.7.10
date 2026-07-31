package keystrokesmod.client.module.modules.world;

import java.util.concurrent.ThreadLocalRandom;

import com.google.common.eventbus.Subscribe;

import keystrokesmod.client.event.impl.GameLoopEvent;
import keystrokesmod.client.event.impl.LookEvent;
import keystrokesmod.client.event.impl.MoveInputEvent;
import keystrokesmod.client.event.impl.Render2DEvent;
import keystrokesmod.client.event.impl.UpdateEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.Module.ModuleCategory;
import keystrokesmod.client.module.setting.impl.ComboSetting;
import keystrokesmod.client.module.setting.impl.DescriptionSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.module.setting.impl.TickSetting;
import keystrokesmod.client.utils.Utils;
import keystrokesmod.client.utils.font.FontUtil;
import net.minecraft.block.Block;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

public class EnhancedScaffold extends Module {

    public static DescriptionSetting desc;
    public static TickSetting eagle, safeWalk, tower, fastPlace, rotate, antiFall;
    public static TickSetting showBlockCount, showInfo, diagonalMode;
    public static ComboSetting mode, towerMode, blockSwitch;
    public static SliderSetting speed, reach, delay, towerSpeed, expandRadius;
    public static SliderSetting rotationSpeed, blockSwitchDelay;

    private float yaw, pitch;
    private int blockCount;
    private long lastPlaceTime = 0;
    private int lastBlockSlot = -1;
    private int towerHeight = 0;
    private boolean shouldJump = false;
    private long lastJumpTime = 0;

    // Side constants: 0=DOWN, 1=UP, 2=NORTH, 3=SOUTH, 4=WEST, 5=EAST
    private static final int SIDE_DOWN = 0;
    private static final int SIDE_UP = 1;
    private static final int SIDE_NORTH = 2;
    private static final int SIDE_SOUTH = 3;
    private static final int SIDE_WEST = 4;
    private static final int SIDE_EAST = 5;

    public enum ScaffoldMode {
        NORMAL, TOWER, BRIDGE, DIAGONAL
    }

    public enum TowerMode {
        JUMP, MOTION, VANILLA
    }

    public enum BlockSwitchMode {
        NONE, SMART, SWAP, INVENTORY
    }

    public EnhancedScaffold() {
        super("EnhancedScaffold", ModuleCategory.world);
        this.registerSetting(desc = new DescriptionSetting("Advanced block placement"));

        // Main settings
        this.registerSetting(mode = new ComboSetting("Mode", ScaffoldMode.NORMAL));
        this.registerSetting(rotate = new TickSetting("Rotate", true));
        this.registerSetting(fastPlace = new TickSetting("Fast Place", true));
        this.registerSetting(antiFall = new TickSetting("Anti Fall", true));
        this.registerSetting(diagonalMode = new TickSetting("Diagonal Mode", false));

        // Movement settings
        this.registerSetting(eagle = new TickSetting("Eagle", false));
        this.registerSetting(safeWalk = new TickSetting("Safe Walk", false));
        this.registerSetting(tower = new TickSetting("Tower", false));
        this.registerSetting(towerMode = new ComboSetting("Tower Mode", TowerMode.JUMP));
        this.registerSetting(towerSpeed = new SliderSetting("Tower Speed", 1.0, 0.5, 3.0, 0.1));

        // Block management
        this.registerSetting(blockSwitch = new ComboSetting("Block Switch", BlockSwitchMode.SMART));
        this.registerSetting(blockSwitchDelay = new SliderSetting("Switch Delay", 100, 0, 500, 10));
        this.registerSetting(expandRadius = new SliderSetting("Expand Radius", 0, 0, 3, 1));

        // Speed and precision
        this.registerSetting(speed = new SliderSetting("Place Speed", 8.0, 1.0, 20.0, 1.0));
        this.registerSetting(delay = new SliderSetting("Place Delay", 50, 0, 200, 10));
        this.registerSetting(reach = new SliderSetting("Reach", 4.5, 3.0, 6.0, 0.1));
        this.registerSetting(rotationSpeed = new SliderSetting("Rotation Speed", 30, 1, 180, 1));

        // Visual settings
        this.registerSetting(showBlockCount = new TickSetting("Show Block Count", true));
        this.registerSetting(showInfo = new TickSetting("Show Info", true));
    }

    @Override
    public void onEnable() {
        blockCount = 0;
        lastPlaceTime = 0;
        lastBlockSlot = -1;
        towerHeight = 0;
        shouldJump = false;
        lastJumpTime = 0;
    }

    @Subscribe
    public void onGameLoopEvent(GameLoopEvent event) {
        if (!Utils.Player.isPlayerInGame()) return;

        updateBlockCount();
        handleScaffold();
        handleTower();
        handleEagle();
        handleSafeWalk();
    }

    @Subscribe
    public void onUpdateEvent(UpdateEvent event) {
        if (!Utils.Player.isPlayerInGame()) return;

        if (rotate.isToggled()) {
            event.setYaw(yaw);
            event.setPitch(pitch);
        }

        handleAntiFall();
    }

    @Subscribe
    public void onLookEvent(LookEvent event) {
        if (!Utils.Player.isPlayerInGame() || !rotate.isToggled()) return;

        event.setYaw(yaw);
        event.setPitch(pitch);
    }

    @Subscribe
    public void onMoveInputEvent(MoveInputEvent event) {
        if (!Utils.Player.isPlayerInGame()) return;

        if (safeWalk.isToggled() && Utils.Player.playerOverAir()) {
            // Prevent walking off edges
            event.setForward(0);
            event.setStrafe(0);
        }
    }

    @Subscribe
    public void onRender2DEvent(Render2DEvent event) {
        if (!Utils.Player.isPlayerInGame()) return;

        if (showBlockCount.isToggled() || showInfo.isToggled()) {
            renderInfo();
        }
    }

    private void handleScaffold() {
        ScaffoldMode currentMode = (ScaffoldMode) mode.getMode();

        switch (currentMode) {
            case NORMAL:
                placeBlockUnderPlayer();
                break;
            case TOWER:
                placeBlockUnderPlayer();
                break;
            case BRIDGE:
                placeBridgeBlocks();
                break;
            case DIAGONAL:
                placeDiagonalBlocks();
                break;
        }
    }

    private void placeBlockUnderPlayer() {
        int underX = (int) Math.floor(mc.thePlayer.posX);
        int underY = (int) Math.floor(mc.thePlayer.posY - 1.0);
        int underZ = (int) Math.floor(mc.thePlayer.posZ);

        if (!mc.theWorld.isAirBlock(underX, underY, underZ)) return;

        // Find suitable position to place block
        int[][] positions = getExpandPositions(underX, underY, underZ);
        int bestPosX = Integer.MIN_VALUE, bestPosY = 0, bestPosZ = 0;
        int bestSide = -1;

        for (int[] pos : positions) {
            int px = pos[0], py = pos[1], pz = pos[2];
            if (mc.theWorld.isAirBlock(px, py, pz)) continue;

            int[] sides = {SIDE_UP, SIDE_NORTH, SIDE_SOUTH, SIDE_EAST, SIDE_WEST};
            for (int side : sides) {
                int adjX = px + getOffsetX(side);
                int adjY = py + getOffsetY(side);
                int adjZ = pz + getOffsetZ(side);
                if (adjX == underX && adjY == underY && adjZ == underZ && canPlaceBlock(px, py, pz, side)) {
                    double distance = mc.thePlayer.getDistanceSq(px + 0.5, py + 0.5, pz + 0.5);
                    if (distance <= reach.getInput() * reach.getInput()) {
                        bestPosX = px;
                        bestPosY = py;
                        bestPosZ = pz;
                        bestSide = side;
                        break;
                    }
                }
            }
            if (bestPosX != Integer.MIN_VALUE) break;
        }

        if (bestPosX != Integer.MIN_VALUE && bestSide != -1) {
            if (rotate.isToggled()) {
                calculateRotations(bestPosX + 0.5, bestPosY + 0.5, bestPosZ + 0.5);
            }

            if (canPlace()) {
                placeBlock(bestPosX, bestPosY, bestPosZ, bestSide);
            }
        }
    }

    private void placeBridgeBlocks() {
        Vec3 look = mc.thePlayer.getLookVec();
        double distance = 1.0;
        int frontX = (int) Math.floor(mc.thePlayer.posX + look.xCoord * distance);
        int frontY = (int) Math.floor(mc.thePlayer.posY);
        int frontZ = (int) Math.floor(mc.thePlayer.posZ + look.zCoord * distance);

        int underX = frontX;
        int underY = frontY - 1;
        int underZ = frontZ;

        if (!mc.theWorld.isAirBlock(underX, underY, underZ)) return;

        // Try to place block under the front position
        int[][] positions = {
            {underX, underY, underZ},
            {frontX, frontY, frontZ - 1}, {frontX, frontY, frontZ + 1},
            {frontX + 1, frontY, frontZ}, {frontX - 1, frontY, frontZ},
            {underX, underY, underZ - 1}, {underX, underY, underZ + 1},
            {underX + 1, underY, underZ}, {underX - 1, underY, underZ}
        };

        for (int[] pos : positions) {
            int px = pos[0], py = pos[1], pz = pos[2];
            if (!mc.theWorld.isAirBlock(px, py, pz)) {
                int side = getSideToPlace(px, py, pz, underX, underY, underZ);
                if (side != -1 && canPlaceBlock(px, py, pz, side)) {
                    if (rotate.isToggled()) {
                        calculateRotations(underX + 0.5, underY + 0.5, underZ + 0.5);
                    }

                    if (canPlace()) {
                        placeBlock(px, py, pz, side);
                    }
                    break;
                }
            }
        }
    }

    private void placeDiagonalBlocks() {
        // Place blocks in diagonal pattern for diagonal movement
        Vec3 motion = mc.thePlayer.getLookVec();
        double dx = motion.xCoord;
        double dz = motion.zCoord;

        // Normalize diagonal movement
        if (Math.abs(dx) > 0.1 && Math.abs(dz) > 0.1) {
            int targetX = (int) Math.floor(mc.thePlayer.posX + Math.signum(dx));
            int targetY = (int) Math.floor(mc.thePlayer.posY - 1);
            int targetZ = (int) Math.floor(mc.thePlayer.posZ + Math.signum(dz));

            if (mc.theWorld.isAirBlock(targetX, targetY, targetZ)) {
                placeBlockUnderPlayer();
            }
        } else {
            placeBlockUnderPlayer();
        }
    }

    private int[][] getExpandPositions(int cx, int cy, int cz) {
        int radius = (int) expandRadius.getInput();
        if (radius == 0) {
            return new int[][]{{cx, cy, cz}};
        }

        java.util.List<int[]> positions = new java.util.ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                positions.add(new int[]{cx + x, cy, cz + z});
            }
        }

        return positions.toArray(new int[0][]);
    }

    private int getSideToPlace(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        if (toX == fromX && toY == fromY + 1 && toZ == fromZ) return SIDE_UP;
        if (toX == fromX && toY == fromY - 1 && toZ == fromZ) return SIDE_DOWN;
        if (toX == fromX && toY == fromY && toZ == fromZ - 1) return SIDE_NORTH;
        if (toX == fromX && toY == fromY && toZ == fromZ + 1) return SIDE_SOUTH;
        if (toX == fromX + 1 && toY == fromY && toZ == fromZ) return SIDE_EAST;
        if (toX == fromX - 1 && toY == fromY && toZ == fromZ) return SIDE_WEST;
        return -1;
    }

    private void handleTower() {
        if (!tower.isToggled()) return;

        TowerMode currentMode = (TowerMode) towerMode.getMode();

        switch (currentMode) {
            case JUMP:
                handleJumpTower();
                break;
            case MOTION:
                handleMotionTower();
                break;
            case VANILLA:
                handleVanillaTower();
                break;
        }
    }

    private void handleJumpTower() {
        if (mc.thePlayer.onGround && mc.gameSettings.keyBindJump.isKeyDown()) {
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastJumpTime > 100) {
                mc.thePlayer.jump();
                lastJumpTime = currentTime;

                // Apply tower speed
                mc.thePlayer.motionY = 0.42f * (float) towerSpeed.getInput();
            }
        }
    }

    private void handleMotionTower() {
        if (mc.gameSettings.keyBindJump.isKeyDown()) {
            mc.thePlayer.motionY = 0.42f * (float) towerSpeed.getInput();
        }
    }

    private void handleVanillaTower() {
        if (mc.gameSettings.keyBindJump.isKeyDown()) {
            mc.thePlayer.motionY = 0.42f;
        }
    }

    private void handleEagle() {
        if (!eagle.isToggled()) return;

        boolean shouldSneak = Utils.Player.playerOverAir() &&
                           (mc.thePlayer.motionY < 0 || mc.gameSettings.keyBindJump.isKeyDown());

        if (shouldSneak) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
        } else {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        }
    }

    private void handleSafeWalk() {
        if (!safeWalk.isToggled()) return;

        // This is handled in MoveInputEvent
    }

    private void handleAntiFall() {
        if (!antiFall.isToggled()) return;

        if (mc.thePlayer.fallDistance > 2.0f && !mc.thePlayer.onGround) {
            int underX = (int) Math.floor(mc.thePlayer.posX);
            int underY = (int) Math.floor(mc.thePlayer.posY - 1.0);
            int underZ = (int) Math.floor(mc.thePlayer.posZ);

            if (mc.theWorld.isAirBlock(underX, underY, underZ)) {
                // Try to place block to prevent fall damage
                placeBlockUnderPlayer();
            }
        }
    }

    private boolean canPlace() {
        long currentTime = System.currentTimeMillis();
        return currentTime - lastPlaceTime >= delay.getInput();
    }

    private boolean canPlaceBlock(int px, int py, int pz, int side) {
        if (mc.theWorld.getBlock(px, py, pz).isNormalCube()) {
            return true;
        }

        // Check if we can place on this side
        int offX = px + getOffsetX(side);
        int offY = py + getOffsetY(side);
        int offZ = pz + getOffsetZ(side);
        return mc.theWorld.isAirBlock(offX, offY, offZ);
    }

    private void placeBlock(int px, int py, int pz, int side) {
        if (!switchToBestBlock()) return;

        mc.playerController.onPlayerRightClick(
            mc.thePlayer,
            mc.theWorld,
            mc.thePlayer.getHeldItem(),
            px,
            py,
            pz,
            side,
            Vec3.createVectorHelper(px + 0.5, py + 0.5, pz + 0.5)
        );

        mc.thePlayer.swingItem();
        lastPlaceTime = System.currentTimeMillis();
    }

    private boolean switchToBestBlock() {
        BlockSwitchMode switchMode = (BlockSwitchMode) blockSwitch.getMode();

        switch (switchMode) {
            case NONE:
                return mc.thePlayer.getHeldItem() != null &&
                       isBlock(mc.thePlayer.getHeldItem().getItem());

            case SMART:
                return smartBlockSwitch();

            case SWAP:
                return swapToBestBlock();

            case INVENTORY:
                return inventoryBlockSwitch();

            default:
                return false;
        }
    }

    private boolean smartBlockSwitch() {
        int bestSlot = findBestBlockSlot();
        if (bestSlot == -1) return false;

        if (bestSlot != mc.thePlayer.inventory.currentItem) {
            mc.thePlayer.inventory.currentItem = bestSlot;
        }

        return true;
    }

    private boolean swapToBestBlock() {
        int bestSlot = findBestBlockSlot();
        if (bestSlot == -1) return false;

        if (bestSlot != mc.thePlayer.inventory.currentItem) {
            // Swap with current slot
            ItemStack current = mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem);
            ItemStack best = mc.thePlayer.inventory.getStackInSlot(bestSlot);

            mc.thePlayer.inventory.setInventorySlotContents(mc.thePlayer.inventory.currentItem, best);
            mc.thePlayer.inventory.setInventorySlotContents(bestSlot, current);
        }

        return true;
    }

    private boolean inventoryBlockSwitch() {
        // More complex inventory management - simplified for now
        return smartBlockSwitch();
    }

    private int findBestBlockSlot() {
        int bestSlot = -1;
        int bestCount = 0;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && isBlock(stack.getItem())) {
                int count = stack.stackSize;
                if (count > bestCount) {
                    bestCount = count;
                    bestSlot = i;
                }
            }
        }

        return bestSlot;
    }

    private boolean isBlock(net.minecraft.item.Item item) {
        return item instanceof net.minecraft.item.ItemBlock;
    }

    private void calculateRotations(double x, double y, double z) {
        double dx = x - mc.thePlayer.posX;
        double dy = y - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = z - mc.thePlayer.posZ;
        double distance = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(dy, distance)));

        // Smooth rotation
        float speed = (float) rotationSpeed.getInput();
        yaw = mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(targetYaw - mc.thePlayer.rotationYaw) * speed / 180.0f;
        pitch = mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(targetPitch - mc.thePlayer.rotationPitch) * speed / 180.0f;
    }

    private void updateBlockCount() {
        blockCount = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && isBlock(stack.getItem())) {
                blockCount += stack.stackSize;
            }
        }
    }

    private void renderInfo() {
        ScaledResolution sr = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
        int x = 5;
        int y = sr.getScaledHeight() - 50;

        if (showBlockCount.isToggled()) {
            String countText = "Blocks: " + blockCount;
            int color = blockCount > 16 ? 0xFFFF0000 : 0xFF00FF00;
            FontUtil.normal.drawString(countText, x, y, color);
            y += 12;
        }

        if (showInfo.isToggled()) {
            ScaffoldMode currentMode = (ScaffoldMode) mode.getMode();
            String modeText = "Mode: " + currentMode.name();
            FontUtil.normal.drawString(modeText, x, y, 0xFFFFFF);
            y += 12;

            if (tower.isToggled()) {
                TowerMode tm = (TowerMode) this.towerMode.getMode();
                String towerText = "Tower: " + tm.name();
                FontUtil.normal.drawString(towerText, x, y, 0xFFFFFF);
            }
        }
    }

    // Helper methods for side offsets
    private static int getOffsetX(int side) {
        switch (side) {
            case SIDE_WEST: return -1;
            case SIDE_EAST: return 1;
            default: return 0;
        }
    }

    private static int getOffsetY(int side) {
        switch (side) {
            case SIDE_DOWN: return -1;
            case SIDE_UP: return 1;
            default: return 0;
        }
    }

    private static int getOffsetZ(int side) {
        switch (side) {
            case SIDE_NORTH: return -1;
            case SIDE_SOUTH: return 1;
            default: return 0;
        }
    }
}
