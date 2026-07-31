package keystrokesmod.client.mixin.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import keystrokesmod.client.event.impl.MoveInputEvent;
import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.modules.player.SafeWalk;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;

@Mixin(priority = 995, value = Entity.class)
public abstract class MixinEntity {

    @Shadow
    public double motionX;
    @Shadow
    public double motionY;
    @Shadow
    public double motionZ;
    @Shadow
    public float rotationYaw;

    @Shadow
    public abstract boolean isSneaking();

    /**
     * Redirect the isSneaking() call inside moveEntity to implement SafeWalk.
     * In 1.7.10 vanilla moveEntity, there is:
     *   boolean flag = this.onGround && this.isSneaking() && this instanceof EntityPlayer;
     * We redirect isSneaking() to return true when SafeWalk is active,
     * making the edge-sneaking logic activate without actually sneaking.
     */
    @Redirect(method = "moveEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isSneaking()Z"))
    private boolean redirectIsSneakingForSafeWalk(Entity entity) {
        Minecraft mc = Minecraft.getMinecraft();
        if (entity == mc.thePlayer && mc.thePlayer.onGround) {
            Module safeWalk = Raven.moduleManager.getModuleByClazz(SafeWalk.class);

            if (safeWalk != null && safeWalk.isEnabled() && !SafeWalk.doShift.isToggled()) {
                boolean flag = true;

                if (SafeWalk.blocksOnly.isToggled()) {
                    ItemStack i = mc.thePlayer.getHeldItem();
                    if (i == null || !(i.getItem() instanceof ItemBlock)) {
                        flag = entity.isSneaking();
                    }
                }

                if (SafeWalk.lookDown.isToggled()) {
                    if (mc.thePlayer.rotationPitch < SafeWalk.pitchRange.getInputMin()
                            || mc.thePlayer.rotationPitch > SafeWalk.pitchRange.getInputMax()) {
                        flag = entity.isSneaking();
                    }
                }

                if (SafeWalk.shawtyMoment.isToggled()) {
                    if (mc.thePlayer.movementInput.moveForward > 0
                            && mc.thePlayer.movementInput.moveStrafe == 0) {
                        flag = entity.isSneaking();
                    }
                }

                return flag;
            }
        }
        return entity.isSneaking();
    }

    /**
     * @author mc code
     * @reason friction
     */
    @Overwrite
    public void moveFlying(float strafe, float forward, float fric) {
        MoveInputEvent e = new MoveInputEvent(strafe, forward, fric, this.rotationYaw);
        if((Object) this == Minecraft.getMinecraft().thePlayer)
            Raven.eventBus.post(e);

        strafe = e.getStrafe();
        forward = e.getForward();
        fric = e.getFriction();
        float yaw = e.getYaw();

        float f = (strafe * strafe) + (forward * forward);

        if (f >= 1.0E-4F) {
            f = MathHelper.sqrt_float(f);
            if (f < 1.0F) {
                f = 1.0F;
            }

            f = fric / f;
            strafe *= f;
            forward *= f;
            float f1 = MathHelper.sin((yaw * 3.1415927F) / 180.0F);
            float f2 = MathHelper.cos((yaw * 3.1415927F) / 180.0F);
            this.motionX += (strafe * f2) - (forward * f1);
            this.motionZ += (forward * f2) + (strafe * f1);
        }

    }

}
