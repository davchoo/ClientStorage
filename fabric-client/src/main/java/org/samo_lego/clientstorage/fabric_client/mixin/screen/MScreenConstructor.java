package org.samo_lego.clientstorage.fabric_client.mixin.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.samo_lego.clientstorage.fabric_client.event.ContainerDiscovery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MenuScreens.ScreenConstructor.class)
public interface MScreenConstructor<T extends AbstractContainerMenu> {
    @Inject(method = "fromPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"), cancellable = true)
    default void mixinSetScreen(Component component, MenuType<T> menuType, Minecraft minecraft, int i, CallbackInfo ci) {
        if (menuType == MenuType.CRAFTING) {
            return;
        }
        if (ContainerDiscovery.fakePacketsActive()) {
            ci.cancel();
        }
    }
}
