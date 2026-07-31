package keystrokesmod.client.mixin.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.mojang.authlib.GameProfile;

import keystrokesmod.client.event.EventTiming;
import keystrokesmod.client.event.impl.TickEvent;
import keystrokesmod.client.event.impl.UpdateEvent;
import keystrokesmod.client.main.Raven;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.world.World;

/**
 * In 1.7.10, EntityClientPlayerMP (not EntityPlayerSP) handles position packet sending.
 * The position update logic that was in EntityPlayerSP.onUpdateWalkingPlayer() in 1.8.9
 * is inline in EntityClientPlayerMP.onLivingUpdate() in 1.7.10.
 * This mixin handles the sprint/sneak state syncing and position update sending
 * with UpdateEvent and TickEvent hooks.
 */
@Mixin(priority = 994, value = EntityClientPlayerMP.class)
public abstract class MixinEntityClientPlayerMP extends AbstractClientPlayer {

    public MixinEntityClientPlayerMP(World world, GameProfile profile) {
        super(world, profile);
    }

    @Shadow
    @Final
    public NetHandlerPlayClient sendQueue;

    @Shadow
    private boolean serverSprintState;
    @Shadow
    private boolean serverSneakState;
    @Shadow
    private double lastReportedPosX;
    @Shadow
    private double lastReportedPosY;
    @Shadow
    private double lastReportedPosZ;
    @Shadow
    private float lastReportedYaw;
    @Shadow
    private float lastReportedPitch;
    @Shadow
    private int positionUpdateTicks;

    @Shadow
    protected Minecraft mc;

    @Override
    @Shadow
    public abstract boolean isSneaking();

    /**
     * @author mc code
     * @reason position update hooks (UpdateEvent, TickEvent) for 1.7.10
     *
     * In 1.7.10, EntityClientPlayerMP.onLivingUpdate() calls super.onLivingUpdate()
     * (which triggers EntityPlayerSP.onLivingUpdate - our custom sprint version)
     * and then handles sprint/sneak syncing + position packet sending.
     */
    @Override
    @Overwrite
    public void onLivingUpdate() {
        // Call parent (EntityPlayerSP.onLivingUpdate - overwritten by MixinEntityPlayerSP)
        super.onLivingUpdate();

        Raven.eventBus.post(new TickEvent());

        boolean flag = this.isSprinting();
        if (flag != this.serverSprintState) {
            if (flag)
                this.sendQueue
                        .addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.START_SPRINTING));
            else
                this.sendQueue
                        .addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.STOP_SPRINTING));

            this.serverSprintState = flag;
        }

        boolean flag1 = this.isSneaking();
        if (flag1 != this.serverSneakState) {
            if (flag1)
                this.sendQueue
                        .addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.START_SNEAKING));
            else
                this.sendQueue
                        .addToSendQueue(new C0BPacketEntityAction(this, C0BPacketEntityAction.Action.STOP_SNEAKING));

            this.serverSneakState = flag1;
        }

        // 1.7.10: this == this.mc.renderViewEntity instead of this.isCurrentViewEntity()
        if (this == this.mc.renderViewEntity) {

            // 1.7.10: this.boundingBox.minY instead of this.getEntityBoundingBox().minY
            UpdateEvent e = new UpdateEvent(EventTiming.PRE, this.posX, this.boundingBox.minY, this.posZ,
                    this.rotationYaw, this.rotationPitch, this.onGround);
            Raven.eventBus.post(e);

            double d0 = e.getX() - this.lastReportedPosX;
            double d1 = e.getY() - this.lastReportedPosY;
            double d2 = e.getZ() - this.lastReportedPosZ;
            double d3 = e.getYaw() - this.lastReportedYaw;
            double d4 = e.getPitch() - this.lastReportedPitch;
            boolean flag2 = (((d0 * d0) + (d1 * d1) + (d2 * d2)) > 9.0E-4D) || (this.positionUpdateTicks >= 20);
            boolean flag3 = (d3 != 0.0D) || (d4 != 0.0D);
            if (this.ridingEntity == null) {
                if (flag2 && flag3)
                    this.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(e.getX(), e.getY(),
                            e.getZ(), e.getYaw(), e.getPitch(), e.isOnGround()));
                else if (flag2)
                    this.sendQueue.addToSendQueue(
                            new C03PacketPlayer.C04PacketPlayerPosition(e.getX(), e.getY(), e.getZ(), e.isOnGround()));
                else if (flag3)
                    this.sendQueue.addToSendQueue(
                            new C03PacketPlayer.C05PacketPlayerLook(e.getYaw(), e.getPitch(), e.isOnGround()));
                else
                    this.sendQueue.addToSendQueue(new C03PacketPlayer(e.isOnGround()));
            } else {
                this.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(this.motionX, -999.0D,
                        this.motionZ, e.getYaw(), e.getPitch(), e.isOnGround()));
                flag2 = false;
            }

            ++this.positionUpdateTicks;
            if (flag2) {
                this.lastReportedPosX = e.getX();
                this.lastReportedPosY = e.getY();
                this.lastReportedPosZ = e.getZ();
                this.positionUpdateTicks = 0;
            }

            if (flag3) {
                this.lastReportedYaw = e.getYaw();
                this.lastReportedPitch = e.getPitch();
            }

            e = UpdateEvent.convertPost(e);
            Raven.eventBus.post(e);

        }

    }
}
