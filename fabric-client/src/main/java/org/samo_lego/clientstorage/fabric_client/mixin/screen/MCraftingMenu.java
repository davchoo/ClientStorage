package org.samo_lego.clientstorage.fabric_client.mixin.screen;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import org.samo_lego.clientstorage.fabric_client.event.ContainerDiscovery;
import org.samo_lego.clientstorage.fabric_client.inventory.RemoteInventory;
import org.samo_lego.clientstorage.fabric_client.inventory.RemoteSlot;
import org.samo_lego.clientstorage.fabric_client.mixin.accessor.ASlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.samo_lego.clientstorage.fabric_client.ClientStorageFabric.config;

@Mixin(CraftingMenu.class)
public abstract class MCraftingMenu extends RecipeBookMenu<CraftingContainer> {

    public MCraftingMenu(MenuType<?> menuType, int i) {
        super(menuType, i);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("RETURN"))
    private void constructor(int syncId, Inventory playerInventory, ContainerLevelAccess context, CallbackInfo ci) {
        if (!config.enabled) return;

        // Moving slots down
        this.slots.forEach(slot -> ((ASlot) slot).setY(slot.y + 36));

        // Adding "remote slots"
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new RemoteSlot(RemoteInventory.getInstance(), col + row * 9, col * 18 + 9, row * 18 - 23));
            }
        }

        ContainerDiscovery.onCraftingScreenOpen();
    }
}
