package org.samo_lego.clientstorage.fabric_client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import org.lwjgl.opengl.GL30;

/**
 * This was kindly provided by AutoHud mod author.
 * Visit their mod at <a href="https://github.com/Crendgrim/AutoHUD">GitHub</a>.
 * This class is used to render half-transparent items.
 *
 * @see <a href="https://github.com/Crendgrim/AutoHUD/blob/632f15845eef56979fcd6b8187a7779efaa80a18/src/main/java/mod/crend/autohud/component/Hud.java">Hud.java</a>
 */
public class TransparencyHelper {
    private static final RenderTarget framebuffer;
    private static int previousFramebuffer;

    static {
        // todo onResize
        Window window = Minecraft.getInstance().getWindow();
        framebuffer = new TextureTarget(window.getWidth(), window.getHeight(), true, Minecraft.ON_OSX);
        framebuffer.setClearColor(0, 0, 0, 0);
    }

    public static void prepareExtraFramebuffer() {
        // Setup extra framebuffer to draw into
        previousFramebuffer = GlStateManager.getBoundFramebuffer();
        framebuffer.clear(Minecraft.ON_OSX);
        framebuffer.bindWrite(false);
    }

    public static void preInject(PoseStack poseStack) {
        //if (AutoHud.config.animationType() == AnimationType.Fade) {
        //    alpha = component.getAlpha();
        //    RenderSystem.enableBlend();
        //    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.5f);
        //} else {
        poseStack.pushPose();
        //}
    }

    public static void drawExtraFramebuffer(PoseStack matrices) {
        // Restore the original framebuffer
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);

        // Render the custom framebuffer's contents with transparency into the main buffer
        RenderSystem.setShaderTexture(0, framebuffer.getColorTextureId());
        Window window = Minecraft.getInstance().getWindow();
        GuiComponent.blit(
                matrices,
                0,
                0,
                window.getGuiScaledWidth(),
                window.getGuiScaledHeight(),
                0,
                framebuffer.height,
                framebuffer.width,
                -framebuffer.height,
                framebuffer.width,
                framebuffer.height
        );
    }

    public static void postInject(PoseStack poseStack) {
        //if (AutoHud.config.animationType() == AnimationType.Fade) {
        //    alpha = 1.0f;
        //    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        //} else {
        poseStack.popPose();
        //}
    }
}
