package io.github.alexwhitehk.gentleui.core;

import io.github.alexwhitehk.gentleui.core.math.Affine;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.render.ImageRef;
import java.util.ArrayList;
import java.util.List;

/** Canvas that records emitted geometry for assertions. Text is 6 units per character. */
final class RecordingCanvas extends Canvas {
	final List<float[]> quads = new ArrayList<>();
	final List<int[]> colors = new ArrayList<>();
	final List<String> texts = new ArrayList<>();
	Rect clip;
	float pixel = 0.5f;

	@Override
	protected void emitQuad(float x0, float y0, int c0, float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3) {
		quads.add(new float[] {x0, y0, x1, y1, x2, y2, x3, y3});
		colors.add(new int[] {c0, c1, c2, c3});
	}

	@Override
	public float pixel() {
		return pixel;
	}

	@Override
	public float width() {
		return 400f;
	}

	@Override
	public float height() {
		return 240f;
	}

	@Override
	public float textWidth(String text) {
		return text.length() * 6f;
	}

	@Override
	public float fontHeight() {
		return 8f;
	}

	@Override
	protected void emitText(String text, Affine transform, int argb, boolean shadow) {
		texts.add(text);
	}

	@Override
	protected void emitImage(ImageRef image, Affine transform, float w, float h, float u0, float v0, float u1, float v1, int tint) {
	}

	@Override
	protected void applyClip(Rect clip) {
		this.clip = clip == null ? null : clip.copy();
	}

	static float shoelace(float[] q) {
		float a = 0f;
		for (int i = 0; i < 4; i++) {
			int j = (i + 1) % 4;
			a += q[i * 2] * q[j * 2 + 1] - q[j * 2] * q[i * 2 + 1];
		}
		return a;
	}
}
