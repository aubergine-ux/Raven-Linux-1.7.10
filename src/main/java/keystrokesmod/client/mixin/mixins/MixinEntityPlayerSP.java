package keystrokesmod.client.mixin.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.authlib.GameProfile;

import keystrokesmod.client.main.Raven;
import keystrokesmod.client.module.Module;
import keystrokesmod.client.module.modules.movement.NoSlow;
import keystrokesmod.client.module.modules.movement.Sprint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.potion.Potion;
import net.minecraft.util.MovementInput;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

@Mixin(priority = 995, value = EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends AbstractClientPlayer {

    @Shadow
    public int sprintingTicksLeft;

    public MixinEntityPlayerSP(World p_i45074_1_, GameProfile p_i45074_2_) {
        super(p_i45074_1_, p_i45074_2_);
    }

    @Override
	@Shadow
    public abstract void setSprinting(boolean p_setSprinting_1_);

    @Shadow
    protected int sprintToggleTimer;
    @Shadow
    public float prevTimeInPortal;
    @Shadow
    public float timeInPortal;
    @Shadow
    protected Minecraft mc;
    @Shadow
    public MovementInput movementInput;

    @Override
	@Shadow
    protected abstract boolean pushOutOfBlocks(double p_pushOutOfBlocks_1_, double p_pushOutOfBlocks_3_,
            double p_pushOutOfBlocks_5_);

    @Override
	@Shadow
    public abstract void sendPlayerAbilities();

    @Shadow
    public abstract boolean isRidingHorse();

    @Shadow
    private int horseJumpPowerCounter;
    @Shadow
    private float horseJumpPower;

    @Shadow
    protected abstract void sendHorseJump();

    @Override
	@Shadow
    public abstract boolean isSneaking();

    /**
     * @author mc code
     * @reason i tried other ways of doing this, nothing else worked
     */
    @Override
	@Overwrite
    public void onLivingUpdate() {
        if (this.sprintingTicksLeft > 0) {
            --this.sprintingTicksLeft;
            if (this.sprintingTicksLeft == 0)
				this.setSprinting(false);
        }

        if (this.sprintToggleTimer > 0)
			--this.sprintToggleTimer;

        this.prevTimeInPortal = this.timeInPortal;
        if (this.inPortal) {
            if ((this.mc.currentScreen != null) && !this.mc.currentScreen.doesGuiPauseGame())
				this.mc.displayGuiScreen(null);

            if (this.timeInPortal == 0.0F)
                // 1.7.10: func_147674_a instead of create
				this.mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("portal.trigger"),
                        (this.rand.nextFloat() * 0.4F) + 0.8F));

            this.timeInPortal += 0.0125F;
            if (this.timeInPortal >= 1.0F)
				this.timeInPortal = 1.0F;

            this.inPortal = false;
        } else if (this.isPotionActive(Potion.confusion)
                && (this.getActivePotionEffect(Potion.confusion).getDuration() > 60)) {
            this.timeInPortal += 0.006666667F;
            if (this.timeInPortal > 1.0F)
				this.timeInPortal = 1.0F;
        } else {
            if (this.timeInPortal > 0.0F)
				this.timeInPortal -= 0.05F;

            if (this.timeInPortal < 0.0F)
				this.timeInPortal = 0.0F;
        }

        if (this.timeUntilPortal > 0)
			--this.timeUntilPortal;

        Module noSlow = Raven.moduleManager.getModuleByClazz(NoSlow.class);
        Module sprint = Raven.moduleManager.getModuleByClazz(Sprint.class);

        boolean flag = this.movementInput.jump;
        boolean flag1 = this.movementInput.sneak;
        float f = 0.8F;
        boolean flag2 = this.movementInput.moveForward >= f;
        this.movementInput.updatePlayerMoveState();
        if (this.isUsingItem() && !this.isRiding()) {

            MovementInput var10000 = this.movementInput;

            if (noSlow.isEnabled()) {
                float slowdown = (float) ((100 - NoSlow.b.getInput()) / 100F);
                var10000.moveStrafe *= slowdown;
                var10000.moveForward *= slowdown;
            } else {
                var10000.moveStrafe *= 0.2F;
                var10000.moveForward *= 0.2F;
                this.sprintToggleTimer = 0;
            }
        }

        // 1.7.10: this.boundingBox.minY instead of this.getEntityBoundingBox().minY
        this.pushOutOfBlocks(this.posX - ((double) this.width * 0.35D), this.boundingBox.minY + 0.5D,
                this.posZ + ((double) this.width * 0.35D));
        this.pushOutOfBlocks(this.posX - ((double) this.width * 0.35D), this.boundingBox.minY + 0.5D,
                this.posZ - ((double) this.width * 0.35D));
        this.pushOutOfBlocks(this.posX + ((double) this.width * 0.35D), this.boundingBox.minY + 0.5D,
                this.posZ - ((double) this.width * 0.35D));
        this.pushOutOfBlocks(this.posX + ((double) this.width * 0.35D), this.boundingBox.minY + 0.5D,
                this.posZ + ((double) this.width * 0.35D));
        boolean flag3 = ((float) this.getFoodStats().getFoodLevel() > 6.0F) || this.capabilities.allowFlying;
        if (this.onGround && !flag1 && !flag2
                && ((this.movementInput.moveForward >= f) || (sprint.isEnabled() && Sprint.multiDir.isToggled()
                        && ((movementInput.moveForward != 0) || (movementInput.moveStrafe != 0))))
                && !this.isSprinting() && flag3 && (!this.isUsingItem() || noSlow.isEnabled())
                && (!this.isPotionActive(Potion.blindness)
                        || (sprint.isEnabled() && Sprint.ignoreBlindness.isToggled())))
			if ((this.sprintToggleTimer <= 0) && !this.mc.gameSettings.keyBindSprint.isKeyDown())
				this.sprintToggleTimer = 7;
			else
				this.setSprinting(true);

        if (!this.isSprinting()
                && ((this.movementInput.moveForward >= f) || (sprint.isEnabled() && Sprint.multiDir.isToggled()
                        && ((movementInput.moveForward != 0) || (movementInput.moveStrafe != 0))))
                && flag3 && (!this.isUsingItem() || noSlow.isEnabled())
                && (!this.isPotionActive(Potion.blindness)
                        || (sprint.isEnabled() && Sprint.ignoreBlindness.isToggled()))
                && this.mc.gameSettings.keyBindSprint.isKeyDown())
			this.setSprinting(true);

        if (this.isSprinting() && (((sprint.isEnabled() && Sprint.multiDir.isToggled())
                ? !((movementInput.moveForward != 0) || (movementInput.moveStrafe != 0))
                : this.movementInput.moveForward < f) || this.isCollidedHorizontally || !flag3))
			this.setSprinting(false);

        if (this.capabilities.allowFlying) {
            // 1.7.10: no spectator mode - removed isSpectatorMode() check
            if (!flag && this.movementInput.jump)
                if (this.flyToggleTimer == 0)
                    this.flyToggleTimer = 7;
                else {
                    this.capabilities.isFlying = !this.capabilities.isFlying;
                    this.sendPlayerAbilities();
                    this.flyToggleTimer = 0;
                }
        }

        // 1.7.10: this == this.mc.renderViewEntity instead of this.isCurrentViewEntity()
        if (this.capabilities.isFlying && this == this.mc.renderViewEntity) {
            if (this.movementInput.sneak)
				this.motionY -= this.capabilities.getFlySpeed() * 3.0F;

            if (this.movementInput.jump)
				this.motionY += this.capabilities.getFlySpeed() * 3.0F;
        }

        if (this.isRidingHorse()) {
            if (this.horseJumpPowerCounter < 0) {
                ++this.horseJumpPowerCounter;
                if (this.horseJumpPowerCounter == 0)
					this.horseJumpPower = 0.0F;
            }

            if (flag && !this.movementInput.jump) {
                this.horseJumpPowerCounter = -10;
                this.sendHorseJump();
            } else if (!flag && this.movementInput.jump) {
                this.horseJumpPowerCounter = 0;
                this.horseJumpPower = 0.0F;
            } else if (flag) {
                ++this.horseJumpPowerCounter;
                if (this.horseJumpPowerCounter < 10)
					this.horseJumpPower = (float) this.horseJumpPowerCounter * 0.1F;
				else
					this.horseJumpPower = 0.8F + ((2.0F / (float) (this.horseJumpPowerCounter - 9)) * 0.1F);
            }
        } else
			this.horseJumpPower = 0.0F;

        super.onLivingUpdate();
        // 1.7.10: no spectator mode - removed isSpectatorMode() check
        if (this.onGround && this.capabilities.isFlying) {
            this.capabilities.isFlying = false;
            this.sendPlayerAbilities();
        }

    }
}
