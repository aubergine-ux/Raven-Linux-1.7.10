package keystrokesmod.client.module.modules.other;

import com.google.common.eventbus.Subscribe;
import keystrokesmod.client.event.impl.TickEvent;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.setting.impl.DescriptionSetting;
import keystrokesmod.client.module.setting.impl.SliderSetting;
import keystrokesmod.client.utils.DimensionHelper;
import keystrokesmod.client.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;

public class WaterBucket extends Module {
    public static DescriptionSetting moduleDesc;
    public static SliderSetting distance;
    private boolean handling;

    public WaterBucket() {
        super("Water bucket", ModuleCategory.other);
        this.registerSetting(moduleDesc = new DescriptionSetting("Credits: aycy - Disabled in the Nether"));
        this.registerSetting(distance = new SliderSetting("Fall Distance", 3, 1, 10, 0.1));
    }

    @Override
    public boolean canBeEnabled() {
        return !DimensionHelper.isPlayerInNether();
    }

    @Subscribe
    public void onTick(TickEvent ev) {
        if (Utils.Player.isPlayerInGame() && !mc.isGamePaused()) {
            if (DimensionHelper.isPlayerInNether())
                this.disable();

            if (this.inPosition() && this.holdWaterBucket()) {
                this.handling = true;
            }

            if (this.handling) {
                this.mlg();
                if (mc.thePlayer.onGround || mc.thePlayer.motionY > 0.0D) {
                    this.reset();
                }
            }

        }
    }

    private boolean inPosition() {
        if (mc.thePlayer.motionY < -0.6D && !mc.thePlayer.onGround && !mc.thePlayer.capabilities.isFlying
                && !mc.thePlayer.capabilities.isCreativeMode && !this.handling && mc.thePlayer.fallDistance > distance.getInput()) {
            int playerX = MathHelper.floor_double(mc.thePlayer.posX);
            int playerY = MathHelper.floor_double(mc.thePlayer.posY);
            int playerZ = MathHelper.floor_double(mc.thePlayer.posZ);

            for (int i = 1; i < 3; ++i) {
                int bx = playerX;
                int by = playerY - i;
                int bz = playerZ;
                Block block = mc.theWorld.getBlock(bx, by, bz);
                if (block.getMaterial().isSolid()) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean holdWaterBucket() {
        if (this.containsItem(mc.thePlayer.getHeldItem(), Items.water_bucket)) {
            return true;
        } else {
            for (int i = 0; i < InventoryPlayer.getHotbarSize(); ++i) {
                if (this.containsItem(mc.thePlayer.inventory.mainInventory[i], Items.water_bucket)) {
                    mc.thePlayer.inventory.currentItem = i;
                    return true;
                }
            }

            return false;
        }
    }

    private void mlg() {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (this.containsItem(heldItem, Items.water_bucket) && mc.thePlayer.rotationPitch >= 45.0F) {
            MovingObjectPosition object = mc.objectMouseOver;
            if (object != null && object.typeOfHit == MovingObjectType.BLOCK && object.sideHit == 1) {
                // Auto-click the water bucket
                mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, heldItem);
            }
        }

    }

    private void reset() {
        ItemStack heldItem = mc.thePlayer.getHeldItem();
        if (this.containsItem(heldItem, Items.bucket)) {
            mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, heldItem);
        }

        this.handling = false;
    }

    private boolean containsItem(ItemStack itemStack, Item item) {
        return itemStack != null && itemStack.getItem() == item;
    }
}
