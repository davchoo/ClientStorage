package org.samo_lego.clientstorage.fabric_client.mixin.screen;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import org.samo_lego.clientstorage.fabric_client.event.ContainerDiscovery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AbstractContainerMenu.class)
public abstract class MAbstractContainerMenu {

    @Inject(method = "initializeContents", at = @At("TAIL"))
    private void mixinInitializeContents(int i, List<ItemStack> list, ItemStack itemStack, CallbackInfo ci) {
        ContainerDiscovery.onInventoryInitialize((AbstractContainerMenu) (Object) this);
    }
}
