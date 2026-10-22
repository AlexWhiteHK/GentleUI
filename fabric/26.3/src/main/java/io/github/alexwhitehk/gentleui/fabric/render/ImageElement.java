package io.github.alexwhitehk.gentleui.fabric.render;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/** Textured quad with float coordinates and UVs, so images can move and scale smoothly. */
record ImageElement(
	TextureSetup textureSetup,
	Matrix3x2f pose,
	float x0,
	float y0,
	float x1,
	float y1,
	float u0,
	float v0,
	float u1,
	float v1,
	int color,
	@Nullable ScreenRectangle scissorArea,
	@Nullable ScreenRectangle bounds
) implements GuiElementRenderState {
	static @Nullable ScreenRectangle boundsOf(float x0, float y0, float x1, float y1, Matrix3x2f pose, @Nullable ScreenRectangle scissor) {
		int ix = (int) Math.floor(x0);
		int iy = (int) Math.floor(y0);
		ScreenRectangle r = new ScreenRectangle(ix, iy, Math.max(1, (int) Math.ceil(x1) - ix), Math.max(1, (int) Math.ceil(y1) - iy)).transformMaxBounds(pose);
		return scissor != null ? scissor.intersection(r) : r;
	}

	@Override
	public void buildVertices(VertexConsumer consumer) {
		consumer.addVertexWith2DPose(pose, x0, y0).setUv(u0, v0).setColor(color);
		consumer.addVertexWith2DPose(pose, x0, y1).setUv(u0, v1).setColor(color);
		consumer.addVertexWith2DPose(pose, x1, y1).setUv(u1, v1).setColor(color);
		consumer.addVertexWith2DPose(pose, x1, y0).setUv(u1, v0).setColor(color);
	}

	@Override
	public RenderPipeline pipeline() {
		return RenderPipelines.GUI_TEXTURED;
	}
}
