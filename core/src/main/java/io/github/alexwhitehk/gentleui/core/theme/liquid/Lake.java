package io.github.alexwhitehk.gentleui.core.theme.liquid;

import io.github.alexwhitehk.gentleui.core.anim.Motion;
import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.math.Noise;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.theme.Backdrop;

/**
 * The living lake behind the Liquid theme: a calm gradient, slow pools of light, contour-like flow lines that
 * bend around the cursor and around floating bubbles, expanding ripples and drifting motes.
 *
 * <p>The lake is a single long-lived object so that its motion continues seamlessly from one menu to the next.
 */
public final class Lake {
	private static final int MAX_RIPPLES = 24;
	private static final int MAX_OBSTACLES = 24;
	private static final int MAX_MOTES = 64;

	private LiquidPalette palette = LiquidPalette.NIGHT;
	private float strength = 1f;
	private float detail = 1f;
	private boolean pointerEffects = true;

	// Pointer, smoothed so that lines glide rather than snap.
	private float px = -1000f;
	private float py = -1000f;
	private float sx = -1000f;
	private float sy = -1000f;
	private float presence;
	private float lastRippleX;
	private float lastRippleY;
	private float travelled;

	private final float[] rx = new float[MAX_RIPPLES];
	private final float[] ry = new float[MAX_RIPPLES];
	private final float[] rAge = new float[MAX_RIPPLES];
	private final float[] rAmp = new float[MAX_RIPPLES];
	private final float[] rSpeed = new float[MAX_RIPPLES];
	private int rippleCount;

	private final float[] ox = new float[MAX_OBSTACLES];
	private final float[] oy = new float[MAX_OBSTACLES];
	private final float[] orad = new float[MAX_OBSTACLES];
	private final float[] ostr = new float[MAX_OBSTACLES];
	private int obstacleCount;

	private final float[] mx = new float[MAX_MOTES];
	private final float[] my = new float[MAX_MOTES];
	private final float[] mDepth = new float[MAX_MOTES];
	private final float[] mPhase = new float[MAX_MOTES];
	private boolean motesSeeded;

	private float time;
	private float calm;
	private float width = 1f;
	private float height = 1f;

	private float[] pts = new float[512];
	private int[] cols = new int[256];

	public void configure(LiquidPalette palette, float strength, float detail, boolean pointerEffects) {
		this.palette = palette;
		this.strength = strength;
		this.detail = detail;
		this.pointerEffects = pointerEffects;
	}

	public LiquidPalette palette() {
		return palette;
	}

	/** Adds an expanding ring at (x, y). {@code amp} ~1 for a click, ~0.4 for movement. */
	public void ripple(float x, float y, float amp) {
		if (amp <= 0f) {
			return;
		}
		int i;
		if (rippleCount < MAX_RIPPLES) {
			i = rippleCount++;
		} else {
			// Replace the oldest ripple.
			i = 0;
			for (int k = 1; k < rippleCount; k++) {
				if (rAge[k] > rAge[i]) {
					i = k;
				}
			}
		}
		rx[i] = x;
		ry[i] = y;
		rAge[i] = 0f;
		rAmp[i] = amp;
		rSpeed[i] = 70f + 40f * Math.min(1f, amp);
	}

	/** Bubbles and other floating shapes register here every frame so the water flows around them. */
	public void obstacle(float x, float y, float radius, float str) {
		if (obstacleCount >= MAX_OBSTACLES || str <= 0.001f) {
			return;
		}
		ox[obstacleCount] = x;
		oy[obstacleCount] = y;
		orad[obstacleCount] = radius;
		ostr[obstacleCount] = str;
		obstacleCount++;
	}

	private void seedMotes() {
		for (int i = 0; i < MAX_MOTES; i++) {
			mx[i] = MathUtil.hash01(i * 3 + 1);
			my[i] = MathUtil.hash01(i * 3 + 2);
			mDepth[i] = 0.3f + 0.7f * MathUtil.hash01(i * 3 + 3);
			mPhase[i] = MathUtil.hash01(i * 7 + 11) * MathUtil.TAU;
		}
		motesSeeded = true;
	}

	private void update(Backdrop b) {
		float dt = Motion.reduced() ? b.dt * 0.25f : Motion.scale(b.dt);
		time += dt;
		width = b.width;
		height = b.height;
		calm = MathUtil.approach(calm, b.calm, 0.25f, b.dt);
		if (!motesSeeded) {
			seedMotes();
		}
		boolean pointerOn = pointerEffects && b.pointerActive && b.mouseX >= 0f && b.mouseY >= 0f && b.mouseX <= b.width && b.mouseY <= b.height;
		if (pointerOn) {
			px = b.mouseX;
			py = b.mouseY;
			if (sx < -500f) {
				sx = px;
				sy = py;
				lastRippleX = px;
				lastRippleY = py;
			}
		}
		presence = MathUtil.approach(presence, pointerOn ? 1f : 0f, 0.3f, b.dt);
		float ex = sx;
		float ey = sy;
		sx = MathUtil.approach(sx, px, 0.06f, b.dt);
		sy = MathUtil.approach(sy, py, 0.06f, b.dt);
		// Moving the pointer through the water leaves soft rings behind it.
		if (pointerOn && !Motion.reduced()) {
			travelled += MathUtil.dist(ex, ey, sx, sy);
			if (travelled > 46f && MathUtil.dist(lastRippleX, lastRippleY, sx, sy) > 30f) {
				ripple(sx, sy, 0.32f * strength);
				lastRippleX = sx;
				lastRippleY = sy;
				travelled = 0f;
			}
		}
		for (int i = 0; i < rippleCount; ) {
			rAge[i] += dt;
			if (rAge[i] > 4.2f) {
				rippleCount--;
				rx[i] = rx[rippleCount];
				ry[i] = ry[rippleCount];
				rAge[i] = rAge[rippleCount];
				rAmp[i] = rAmp[rippleCount];
				rSpeed[i] = rSpeed[rippleCount];
			} else {
				i++;
			}
		}
		for (int i = 0; i < MAX_MOTES; i++) {
			float n = Noise.noise2(i * 0.37f, time * 0.05f);
			mx[i] += (0.0035f + n * 0.004f) * dt * mDepth[i];
			my[i] -= (0.006f + 0.004f * mDepth[i]) * dt;
			if (mx[i] > 1.05f) {
				mx[i] -= 1.1f;
			}
			if (my[i] < -0.05f) {
				my[i] += 1.1f;
				mx[i] = MathUtil.hash01((int) (time * 1000f) + i);
			}
		}
	}

	/** Draws the lake. Obstacles registered since the previous call are consumed. */
	public void render(Canvas c, Backdrop b) {
		update(b);
		LiquidPalette p = palette;
		float w = b.width;
		float h = b.height;
		float act = 1f - calm * 0.55f;

		if (b.inWorld) {
			// Over a running world: a translucent veil tinted like the lake, the world stays visible through it.
			c.fillRectGradientV(0f, 0f, w, h,
				new int[] {Argb.withAlpha(p.bgTop, 0.78f), Argb.withAlpha(p.bgMid, 0.62f), Argb.withAlpha(p.bgBottom, 0.78f)},
				new float[] {0f, 0.5f, 1f});
		} else {
			c.fillRectGradientV(0f, 0f, w, h, new int[] {p.bgTop, p.bgMid, p.bgBottom}, new float[] {0f, 0.55f, 1f});
		}

		// Pools of light, drifting slowly and leaning toward the cursor.
		float parX = presence * (sx - w * 0.5f) / Math.max(1f, w);
		float parY = presence * (sy - h * 0.5f) / Math.max(1f, h);
		float big = Math.max(w, h);
		float gax = w * (0.28f + 0.06f * Noise.noise2(time * 0.03f, 1.3f)) + parX * 40f;
		float gay = h * (0.30f + 0.06f * Noise.noise2(time * 0.03f, 7.1f)) + parY * 30f;
		float gbx = w * (0.74f + 0.06f * Noise.noise2(time * 0.025f, 3.7f)) - parX * 30f;
		float gby = h * (0.72f + 0.05f * Noise.noise2(time * 0.025f, 9.2f)) - parY * 24f;
		float glowAlpha = p.light ? 0.55f : 0.30f;
		c.glow(gax, gay, big * 0.62f, Argb.withAlpha(p.glowA, glowAlpha * (0.8f + 0.2f * act)));
		c.glow(gbx, gby, big * 0.55f, Argb.withAlpha(p.glowB, glowAlpha * 0.9f * (0.8f + 0.2f * act)));
		if (presence > 0.01f) {
			c.glow(sx, sy, 120f, Argb.withAlpha(p.accent, (p.light ? 0.10f : 0.07f) * presence * act));
		}

		renderLines(c, w, h, act);
		renderRipples(c, act);
		renderMotes(c, w, h, act, parX, parY);

		// Gentle vignette to hold the eye in the middle.
		int edge = Argb.withAlpha(p.shadow, p.light ? 0.07f : 0.35f);
		c.fillRectGradientV(0f, 0f, w, h * 0.22f, new int[] {edge, Argb.withAlpha(p.shadow, 0f)}, new float[] {0f, 1f});
		c.fillRectGradientV(0f, h * 0.78f, w, h * 0.22f, new int[] {Argb.withAlpha(p.shadow, 0f), edge}, new float[] {0f, 1f});

		obstacleCount = 0;
	}

	private void renderLines(Canvas c, float w, float h, float act) {
		LiquidPalette p = palette;
		float spacing = MathUtil.clamp(15f / detail, 10f, 34f);
		int lines = (int) (h / spacing) + 2;
		float step = MathUtil.clamp(7f / detail, 4f, 14f);
		int n = (int) (w / step) + 2;
		if (pts.length < n * 2) {
			pts = new float[n * 2];
			cols = new int[n];
		}
		float drift = (time * 3.2f) % spacing;
		float lineAlphaBase = p.light ? 0.22f : 0.16f;
		float push = 13f * strength * presence;
		float sigma = 60f;
		float inv2Sigma = 1f / (sigma * sigma);
		float lineWidth = c.pixel() * 1.15f;
		for (int li = 0; li < lines; li++) {
			float baseY = li * spacing - drift + spacing * 0.5f;
			int lineSeed = li + (int) Math.floor((time * 3.2f) / spacing);
			float phase = MathUtil.hash01(lineSeed * 13 + 5) * MathUtil.TAU;
			float variety = 0.55f + 0.45f * MathUtil.hash01(lineSeed * 31 + 7);
			float vertFade = MathUtil.smoothstep(0f, h * 0.12f, baseY) * MathUtil.smoothstep(h, h * 0.88f, baseY);
			if (vertFade <= 0.01f) {
				continue;
			}
			for (int i = 0; i < n; i++) {
				float x = Math.min(w, i * step);
				float y = baseY;
				y += MathUtil.sin(x * 0.012f + time * 0.35f + phase) * 2.6f * act;
				y += Noise.noise2(x * 0.006f + lineSeed * 0.4f, time * 0.08f) * 5.5f * act;
				float glowBoost = 0f;
				if (push > 0.01f) {
					float dx = x - sx;
					float dy = baseY - sy;
					float d2 = dx * dx + dy * dy;
					float g = (float) Math.exp(-d2 * inv2Sigma);
					float dir = dy >= 0f ? 1f : -1f;
					y += dir * push * g * (0.6f + 0.4f * MathUtil.clamp01(Math.abs(dy) / 18f));
					glowBoost += g * 0.55f * presence;
				}
				for (int k = 0; k < obstacleCount; k++) {
					float dx = x - ox[k];
					float dy = baseY - oy[k];
					float reach = orad[k] * 2.4f;
					float d2 = dx * dx + dy * dy;
					if (d2 < reach * reach) {
						float d = MathUtil.sqrt(d2);
						float g = 1f - d / reach;
						g = g * g * (3f - 2f * g);
						float dir = dy >= 0f ? 1f : -1f;
						y += dir * orad[k] * 0.55f * g * ostr[k];
						glowBoost += g * 0.35f * ostr[k];
					}
				}
				for (int k = 0; k < rippleCount; k++) {
					float age = rAge[k];
					float radius = age * rSpeed[k];
					float dx = x - rx[k];
					float dy = baseY - ry[k];
					float d = MathUtil.sqrt(dx * dx + dy * dy);
					float band = (d - radius) / 14f;
					if (band > -3f && band < 3f) {
						float decay = rAmp[k] * (1f - age / 4.2f) * (1f - age / 4.2f);
						float ring = (float) Math.exp(-band * band) * decay;
						float dir = dy >= 0f ? 1f : -1f;
						y += dir * ring * 7f * strength;
						glowBoost += ring * 0.7f;
					}
				}
				pts[i * 2] = x;
				pts[i * 2 + 1] = y;
				float edgeFade = MathUtil.smoothstep(0f, w * 0.14f, x) * MathUtil.smoothstep(w, w * 0.86f, x);
				float a = (lineAlphaBase * variety * act + glowBoost * 0.32f) * edgeFade * vertFade;
				cols[i] = Argb.withAlpha(p.line, MathUtil.clamp01(a));
			}
			c.strokePolyline(pts, cols, n, false, lineWidth, Canvas.Cap.BUTT);
		}
	}

	private void renderRipples(Canvas c, float act) {
		LiquidPalette p = palette;
		for (int k = 0; k < rippleCount; k++) {
			float age = rAge[k];
			float life = 1f - age / 4.2f;
			float radius = age * rSpeed[k];
			float a = rAmp[k] * life * life * (p.light ? 0.45f : 0.35f) * (0.6f + 0.4f * act);
			if (a < 0.004f || radius < 1f) {
				continue;
			}
			c.strokeCircle(rx[k], ry[k], radius, c.pixel() * 1.3f, Argb.withAlpha(p.rim, Math.min(1f, a)));
			if (radius > 16f) {
				c.strokeCircle(rx[k], ry[k], radius * 0.72f, c.pixel(), Argb.withAlpha(p.rim, Math.min(1f, a * 0.45f)));
			}
		}
	}

	private void renderMotes(Canvas c, float w, float h, float act, float parX, float parY) {
		LiquidPalette p = palette;
		int count = (int) MathUtil.clamp(40f * detail, 0f, MAX_MOTES);
		for (int i = 0; i < count; i++) {
			float depth = mDepth[i];
			float x = mx[i] * w - parX * 26f * depth;
			float y = my[i] * h - parY * 18f * depth;
			float tw = 0.5f + 0.5f * MathUtil.sin(time * (0.6f + depth) + mPhase[i]);
			float fade = MathUtil.smoothstep(0f, 0.12f, my[i]) * MathUtil.smoothstep(1f, 0.85f, my[i]);
			float a = (0.18f + 0.5f * tw) * depth * fade * act;
			if (a < 0.01f) {
				continue;
			}
			float r = 0.5f + depth * 0.9f;
			c.glow(x, y, r * 5f, Argb.withAlpha(p.mote, a * 0.35f));
			c.fillCircle(x, y, r, Argb.withAlpha(p.mote, a));
		}
	}
}
