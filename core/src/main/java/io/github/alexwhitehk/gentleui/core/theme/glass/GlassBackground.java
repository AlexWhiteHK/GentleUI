package io.github.alexwhitehk.gentleui.core.theme.glass;

import io.github.alexwhitehk.gentleui.core.anim.Motion;
import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.math.Noise;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.theme.Backdrop;

/** Large luminous orbs drifting over a dark gradient; the nearest one leans toward the pointer. */
public final class GlassBackground {
	private static final float[][] HOME = {
		{0.22f, 0.28f, 0.55f},
		{0.80f, 0.22f, 0.48f},
		{0.70f, 0.82f, 0.52f},
		{0.18f, 0.84f, 0.42f},
		{0.50f, 0.50f, 0.36f},
	};

	private float time;
	private float calm;
	private float px;
	private float py;
	private float presence;
	private final float[] ox = new float[5];
	private final float[] oy = new float[5];
	private boolean seeded;
	private final float[] sparkX = new float[6];
	private final float[] sparkY = new float[6];
	private final float[] sparkAge = new float[6];
	private int sparks;

	public void spark(float x, float y) {
		int i = sparks < sparkX.length ? sparks++ : 0;
		sparkX[i] = x;
		sparkY[i] = y;
		sparkAge[i] = 0f;
	}

	public void render(Canvas c, Backdrop b, GlassPalette p) {
		float dt = Motion.scale(b.dt);
		time += dt * (Motion.reduced() ? 0.2f : 1f);
		calm = MathUtil.approach(calm, b.calm, 0.3f, b.dt);
		float w = b.width;
		float h = b.height;
		boolean pointer = b.pointerActive && b.mouseX >= 0f && b.mouseY >= 0f;
		if (pointer) {
			px = MathUtil.approach(px, b.mouseX, 0.12f, b.dt);
			py = MathUtil.approach(py, b.mouseY, 0.12f, b.dt);
		}
		presence = MathUtil.approach(presence, pointer ? 1f : 0f, 0.4f, b.dt);
		if (b.inWorld) {
			c.fillRectGradientV(0f, 0f, w, h, new int[] {Argb.withAlpha(p.baseTop, 0.55f), Argb.withAlpha(p.baseBottom, 0.65f)}, new float[] {0f, 1f});
		} else {
			c.fillRectGradientV(0f, 0f, w, h, new int[] {p.baseTop, p.baseBottom}, new float[] {0f, 1f});
		}
		float big = Math.max(w, h);
		float strength = (p.light ? 0.85f : 0.62f) * (1f - 0.2f * calm) * (b.inWorld ? 0.55f : 1f);
		for (int i = 0; i < 5; i++) {
			float[] home = HOME[i];
			float tx = home[0] * w + MathUtil.sin(time * (0.11f + i * 0.023f) + i * 1.7f) * w * 0.10f + Noise.noise2(i, time * 0.04f) * w * 0.05f;
			float ty = home[1] * h + MathUtil.cos(time * (0.09f + i * 0.019f) + i * 2.3f) * h * 0.10f + Noise.noise2(i + 9f, time * 0.04f) * h * 0.05f;
			// Pointer: parallax for all, gentle attraction for the closest orbs.
			float d = MathUtil.dist(tx, ty, px, py);
			float pull = presence * 0.22f * (1f - MathUtil.clamp01(d / (big * 0.6f)));
			tx = MathUtil.lerp(tx, px, pull) - presence * (px / Math.max(1f, w) - 0.5f) * 20f * (i % 2 == 0 ? 1f : -1f);
			ty = MathUtil.lerp(ty, py, pull) - presence * (py / Math.max(1f, h) - 0.5f) * 14f;
			if (!seeded) {
				ox[i] = tx;
				oy[i] = ty;
			}
			ox[i] = MathUtil.approach(ox[i], tx, 0.25f, b.dt);
			oy[i] = MathUtil.approach(oy[i], ty, 0.25f, b.dt);
			float r = home[2] * big * (0.92f + 0.08f * MathUtil.sin(time * 0.3f + i));
			c.glow(ox[i], oy[i], r, Argb.withAlpha(p.orbs[i], strength));
		}
		seeded = true;
		if (presence > 0.01f) {
			c.glow(px, py, big * 0.18f, Argb.withAlpha(p.light ? 0xFFFFFFFF : p.accent, (p.light ? 0.35f : 0.12f) * presence));
		}
		for (int k = 0; k < sparks; ) {
			sparkAge[k] += dt;
			float t = sparkAge[k] / 0.7f;
			if (t >= 1f) {
				sparks--;
				sparkX[k] = sparkX[sparks];
				sparkY[k] = sparkY[sparks];
				sparkAge[k] = sparkAge[sparks];
				continue;
			}
			float e = 1f - (1f - t) * (1f - t);
			c.glow(sparkX[k], sparkY[k], 20f + e * 90f, Argb.withAlpha(p.light ? 0xFFFFFFFF : p.accent, 0.25f * (1f - t)));
			k++;
		}
		// Soft top sheen.
		c.fillRectGradientV(0f, 0f, w, h * 0.35f, new int[] {Argb.withAlpha(0xFFFFFFFF, p.light ? 0.25f : 0.04f), Argb.withAlpha(0xFFFFFFFF, 0f)}, new float[] {0f, 1f});
	}
}
