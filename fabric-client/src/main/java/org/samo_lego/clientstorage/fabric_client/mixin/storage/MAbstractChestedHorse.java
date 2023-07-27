package org.samo_lego.clientstorage.fabric_client.mixin.storage;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.samo_lego.clientstorage.fabric_client.storage.InteractableContainerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractChestedHorse.class)
public abstract class MAbstractChestedHorse extends AbstractHorse implements InteractableContainerEntity {

    @Shadow protected abstract int getInventorySize();

    protected MAbstractChestedHorse(EntityType<? extends AbstractHorse> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public int cs_startSlot() {
        return 2;
    }

    @Override
    public int getContainerSize() {
        return this.getInventorySize();
    }

    @Override
    public boolean isEmpty() {
        return this.inventory.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (!this.level().isClientSide()) throw new IllegalStateException("Expected clientside");
        resizeInventory();
        return this.inventory.getItem(slot);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!this.level().isClientSide()) throw new IllegalStateException("Expected clientside");
        resizeInventory();
        return this.inventory.removeItemNoUpdate(slot);
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        if (!this.level().isClientSide()) return;
        resizeInventory();
        this.inventory.setItem(slot, itemStack);
    }

    @Override
    public void setChanged() {
        this.inventory.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return this.level().isClientSide();
    }

    private void resizeInventory() {
        if (this.inventory.getContainerSize() != getInventorySize()) {
            this.createInventory();
        }
    }

    @Override
    public boolean canPlaceItem(int i, ItemStack itemStack) {
        return this.inventory.canPlaceItem(i, itemStack);
    }
}
