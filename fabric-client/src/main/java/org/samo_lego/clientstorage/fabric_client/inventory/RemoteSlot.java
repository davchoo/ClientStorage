package org.samo_lego.clientstorage.fabric_client.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.samo_lego.clientstorage.fabric_client.casts.ICSPlayer;
import org.samo_lego.clientstorage.fabric_client.event.ContainerDiscovery;
import org.samo_lego.clientstorage.fabric_client.storage.InteractableContainer;


public class RemoteSlot extends Slot {
    public RemoteSlot(RemoteInventory inventory, int slot, int x, int y) {
        super(inventory, slot, x, y);
    }

    public void onTake(ClickType clickType) {
        // Make sure the player has an empty slot
        var player = Minecraft.getInstance().player;
        if (player.getInventory().getFreeSlot() == -1) {
            return;
        }

        ItemStack stack = RemoteInventory.getInstance().removeItemNoUpdate(getContainerSlot());
        if (stack.cs_getContainer() == null) {
            return;
        }

        // Send interaction packet to server
        InteractableContainer sourceContainer = stack.cs_getContainer();
        //BlockPos blockPos = sourceContainer.getBlockPos();

        // Remove item from client container
        InteractableContainer container;
        if (sourceContainer instanceof ChestBlockEntity chest) {
            var state = chest.getBlockState();
            container = (InteractableContainer) ChestBlock.getContainer((ChestBlock) state.getBlock(), state, chest.getLevel(), chest.getBlockPos(), true);
        } else {
            container = sourceContainer;
        }
        container.removeItemNoUpdate(stack.cs_getSlotId());

        // Close crafting
        player.connection.send(new ServerboundContainerClosePacket(player.containerMenu.containerId));

        // Helps us ignore GUI open packet later then
        ((ICSPlayer) player).cs_setAccessingItem(true);

        ContainerDiscovery.addPendingInteraction(sourceContainer, containerMenu -> {
            if (!containerMenu.getSlot(stack.cs_getSlotId()).getItem().is(stack.getItem())) {
                // TODO restart search
                return;
            }
            // Find an empty slot in the player's inventory
            int targetSlot = -1;
            for (int i = sourceContainer.getContainerSize(); i < containerMenu.slots.size(); i++) {
                if (containerMenu.getSlot(i).getItem().isEmpty()) {
                    targetSlot = i;
                    if (clickType != ClickType.QUICK_MOVE) {
                        ContainerDiscovery.craftingPickupSlotId = targetSlot - sourceContainer.getContainerSize() + /* CraftingMenu.INV_SLOT_START */ 10;
                    }
                    break;
                }
            }
            if (targetSlot != -1) {
                // Pickup the item
                Minecraft.getInstance().gameMode.handleInventoryMouseClick(containerMenu.containerId, stack.cs_getSlotId(), 0, ClickType.PICKUP, player);
                // Put the item in the player's inventory
                Minecraft.getInstance().gameMode.handleInventoryMouseClick(containerMenu.containerId, targetSlot, 0, ClickType.PICKUP, player);

                stack.cs_clearData();
            }
        });

        ContainerDiscovery.startSendPackets();
    }

    public void onPut(ItemStack stack) {
        stack.cs_transfer2Remote();
    }
}
