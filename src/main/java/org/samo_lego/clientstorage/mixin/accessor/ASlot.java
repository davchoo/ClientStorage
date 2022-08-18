package org.samo_lego.clientstorage.mixin.accessor;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Slot.class)
public interface ASlot {
    @Mutable
    @Accessor("x")
    void setX(int x);

    @Mutable
    @Accessor("y")
    void setY(int y);
}
