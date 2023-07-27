package org.samo_lego.clientstorage.fabric_client.storage;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.samo_lego.clientstorage.fabric_client.ClientStorageFabric;
import org.samo_lego.clientstorage.fabric_client.casts.ICSPlayer;
import org.samo_lego.clientstorage.fabric_client.event.ContainerDiscovery;
import org.samo_lego.clientstorage.fabric_client.util.StorageCache;

import java.util.function.Predicate;

public interface InteractableContainer extends Container {
    Predicate<? super Entity> CONTAINER_ENTITY_SELECTOR = (entity) -> entity instanceof InteractableContainer;

    default void cs_sendInteractionPacket() {
        // Save interacted container
        ((ICSPlayer) Minecraft.getInstance().player).cs_setLastInteractedContainer(this);
    }

    boolean cs_isDelayed();

    default void cs_storeContents(AbstractContainerMenu containerMenu) {
        final NonNullList<ItemStack> items = containerMenu.getItems();

        int emptySlots = 0;
        if (items.size() - 36 != getContainerSize()) {
            ClientStorageFabric.tryLog("Mismatch inventory size. Got: " + (items.size() - 36) + ", world container: " + getContainerSize(), ChatFormatting.RED);
            return;
        }

        boolean hasItems = false;
        for (int i = 0; i < getContainerSize(); ++i) {
            ItemStack stack = items.get(i);
            setItem(i, stack);
            if (stack.isEmpty()) {
                ++emptySlots;
            } else if (ContainerDiscovery.fakePacketsActive()) {
                // Also add to remote inventory
                ContainerDiscovery.addRemoteItem(this, i, stack);
                hasItems = true;
            }
        }

        if (emptySlots == 0) {
            StorageCache.FREE_SPACE_CONTAINERS.remove(this);
        } else {
            StorageCache.FREE_SPACE_CONTAINERS.put(this, emptySlots);
        }
        if (hasItems) {
            StorageCache.CACHED_INVENTORIES.add(this);
        }
    }

    /**
     * Mark this container to be glowing.
     */
    void cs_markGlowing();

    /**
     * Returns the position of this container.
     *
     * @return position of this container.
     */

    Vec3 cs_position();

    /**
     * Whether this container is an entity.
     *
     * @return true if this container is an entity, false otherwise.
     */
    default boolean cs_isEntity() {
        return this instanceof Entity;
    }

    /**
     * Gets this container's name.
     *
     * @return name of this container.
     */
    Component cs_getName();

    /**
     * Gets this container's information (name and position) as string.
     *
     * @return information of this container.
     */
    default String cs_info() {
        // Get position
        final String position = String.format("(%.2f, %.2f, %.2f)",
                this.cs_position().x(),
                this.cs_position().y(),
                this.cs_position().z());
        return String.format("%s @ %s [%d slots]", this.cs_getName().getString(), position, this.getContainerSize());
    }
}
