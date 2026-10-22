package io.github.alexwhitehk.gentleui.core.theme.material;

import io.github.alexwhitehk.gentleui.core.anim.Motion;
import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.math.Noise;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.render.Paint;
import io.github.alexwhitehk.gentleui.core.theme.Backdrop;

/**
 * Bright canvas with playful, slowly turning shapes in the four brand colors. Shapes sit on depth layers that
 * shift with the pointer, and taps spread an ink ripple.
 */
public final class MaterialBackground {
	private record ShapeSpec(Shapes.Kind kind, int brand, boolean pastel, float x, float y, float size, float depth, float spin, float calmX, float calmY) {
	}

	private static final ShapeSpec[] SHAPES = {
		new ShapeSpec(Shapes.Kind.COOKIE, MaterialColors.BLUE, true, 0.82f, 0.30f, 0.34f, 0.35f, 0.05f, 1.02f, 0.05f),
		new ShapeSpec(Shapes.Kind.CIRCLE, MaterialColors.RED, true, 0.68f, 0.70f, 0.16f, 0.55f, 0f, 0.95f, 0.95f),
		new ShapeSpec(Shapes.Kind.PILL, MaterialColors.GREEN, true, 0.95f, 0.78f, 0.20f, 0.45f, -0.04f, 0.04f, 0.98f),
		new ShapeSpec(Shapes.Kind.FLOWER, MaterialColors.YELLOW, true, 0.60f, 0.16f, 0.11f, 0.7f, 0.12f, -0.02f, 0.10f),
		new ShapeSpec(Shapes.Kind.TRIANGLE, MaterialColors.GREEN, false, 0.74f, 0.10f, 0.035f, 0.9f, 0.3f, 0.06f, 0.30f),
		new ShapeSpec(Shapes.Kind.CIRCLE, MaterialColors.BLUE, false, 0.57f, 0.88f, 0.028f, 1f, 0f, 0.88f, 0.97f),
		new ShapeSpec(Shapes.Kind.SQUIRCLE, MaterialColors.YELLOW, false, 0.93f, 0.52f, 0.032f, 0.85f, 0.2f, 0.97f, 0.60f),
		new ShapeSpec(Shapes.Kind.BURST, MaterialColors.RED, false, 0.035f, 0.62f, 0.03f, 0.8f, 0.25f, 0.03f, 0.40f),
		new ShapeSpec(Shapes.Kind.CLOVER, MaterialColors.BLUE, false, 0.52f, 0.93f, 0.035f, 0.9f, -0.18f, 0.97f, 0.30f),
	};

	private static final int MAX_INK = 8;
	private final float[] inkX = new float[MAX_INK];
	private final float[] inkY = new float[MAX_INK];
	private final float[] inkAge = new float[MAX_INK];
	private int inkCount;
	private final float[] pts = new float[2 * 160];
	private final Paint paint = new Paint();
	private float time;
	private float calm;
	private float px;
	private float py;
	private float presence;
	private boolean showShapes = true;

	public void configure(boolean shapes) {
		this.showShapes = shapes;
	}

	public void ink(float x, float y) {
		int i = inkCount < MAX_INK ? inkCount++ : 0;
		inkX[i] = x;
		inkY[i] = y;
		inkAge[i] = 0f;
	}

	public void render(Canvas c, Backdrop b, MaterialColors colors) {
		float dt = Motion.scale(b.dt);
		time += dt;
		calm = MathUtil.approach(calm, b.calm, 0.22f, b.dt);
		boolean pointer = b.pointerActive && b.mouseX >= 0f && b.mouseY >= 0f;
		if (pointer) {
			px = MathUtil.approach(px, b.mouseX, 0.08f, b.dt);
			py = MathUtil.approach(py, b.mouseY, 0.08f, b.dt);
		}
		presence = MathUtil.approach(presence, pointer ? 1f : 0f, 0.3f, b.dt);
		float w = b.width;
		float h = b.height;
		int bg = colors.palette.background;
		if (b.inWorld) {
			c.fillRectSharp(0f, 0f, w, h, Argb.withAlpha(bg, 0.82f));
		} else {
			c.fillRectSharp(0f, 0f, w, h, bg);
		}
		float m = Math.min(w, h);
		float parX = presence * (px / Math.max(1f, w) - 0.5f);
		float parY = presence * (py / Math.max(1f, h) - 0.5f);
		if (showShapes) {
			for (int i = 0; i < SHAPES.length; i++) {
				ShapeSpec s = SHAPES[i];
				float drift = Motion.reduced() ? 0f : 1f;
				float x = MathUtil.lerp(s.x, s.calmX, calm) * w + Noise.noise2(i * 3.1f, time * 0.05f) * 10f * drift - parX * 34f * s.depth;
				float y = MathUtil.lerp(s.y, s.calmY, calm) * h + Noise.noise2(i * 5.7f, time * 0.05f + 4f) * 10f * drift - parY * 24f * s.depth;
				float size = s.size * m * MathUtil.lerp(1f, s.pastel ? 0.7f : 0.85f, calm);
				float near = presence * (1f - MathUtil.clamp01(MathUtil.dist(x, y, px, py) / (size + 60f)));
				size *= 1f + 0.06f * near;
				float rot = time * s.spin * drift + i + near * 0.3f;
				int color = s.pastel ? colors.pastel(s.brand) : s.brand;
				float alpha = (s.pastel ? 1f : 0.95f) * MathUtil.lerp(1f, s.pastel ? 0.55f : 0.7f, calm);
				if (b.inWorld) {
					alpha *= 0.35f;
				}
				int segs = Math.min(160, Math.max(48, (int) (size / c.pixel() / 3f)));
				Shapes.outline(s.kind, x, y, size, rot, segs, pts);
				c.fillPolygon(pts, segs, paint.setSolid(Argb.withAlpha(color, alpha)));
			}
			// A small grid of dots, a familiar decoration in product illustrations.
			float gx = MathUtil.lerp(0.86f, 0.9f, calm) * w - parX * 12f;
			float gy = MathUtil.lerp(0.58f, 0.86f, calm) * h - parY * 8f;
			int dots = Argb.withAlpha(colors.palette.outline, b.inWorld ? 0.2f : 0.45f * (1f - calm * 0.5f));
			for (int ix = 0; ix < 4; ix++) {
				for (int iy = 0; iy < 4; iy++) {
					c.fillCircle(gx + ix * 7f, gy + iy * 7f, 1.2f, dots);
				}
			}
		}
		for (int k = 0; k < inkCount; ) {
			inkAge[k] += dt;
			float t = inkAge[k] / 0.9f;
			if (t >= 1f) {
				inkCount--;
				inkX[k] = inkX[inkCount];
				inkY[k] = inkY[inkCount];
				inkAge[k] = inkAge[inkCount];
				continue;
			}
			float e = 1f - (1f - t) * (1f - t);
			c.fillCircle(inkX[k], inkY[k], 8f + e * m * 0.35f, Argb.withAlpha(colors.palette.accent, 0.10f * (1f - t)));
			k++;
		}
	}
}
