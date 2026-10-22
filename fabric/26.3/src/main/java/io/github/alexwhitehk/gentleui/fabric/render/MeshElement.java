package io.github.alexwhitehk.gentleui.fabric.render;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

/**
 * A batch of anti-aliased vector quads submitted to the GUI render state as one element. It uses the vanilla
 * position-color GUI pipeline, so no custom shaders are involved.
 */
final class MeshElement implements GuiElementRenderState {
	float[] xy = new float[256];
	int[] colors = new int[128];
	int vertices;
	final Matrix3x2f pose = new Matrix3x2f();
	@Nullable ScreenRectangle scissor;
	@Nullable ScreenRectangle bounds;
	private float minX;
	private float minY;
	private float maxX;
	private float maxY;

	void reset() {
		vertices = 0;
		minX = Float.MAX_VALUE;
		minY = Float.MAX_VALUE;
		maxX = -Float.MAX_VALUE;
		maxY = -Float.MAX_VALUE;
		scissor = null;
		bounds = null;
	}

	boolean isEmpty() {
		return vertices == 0;
	}

	void vertex(float x, float y, int color) {
		if (vertices == colors.length) {
			colors = Arrays.copyOf(colors, vertices * 2);
			xy = Arrays.copyOf(xy, vertices * 4);
		}
		xy[vertices * 2] = x;
		xy[vertices * 2 + 1] = y;
		colors[vertices] = color;
		vertices++;
		if (x < minX) {
			minX = x;
		}
		if (x > maxX) {
			maxX = x;
		}
		if (y < minY) {
			minY = y;
		}
		if (y > maxY) {
			maxY = y;
		}
	}

	/** Finalizes bounds; returns false when the batch is fully clipped away. */
	boolean seal(Matrix3x2f currentPose, @Nullable ScreenRectangle scissorArea) {
		pose.set(currentPose);
		scissor = scissorArea;
		int x0 = (int) Math.floor(minX);
		int y0 = (int) Math.floor(minY);
		int x1 = (int) Math.ceil(maxX);
		int y1 = (int) Math.ceil(maxY);
		ScreenRectangle rect = new ScreenRectangle(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0)).transformMaxBounds(pose);
		bounds = scissorArea != null ? scissorArea.intersection(rect) : rect;
		return bounds != null;
	}

	@Override
	public void buildVertices(VertexConsumer consumer) {
		for (int i = 0; i < vertices; i++) {
			consumer.addVertexWith2DPose(pose, xy[i * 2], xy[i * 2 + 1]).setColor(colors[i]);
		}
	}

	@Override
	public RenderPipeline pipeline() {
		return RenderPipelines.GUI;
	}

	@Override
	public TextureSetup textureSetup() {
		return TextureSetup.noTexture();
	}

	@Override
	public @Nullable ScreenRectangle scissorArea() {
		return scissor;
	}

	@Override
	public @Nullable ScreenRectangle bounds() {
		return bounds;
	}
}
