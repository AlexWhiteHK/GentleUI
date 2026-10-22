package io.github.alexwhitehk.gentleui.fabric.render;

import io.github.alexwhitehk.gentleui.core.math.Affine;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.render.ImageRef;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

/** {@link Canvas} backend for the 1.21.6+ deferred GUI renderer. */
public final class McCanvas extends Canvas {
	private static final McCanvas SHARED = new McCanvas();

	private GuiGraphicsExtractor g;
	private final Minecraft mc = Minecraft.getInstance();
	private float pixel = 0.5f;
	private MeshElement mesh;
	private boolean ownScissor;
	private boolean blurred;

	// Mesh elements are referenced by the render state until the frame is drawn, so they are recycled with a
	// delay of two frames.
	private final List<List<MeshElement>> generations = new ArrayList<>();
	private final List<MeshElement> free = new ArrayList<>();
	private GuiGraphicsExtractor lastFrame;

	private McCanvas() {
		for (int i = 0; i < 3; i++) {
			generations.add(new ArrayList<>());
		}
	}

	/** Binds the shared canvas to the graphics of the frame being drawn. */
	public static McCanvas begin(GuiGraphicsExtractor graphics) {
		return SHARED.bind(graphics);
	}

	private McCanvas bind(GuiGraphicsExtractor graphics) {
		if (graphics != lastFrame) {
			lastFrame = graphics;
			List<MeshElement> oldest = generations.remove(0);
			free.addAll(oldest);
			oldest.clear();
			generations.add(oldest);
			blurred = false;
		}
		this.g = graphics;
		this.pixel = 1f / Math.max(1, mc.getWindow().getGuiScale());
		reset();
		return this;
	}

	/** Submits pending geometry and releases the clip. Must be called before vanilla code draws again. */
	public void end() {
		flush();
		if (ownScissor) {
			g.disableScissor();
			ownScissor = false;
		}
		reset();
	}

	public GuiGraphicsExtractor graphics() {
		return g;
	}

	@Override
	protected void emitQuad(float x0, float y0, int c0, float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3) {
		if (mesh == null) {
			mesh = free.isEmpty() ? new MeshElement() : free.remove(free.size() - 1);
			mesh.reset();
		}
		mesh.vertex(x0, y0, c0);
		mesh.vertex(x1, y1, c1);
		mesh.vertex(x2, y2, c2);
		mesh.vertex(x3, y3, c3);
	}

	@Override
	public void flush() {
		MeshElement m = mesh;
		mesh = null;
		if (m == null) {
			return;
		}
		if (!m.isEmpty() && m.seal(g.pose(), g.scissorStack.peek())) {
			g.guiRenderState.addGuiElement(m);
		}
		generations.get(generations.size() - 1).add(m);
	}

	@Override
	public float pixel() {
		return pixel;
	}

	@Override
	public float width() {
		return g.guiWidth();
	}

	@Override
	public float height() {
		return g.guiHeight();
	}

	@Override
	public float textWidth(String text) {
		return mc.font.width(text);
	}

	@Override
	public float fontHeight() {
		return 8f;
	}

	private Matrix3x2f toJoml(Affine a, boolean snap) {
		float tx = a.m02;
		float ty = a.m12;
		if (snap && a.m01 == 0f && a.m10 == 0f) {
			tx = Math.round(tx / pixel) * pixel;
			ty = Math.round(ty / pixel) * pixel;
		}
		return new Matrix3x2f(a.m00, a.m10, a.m01, a.m11, tx, ty);
	}

	@Override
	protected void emitText(String text, Affine transform, int argb, boolean shadow) {
		Matrix3x2fStack pose = g.pose();
		pose.pushMatrix();
		pose.mul(toJoml(transform, true));
		g.text(mc.font, text, 0, 0, argb, shadow);
		pose.popMatrix();
	}

	@Override
	protected void emitImage(ImageRef image, Affine transform, float w, float h, float u0, float v0, float u1, float v1, int tint) {
		if (!(image.handle instanceof Images.Handle handle)) {
			return;
		}
		TextureSetup setup = handle.setup();
		if (setup == null) {
			return;
		}
		Matrix3x2f pose = new Matrix3x2f(g.pose()).mul(toJoml(transform, false));
		var scissor = g.scissorStack.peek();
		var bounds = ImageElement.boundsOf(0f, 0f, w, h, pose, scissor);
		if (bounds == null) {
			return;
		}
		g.guiRenderState.addGuiElement(new ImageElement(setup, pose, 0f, 0f, w, h, u0, v0, u1, v1, tint, scissor, bounds));
	}

	@Override
	protected void applyClip(Rect clip) {
		if (ownScissor) {
			g.disableScissor();
			ownScissor = false;
		}
		if (clip != null) {
			int x0 = (int) Math.floor(clip.x);
			int y0 = (int) Math.floor(clip.y);
			int x1 = (int) Math.ceil(clip.right());
			int y1 = (int) Math.ceil(clip.bottom());
			g.enableScissor(x0, y0, Math.max(x0, x1), Math.max(y0, y1));
			ownScissor = true;
		}
	}

	@Override
	public boolean backdropBlur() {
		if (blurred) {
			return false;
		}
		flush();
		try {
			g.blurBeforeThisStratum();
			g.nextStratum();
			blurred = true;
			return true;
		} catch (IllegalStateException alreadyBlurred) {
			blurred = true;
			return false;
		}
	}
}
