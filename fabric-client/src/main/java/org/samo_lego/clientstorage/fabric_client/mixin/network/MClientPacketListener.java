package org.samo_lego.clientstorage.fabric_client.mixin.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.samo_lego.clientstorage.fabric_client.casts.ICSPlayer;
import org.samo_lego.clientstorage.fabric_client.event.ContainerDiscovery;
import org.samo_lego.clientstorage.fabric_client.network.PacketLimits;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.sounds.SoundSource.BLOCKS;


@Mixin(ClientPacketListener.class)
public class MClientPacketListener {

    @Shadow
    @Final
    private Minecraft minecraft;

    /**
     * Disables incoming block sounds (e.g. barrel opening)
     * if it was generated due to mod's packets.
     *
     * @param packet
     * @param ci
     */
    @Inject(method = "handleSoundEvent",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V",
                    shift = At.Shift.AFTER),
            cancellable = true)
    private void onSoundEvent(ClientboundSoundPacket packet, CallbackInfo ci) {
        // Cancel sounds if item search is active
        if (packet.getSource().equals(BLOCKS) && (((ICSPlayer) this.minecraft.player).cs_isAccessingItem() || ContainerDiscovery.fakePacketsActive())) {
            ci.cancel();
        }
    }


    /**
     * Tries to recognize server type from server brand packet.
     *
     * @param packet
     * @param ci
     */
    @Inject(method = "handleCustomPayload",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;setServerBrand(Ljava/lang/String;)V",
                    shift = At.Shift.AFTER))
    private void onServerBrand(ClientboundCustomPayloadPacket packet, CallbackInfo ci) {
        PacketLimits.tryRecognizeServer();
    }
}
