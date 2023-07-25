package org.samo_lego.clientstorage.fabric_client.mixin.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.samo_lego.clientstorage.fabric_client.storage.InteractableContainerBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BaseContainerBlockEntity.class)
public abstract class MBaseContainerBlockEntity implements InteractableContainerBlock {

    @Shadow
    public abstract Component getDisplayName();

    @Override
    public BlockPos cs_blockPos() {
        return ((BlockEntity) (Object) this).getBlockPos();
    }

    @Override
    public Vec3 cs_position() {
        return Vec3.atCenterOf(cs_blockPos());
    }

    @Override
    public Component cs_getName() {
        return this.getDisplayName();
    }
}
