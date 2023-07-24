package org.samo_lego.clientstorage.fabric_client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30;

/**
 * This was kindly provided by AutoHud mod author.
 * Visit their mod at <a href="https://github.com/Crendgrim/AutoHUD">GitHub</a>.
 * This class is used to render half-transparent items.
 *
 * @see <a href="https://github.com/Crendgrim/AutoHUD/blob/632f15845eef56979fcd6b8187a7779efaa80a18/src/main/java/mod/crend/autohud/component/Hud.java">Hud.java</a>
 */
public class TransparencyBuffer {
    private static final RenderTarget framebuffer;
    private static int previousFramebuffer;

    static {
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

    public static void preInject() {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.5f);
    }

    public static void drawExtraFramebuffer(PoseStack poseStack) {
        // Restore the original framebuffer
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);

        // Render the custom framebuffer's contents with transparency into the main buffer
        RenderSystem.setShaderTexture(0, framebuffer.getColorTextureId());
        Window window = Minecraft.getInstance().getWindow();
        int k = window.getGuiScaledWidth();
        int l = window.getGuiScaledHeight();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = poseStack.last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix4f, (float) 0, (float)k, (float) 0).uv((float) 0, (float) 1).endVertex();
        bufferBuilder.vertex(matrix4f, (float) 0, (float)l, (float) 0).uv((float) 0, (float) 0).endVertex();
        bufferBuilder.vertex(matrix4f, (float) 0, (float)l, (float) 0).uv((float) 1, (float) 0).endVertex();
        bufferBuilder.vertex(matrix4f, (float) 0, (float)k, (float) 0).uv((float) 1, (float) 1).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    public static void postInject() {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void resizeDisplay() {
        Window window = Minecraft.getInstance().getWindow();
        framebuffer.resize(window.getWidth(), window.getHeight(), Minecraft.ON_OSX);
    }
}
