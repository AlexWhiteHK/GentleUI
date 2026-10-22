package io.github.alexwhitehk.gentleui.core.theme.liquid;

import io.github.alexwhitehk.gentleui.core.config.ConfigStore;
import io.github.alexwhitehk.gentleui.core.config.GentleConfig;
import io.github.alexwhitehk.gentleui.core.host.SoundSpec;
import io.github.alexwhitehk.gentleui.core.host.UiSound;
import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.scene.UiScreen;
import io.github.alexwhitehk.gentleui.core.theme.Backdrop;
import io.github.alexwhitehk.gentleui.core.theme.Skin;
import io.github.alexwhitehk.gentleui.core.theme.Theme;

/** Default theme: an ethereal lake with menus drawn out of its shores. */
public final class LiquidTheme implements Theme {
	public static final String ID = "liquid";

	private final Lake lake = new Lake();
	private final LiquidSkin skin = new LiquidSkin();

	public LiquidTheme() {
		configChanged();
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String nameKey() {
		return "gentleui.theme.liquid";
	}

	@Override
	public String iconId() {
		return "drop";
	}

	@Override
	public Skin skin() {
		return skin;
	}

	public Lake lake() {
		return lake;
	}

	@Override
	public void background(Canvas c, Backdrop b) {
		lake.render(c, b);
	}

	@Override
	public void pointerPressed(float x, float y) {
		lake.ripple(x, y, 0.8f * ConfigStore.config().liquid.rippleStrength);
	}

	@Override
	public UiScreen createTitleScreen() {
		return new LiquidTitleScreen(this);
	}

	private final float[] previewLine = new float[2 * 24];
	private final float[] water = new float[2 * 96];

	/** A drop that fills with water as progress grows; its surface ripples gently. */
	@Override
	public void progress(Canvas c, float cx, float cy, float radius, float progress, float time) {
		LiquidPalette p = lake.palette();
		float level = progress < 0f ? 0.5f + 0.08f * MathUtil.sin(time * 1.3f) : MathUtil.clamp01(progress);
		c.glow(cx, cy, radius * 2.6f, Argb.withAlpha(p.accent, 0.12f));
		c.fillCircle(cx, cy, radius, Argb.withAlpha(p.fill, p.light ? 0.6f : 0.06f));
		if (level > 0.005f) {
			// Water surface height inside the circle, from the bottom (-r) to the top (+r).
			float surfaceY = cy + radius - level * radius * 2f;
			int n = 0;
			int waveSegs = 24;
			float half = (float) Math.sqrt(Math.max(0f, radius * radius - (surfaceY - cy) * (surfaceY - cy)));
			for (int i = 0; i <= waveSegs && level < 0.995f; i++) {
				float t = (float) i / waveSegs;
				float x = cx - half + half * 2f * t;
				float amp = 1.6f * MathUtil.sin(t * MathUtil.PI);
				float y = surfaceY + MathUtil.sin(t * 9f + time * 3f) * amp;
				water[n * 2] = x;
				water[n * 2 + 1] = y;
				n++;
			}
			float a0 = level >= 0.995f ? 0f : MathUtil.atan2(surfaceY - cy, half);
			float a1 = level >= 0.995f ? MathUtil.TAU : MathUtil.PI - a0;
			int arcSegs = 40;
			for (int i = 1; i < arcSegs; i++) {
				float a = a0 + (a1 - a0) * i / arcSegs;
				water[n * 2] = cx + MathUtil.cos(a) * radius;
				water[n * 2 + 1] = cy + MathUtil.sin(a) * radius;
				n++;
			}
			c.fillPolygon(water, n, Argb.withAlpha(p.accent, p.light ? 0.75f : 0.45f));
		}
		c.strokeCircle(cx, cy, radius, c.pixel() * 1.5f, Argb.withAlpha(p.rim, p.light ? 0.9f : 0.6f));
		c.strokeArc(cx, cy, radius * 0.75f, (float) Math.toRadians(195), (float) Math.toRadians(245), c.pixel() * 1.6f,
			Argb.withAlpha(0xFFFFFFFF, 0.6f), Canvas.Cap.ROUND);
		for (int k = 0; k < 2; k++) {
			float t = (time * 0.45f + k * 0.5f) % 1f;
			c.strokeCircle(cx, cy, radius * (1.1f + t * 0.9f), c.pixel(), Argb.withAlpha(p.rim, 0.3f * (1f - t)));
		}
	}

	@Override
	public void preview(Canvas c, Rect r, float time) {
		LiquidPalette p = lake.palette();
		c.pushClip(r.x, r.y, r.w, r.h);
		c.fillRectGradientV(r.x, r.y, r.w, r.h, new int[] {p.bgTop, p.bgMid, p.bgBottom}, new float[] {0f, 0.55f, 1f});
		c.glow(r.x + r.w * 0.3f, r.y + r.h * 0.35f, Math.max(r.w, r.h) * 0.7f, Argb.withAlpha(p.glowA, 0.35f));
		float bx = r.x + r.w * 0.5f;
		float by = r.y + r.h * 0.55f;
		for (int li = 0; li < 6; li++) {
			float y0 = r.y + r.h * (li + 0.5f) / 6f;
			for (int i = 0; i < 24; i++) {
				float x = r.x + r.w * i / 23f;
				float dy = y0 - by;
				float push = 6f * (float) Math.exp(-((x - bx) * (x - bx) + dy * dy) / 220f) * (dy >= 0f ? 1f : -1f);
				previewLine[i * 2] = x;
				previewLine[i * 2 + 1] = y0 + MathUtil.sin(x * 0.08f + time + li) * 1.2f + push;
			}
			c.strokePolyline(previewLine, 24, false, c.pixel(), Argb.withAlpha(p.line, 0.35f), Canvas.Cap.BUTT);
		}
		float br = Math.min(r.w, r.h) * 0.16f;
		c.fillCircle(bx, by, br, Argb.withAlpha(p.accent, p.light ? 0.8f : 0.3f));
		c.strokeCircle(bx, by, br, c.pixel() * 1.3f, Argb.withAlpha(p.rim, 0.7f));
		c.popClip();
	}

	@Override
	public SoundSpec sound(UiSound sound) {
		return switch (sound) {
			case OPEN -> new SoundSpec("minecraft:block.bubble_column.bubble_pop", 1.25f, 0.55f);
			case CLOSE -> new SoundSpec("minecraft:ui.hud.bubble_pop", 0.85f, 0.45f);
			case CLICK -> new SoundSpec("minecraft:block.amethyst_block.chime", 1.5f, 0.45f);
			case TOGGLE -> new SoundSpec("minecraft:ui.hud.bubble_pop", 1.45f, 0.35f);
			default -> null;
		};
	}

	@Override
	public void configChanged() {
		GentleConfig cfg = ConfigStore.config();
		LiquidPalette p = LiquidPalette.byId(cfg.liquid.palette);
		lake.configure(p, cfg.liquid.rippleStrength, cfg.detail, cfg.pointerEffects);
		skin.apply(p);
	}
}
