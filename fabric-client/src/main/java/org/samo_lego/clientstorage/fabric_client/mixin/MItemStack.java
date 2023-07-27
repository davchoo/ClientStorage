package org.samo_lego.clientstorage.fabric_client.mixin;

import net.minecraft.world.item.ItemStack;
import org.samo_lego.clientstorage.fabric_client.casts.IRemoteStack;
import org.samo_lego.clientstorage.fabric_client.storage.InteractableContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ItemStack.class)
public abstract class MItemStack implements IRemoteStack {
    @Unique
    private int slotId = -1;
    @Unique
    private InteractableContainer parentContainer;

    @Override
    public int cs_getSlotId() {
        return slotId;
    }

    @Override
    public void cs_setSlot(InteractableContainer container, int slotId) {
        this.parentContainer = container;
        this.slotId = slotId;
    }

    @Override
    public InteractableContainer cs_getContainer() {
        return this.parentContainer;
    }

    @Inject(method = "copy", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onCopy(CallbackInfoReturnable<ItemStack> cir, ItemStack newStack) {
        newStack.cs_setSlot(this.parentContainer, this.slotId);
    }
}
