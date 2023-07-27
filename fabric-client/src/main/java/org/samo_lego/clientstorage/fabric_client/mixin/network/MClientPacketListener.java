package org.samo_lego.clientstorage.fabric_client.mixin.network;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import org.samo_lego.clientstorage.fabric_client.event.ContainerDiscovery;
import org.samo_lego.clientstorage.fabric_client.network.PacketLimits;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.sounds.SoundSource.BLOCKS;


@Mixin(ClientPacketListener.class)
public class MClientPacketListener {

    @Inject(method = "handleHorseScreenOpen",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"),
            cancellable = true
    )
    private void mixinHorseScreenOpen(ClientboundHorseScreenOpenPacket clientboundHorseScreenOpenPacket, CallbackInfo ci) {
        if (ContainerDiscovery.fakePacketsActive()) {
            ci.cancel();
        }
    }

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
        if (packet.getSource().equals(BLOCKS) && ContainerDiscovery.fakePacketsActive()) {
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
