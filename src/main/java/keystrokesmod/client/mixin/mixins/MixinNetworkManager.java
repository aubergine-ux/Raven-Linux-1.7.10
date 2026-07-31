package keystrokesmod.client.mixin.mixins;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GenericFutureListener;
import keystrokesmod.client.event.EventDirection;
import keystrokesmod.client.event.impl.PacketEvent;
import keystrokesmod.client.main.Raven;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(priority = 995, value = NetworkManager.class)
public class MixinNetworkManager {

    // 1.7.10: NetworkManager.scheduleOutboundPacket(Packet, GenericFutureListener...)
    // replaces 1.8.9: NetworkManager.sendPacket(Packet)
    @Inject(method = "scheduleOutboundPacket", at = @At("HEAD"), cancellable = true)
    public void sendPacket(Packet p_sendPacket_1_, GenericFutureListener[] p_sendPacket_2_, CallbackInfo ci) {
        PacketEvent e = new PacketEvent(p_sendPacket_1_, EventDirection.OUTGOING);

        Raven.eventBus.post(e);

        p_sendPacket_1_ = e.getPacket();
        if (e.isCancelled())
            ci.cancel();
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    public void receivePacket(ChannelHandlerContext p_channelRead0_1_, Packet p_channelRead0_2_, CallbackInfo ci) {
        PacketEvent e = new PacketEvent(p_channelRead0_2_, EventDirection.INCOMING);

        Raven.eventBus.post(e);

        p_channelRead0_2_ = e.getPacket();
        if (e.isCancelled())
            ci.cancel();
    }

}
