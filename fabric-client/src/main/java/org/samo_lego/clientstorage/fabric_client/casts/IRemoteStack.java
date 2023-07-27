package org.samo_lego.clientstorage.fabric_client.casts;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.samo_lego.clientstorage.fabric_client.event.ContainerDiscovery;
import org.samo_lego.clientstorage.fabric_client.inventory.RemoteInventory;
import org.samo_lego.clientstorage.fabric_client.storage.InteractableContainer;

import java.util.Map;

import static org.samo_lego.clientstorage.fabric_client.util.StorageCache.FREE_SPACE_CONTAINERS;

public interface IRemoteStack {

    /**
     * 3x3 crafting slots + 1 output slot.
     */
    int CRAFTING_SLOT_OFFSET = 10;

    int cs_getSlotId();

    void cs_setSlot(InteractableContainer container, int slotId);

    InteractableContainer cs_getContainer();

    /**
     * Assigns remote container data to provided item stack.
     *
     * @param stack     stack to assign data to.
     * @param container origin of the stack.
     * @param slot      slot where stack is located in origin container.
     * @return item stack with added info.
     */
    static ItemStack fromStack(ItemStack stack, InteractableContainer container, int slot) {
        // Add properties to ItemStack via IRemoteStack interface
        stack.cs_setSlot(container, slot);
        return stack;
    }

    default void cs_clearData() {
        this.cs_setSlot(null, -1);
    }

    /**
     * Transfers this stack to any of
     * containers with free space left.
     *
     * @see #cs_transfer2Remote(boolean, int)
     */
    default void cs_transfer2Remote() {
        var player = Minecraft.getInstance().player;
        // Get first free slot in player's inventory (to move item to)
        int freeSlot = -1;
        NonNullList<Slot> slots = player.containerMenu.slots;
        for (int i = CRAFTING_SLOT_OFFSET; i < slots.size(); ++i) {
            var slot = slots.get(i);

            if (!slot.hasItem()) {
                freeSlot = i;
                break;
            }
        }

        if (freeSlot == -1) {
            return;
        }

        this.cs_transfer2Remote(true, freeSlot);
    }

    /**
     * Transfers this stack to any of containers
     * that have free space left.
     *
     * @param carried  whether this item stack is currently carried.
     * @param freeSlot any free slot index in player inventory to temporarily put item to.
     * @see org.samo_lego.clientstorage.fabric_client.util.StorageCache#FREE_SPACE_CONTAINERS
     */
    default void cs_transfer2Remote(boolean carried, int freeSlot) {
        var mc = Minecraft.getInstance();
        var player = mc.player;

        final InteractableContainer container = FREE_SPACE_CONTAINERS.entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .findAny()
                .orElse(null);
        if (container == null) {
            player.sendSystemMessage(Component.literal("No free space containers found.").withStyle(ChatFormatting.RED));
            return;
        }

        if (carried) {
            mc.gameMode.handleInventoryMouseClick(player.containerMenu.containerId, freeSlot, 0, ClickType.PICKUP, player);
        }

        // Free slot in player's inv now has different index due to new container being open
        int sourceSlot = freeSlot - CRAFTING_SLOT_OFFSET + container.getContainerSize();
        ContainerDiscovery.addPendingInteraction(container, containerMenu -> {
            // Pick up the item from the player's inventory
            mc.gameMode.handleInventoryMouseClick(containerMenu.containerId, sourceSlot, 0, ClickType.PICKUP, player);
            // TODO allow partial stacking
            // Place the item in the free slot in the container and count the amount of empty slots left
            boolean inserted = false;
            int spaceLeft = 0;
            for (int containerSlot = container.cs_startSlot(); containerSlot < container.getContainerSize(); ++containerSlot) {
                if (containerMenu.getSlot(containerSlot).getItem().isEmpty()) {
                    if (!inserted) {
                        inserted = true;
                        mc.gameMode.handleInventoryMouseClick(containerMenu.containerId, containerSlot, 0, ClickType.PICKUP, player);
                        ItemStack stack = containerMenu.getSlot(containerSlot).getItem();
                        stack.cs_setSlot(container, containerSlot);

                        // Add to remote
                        final var copiedStack = stack.copy();
                        RemoteInventory.getInstance().addStack(copiedStack);
                        container.setItem(containerSlot, copiedStack);
                    } else {
                        spaceLeft++;
                    }
                }
            }
            // Check left space
            if (spaceLeft <= 0) {
                FREE_SPACE_CONTAINERS.remove(container);
            } else {
                FREE_SPACE_CONTAINERS.put(container, spaceLeft);
            }
            // TODO find a different container if it was full
        });
        ContainerDiscovery.startSendPackets();
    }
}
