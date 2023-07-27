package org.samo_lego.clientstorage.fabric_client.storage;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import org.samo_lego.clientstorage.fabric_client.mixin.accessor.AMultiPlayerGamemode;
import org.samo_lego.clientstorage.fabric_client.network.PacketLimiter;
import org.samo_lego.clientstorage.fabric_client.render.ESPRender;
import org.samo_lego.clientstorage.fabric_client.util.PlayerLookUtil;

/**
 * Implementation of {@link InteractableContainer}, used for containers that are blocks.
 */
public interface InteractableContainerBlock extends InteractableContainer {

    BlockPos cs_blockPos();

    @Override
    default void cs_sendInteractionPacket() {
        PacketLimiter.needDelay();
        InteractableContainer.super.cs_sendInteractionPacket();
        var gm = (AMultiPlayerGamemode) Minecraft.getInstance().gameMode;
        var hitResult = PlayerLookUtil.raycastTo(this.cs_position());
        if (!hitResult.getBlockPos().equals(cs_blockPos())) {
            // Force hit result to have the correct block position
            hitResult = new BlockHitResult(this.cs_position(), Direction.NORTH, cs_blockPos(), true);
        }

        BlockHitResult finalHitResult = hitResult;
        gm.cs_startPrediction(Minecraft.getInstance().level, i ->
                new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, finalHitResult, i));

    }

    @Override
    default void cs_markGlowing() {
        ESPRender.markBlock(cs_blockPos());
    }

    @Override
    default String cs_info() {
        final String position = String.format("(%d, %d, %d)",
                this.cs_blockPos().getX(),
                this.cs_blockPos().getY(),
                this.cs_blockPos().getZ());
        return String.format("%s @ %s [%d slots]", this.cs_getName().getString(), position, this.getContainerSize());
    }
}
