package io.github.alexwhitehk.gentleui.core.theme.glass;

import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.render.Icon;
import io.github.alexwhitehk.gentleui.core.render.Paint;
import io.github.alexwhitehk.gentleui.core.theme.ButtonStyle;
import io.github.alexwhitehk.gentleui.core.theme.Palette;
import io.github.alexwhitehk.gentleui.core.theme.PanelStyle;
import io.github.alexwhitehk.gentleui.core.theme.Skin;
import io.github.alexwhitehk.gentleui.core.theme.WidgetState;

/** Frosted glass controls: translucent fills, light-catching rims and soft depth. */
public final class GlassSkin implements Skin {
	private final Palette palette = new Palette();
	private GlassPalette gp = GlassPalette.AURORA;
	private float frost = 0.55f;
	private final Paint fill = new Paint();
	private final Paint rim = new Paint();
	private final Rect tmp = new Rect();

	public GlassSkin() {
		apply(GlassPalette.AURORA, 0.55f);
	}

	public void apply(GlassPalette p, float frost) {
		this.gp = p;
		this.frost = frost;
		palette.light = p.light;
		palette.background = Argb.lerp(p.baseTop, p.baseBottom, 0.5f);
		palette.surface = Argb.withAlpha(0xFFFFFFFF, p.light ? 0.5f : 0.1f);
		palette.surfaceVariant = Argb.withAlpha(0xFFFFFFFF, p.light ? 0.35f : 0.06f);
		palette.outline = Argb.withAlpha(p.light ? 0xFF1B2030 : 0xFFFFFFFF, p.light ? 0.18f : 0.22f);
		palette.text = p.text;
		palette.textMuted = p.textMuted;
		palette.textDisabled = Argb.withAlpha(p.textMuted, 0.6f);
		palette.accent = p.accent;
		palette.onAccent = p.light ? 0xFFFFFFFF : 0xFF0B1020;
		palette.accentContainer = Argb.withAlpha(p.accent, 0.3f);
		palette.onAccentContainer = p.text;
		palette.danger = 0xFFFF6B7A;
		palette.shadow = p.light ? 0xFF2A3350 : 0xFF000000;
		palette.highlight = 0xFFFFFFFF;
	}

	public GlassPalette glassPalette() {
		return gp;
	}

	@Override
	public Palette palette() {
		return palette;
	}

	@Override
	public float radius(float height) {
		return Math.min(height * 0.5f, 12f);
	}

	/**
	 * Draws a glass slab.
	 *
	 * @param lift   0..1 extra brightness (hover)
	 * @param tint   optional color mixed into the glass (0 for none)
	 * @param depth  shadow strength multiplier
	 */
	public void glass(Canvas c, Rect r, float radius, float lift, int tint, float depth) {
		boolean light = gp.light;
		if (depth > 0f) {
			c.shadow(r.x, r.y + 2f + 2f * depth, r.w, r.h, radius, 10f + 10f * depth, 0f, Argb.withAlpha(palette.shadow, (light ? 0.10f : 0.28f) * depth));
		}
		float f = frost;
		int top = Argb.withAlpha(0xFFFFFFFF, (light ? 0.55f : 0.15f) * (0.6f + 0.8f * f) + 0.08f * lift);
		int bottom = Argb.withAlpha(0xFFFFFFFF, (light ? 0.32f : 0.06f) * (0.6f + 0.8f * f) + 0.05f * lift);
		if ((tint >>> 24) != 0) {
			top = Argb.lerp(top, Argb.withAlpha(tint, Argb.a(tint) / 255f), 0.6f);
			bottom = Argb.lerp(bottom, Argb.withAlpha(tint, Argb.a(tint) / 255f * 0.8f), 0.6f);
		}
		fill.setLinear(r.x, r.y, top, r.x, r.bottom(), bottom);
		c.fillRoundRect(r.x, r.y, r.w, r.h, radius, fill);
		// Rim catches light at the top-left and fades toward the bottom-right.
		int rimHi = Argb.withAlpha(0xFFFFFFFF, (light ? 0.85f : 0.42f) + 0.25f * lift);
		int rimLo = Argb.withAlpha(0xFFFFFFFF, (light ? 0.35f : 0.08f) + 0.1f * lift);
		rim.setLinear(r.x, r.y, rimHi, r.x + r.w * 0.6f, r.bottom(), rimLo);
		c.strokeRoundRect(r.x, r.y, r.w, r.h, radius, c.pixel() * 1.5f, rim);
		// Specular line just inside the top edge.
		float inset = Math.max(radius, 6f);
		if (r.w > inset * 2f + 4f) {
			c.line(r.x + inset, r.y + 1.4f, r.right() - inset, r.y + 1.4f, c.pixel() * 1.2f, Argb.withAlpha(0xFFFFFFFF, (light ? 0.9f : 0.35f) + 0.2f * lift), Canvas.Cap.ROUND);
		}
	}

	@Override
	public void button(Canvas c, Rect r, ButtonStyle style, WidgetState s) {
		float rad = radius(r.h);
		if ((style == ButtonStyle.GHOST || style == ButtonStyle.NAV) && s.hover < 0.01f && s.active < 0.01f) {
			return;
		}
		int tint = switch (style) {
			case PRIMARY -> Argb.withAlpha(gp.accent, gp.light ? 0.55f : 0.35f);
			case DANGER -> Argb.withAlpha(0xFFFF4D6D, 0.3f);
			default -> s.active > 0.5f ? Argb.withAlpha(gp.accent, 0.25f) : 0;
		};
		if (!s.enabled) {
			c.pushAlpha(0.55f);
		}
		if (s.hover > 0.01f && s.enabled) {
			c.glow(r.cx(), r.cy(), Math.max(r.w, r.h) * 0.75f, Argb.withAlpha(gp.accent, 0.12f * s.hover));
		}
		tmp.set(r.x, r.y + s.press * 0.6f, r.w, r.h);
		glass(c, tmp, rad, s.hover - 0.5f * s.press, tint, 0.6f + 0.6f * s.hover);
		if (!s.enabled) {
			c.popAlpha();
		}
	}

	@Override
	public void iconButton(Canvas c, Rect r, ButtonStyle style, WidgetState s) {
		float rad = Math.min(r.w, r.h) * 0.5f;
		if (style == ButtonStyle.GHOST && s.hover < 0.01f) {
			return;
		}
		int tint = style == ButtonStyle.PRIMARY ? Argb.withAlpha(gp.accent, 0.4f) : (style == ButtonStyle.DANGER ? Argb.withAlpha(0xFFFF4D6D, 0.3f) : 0);
		glass(c, r, rad, s.hover, tint, 0.4f + 0.6f * s.hover);
	}

	@Override
	public int buttonText(ButtonStyle style, WidgetState s) {
		if (!s.enabled) {
			return palette.textDisabled;
		}
		if (style == ButtonStyle.DANGER && !gp.light) {
			return 0xFFFFC2CA;
		}
		if (style == ButtonStyle.NAV) {
			return Argb.lerp(Argb.lerp(gp.textMuted, gp.text, s.hover), gp.text, s.active);
		}
		return gp.text;
	}

	@Override
	public void toggle(Canvas c, Rect r, WidgetState s) {
		float on = MathUtil.clamp01(s.active);
		float rad = r.h * 0.5f;
		glass(c, r, rad, s.hover * 0.5f, Argb.withAlpha(gp.accent, (gp.light ? 0.75f : 0.6f) * on), 0f);
		float kr = rad - 2f;
		float kx = MathUtil.lerp(r.x + rad, r.right() - rad, s.active);
		c.shadow(kx - kr, r.cy() - kr + 1f, kr * 2f, kr * 2f, kr, 4f, 0f, Argb.withAlpha(0xFF000000, 0.25f));
		c.fillCircle(kx, r.cy(), kr, 0xFFFFFFFF);
	}

	@Override
	public void checkbox(Canvas c, Rect r, WidgetState s) {
		float on = MathUtil.clamp01(s.active);
		float size = Math.min(r.w, r.h) - 2f;
		tmp.set(r.cx() - size * 0.5f, r.cy() - size * 0.5f, size, size);
		glass(c, tmp, 4.5f, Math.max(s.hover, s.focus), Argb.withAlpha(gp.accent, 0.8f * on), 0.3f);
		if (on > 0.01f) {
			Icon.CHECK.draw(c, tmp.cx(), tmp.cy(), size * 0.9f, Argb.withAlpha(gp.light ? 0xFFFFFFFF : 0xFF0B1020, on), 2.8f);
		}
	}

	@Override
	public void slider(Canvas c, Rect r, float value, WidgetState s) {
		float cy = r.cy();
		tmp.set(r.x, cy - 2.5f, r.w, 5f);
		glass(c, tmp, 2.5f, 0f, 0, 0f);
		float vx = MathUtil.lerp(r.x + 3f, r.right() - 3f, value);
		c.fillRoundRect(r.x, cy - 2.5f, vx - r.x, 5f, 2.5f, Argb.withAlpha(gp.accent, 0.9f));
		float kr = 5.5f + s.hover + s.active;
		c.shadow(vx - kr, cy - kr + 1.5f, kr * 2f, kr * 2f, kr, 5f, 0f, Argb.withAlpha(0xFF000000, 0.3f));
		c.fillCircle(vx, cy, kr, 0xFFFFFFFF);
	}

	@Override
	public void sliderContainer(Canvas c, Rect r, float value, WidgetState s) {
		float rad = radius(r.h);
		glass(c, r, rad, s.hover * 0.6f, 0, 0.4f);
		if (value >= 0f) {
			float fw = MathUtil.clamp(r.w * value, rad * 2f, r.w);
			c.fillRoundRect(r.x + 1f, r.y + 1f, fw - 2f, r.h - 2f, rad - 1f, Argb.withAlpha(gp.accent, gp.light ? 0.35f : 0.28f));
			float kx = MathUtil.clamp(r.x + r.w * value, r.x + rad, r.right() - rad);
			c.fillRoundRect(kx - 2f, r.y + 3f, 4f, r.h - 6f, 2f, 0xFFFFFFFF);
		}
	}

	@Override
	public void textField(Canvas c, Rect r, WidgetState s) {
		float rad = Math.min(radius(r.h), 8f);
		float focus = MathUtil.clamp01(Math.max(s.active, s.focus));
		c.fillRoundRect(r.x, r.y, r.w, r.h, rad, Argb.withAlpha(gp.light ? 0xFFFFFFFF : 0xFF000000, gp.light ? 0.5f : 0.22f));
		if (focus > 0.01f) {
			c.glow(r.cx(), r.cy(), Math.max(r.w, r.h) * 0.6f, Argb.withAlpha(gp.accent, 0.12f * focus));
		}
		int border = Argb.lerp(Argb.withAlpha(gp.light ? 0xFF1B2030 : 0xFFFFFFFF, 0.2f + 0.15f * s.hover), Argb.withAlpha(gp.accent, 0.9f), focus);
		c.strokeRoundRect(r.x, r.y, r.w, r.h, rad, c.pixel() * 1.5f, border);
	}

	@Override
	public void tab(Canvas c, Rect r, WidgetState s) {
		float sel = MathUtil.clamp01(s.active);
		if (sel > 0.01f || s.hover > 0.01f) {
			tmp.set(r.x + 2f, r.y + 2f, r.w - 4f, r.h - 4f);
			c.pushAlpha(Math.max(sel, s.hover * 0.6f));
			glass(c, tmp, radius(tmp.h), s.hover, sel > 0.5f ? Argb.withAlpha(gp.accent, 0.2f) : 0, 0.3f);
			c.popAlpha();
		}
	}

	@Override
	public void scrollbar(Canvas c, Rect track, Rect thumb, WidgetState s) {
		float w = Math.min(thumb.w + s.hover, 4f);
		c.fillRoundRect(thumb.cx() - w * 0.5f, thumb.y, w, thumb.h, w * 0.5f, Argb.withAlpha(gp.light ? 0xFF1B2030 : 0xFFFFFFFF, 0.3f + 0.25f * s.hover));
	}

	@Override
	public void panel(Canvas c, Rect r, PanelStyle style) {
		switch (style) {
			case CARD -> glass(c, r, 16f, 0f, 0, 1f);
			case SHEET -> glass(c, r, 22f, 0f, 0, 1.4f);
			case LIST -> {
				c.fillRoundRect(r.x, r.y, r.w, r.h, 16f, Argb.withAlpha(gp.light ? 0xFFFFFFFF : 0xFF000000, gp.light ? 0.25f : 0.16f));
				rim.setLinear(r.x, r.y, Argb.withAlpha(0xFFFFFFFF, gp.light ? 0.8f : 0.22f), r.x, r.bottom(), Argb.withAlpha(0xFFFFFFFF, gp.light ? 0.3f : 0.05f));
				c.strokeRoundRect(r.x, r.y, r.w, r.h, 16f, c.pixel() * 1.4f, rim);
			}
			case POPOVER -> {
				c.shadow(r.x, r.y + 3f, r.w, r.h, 10f, 14f, 0f, Argb.withAlpha(palette.shadow, gp.light ? 0.15f : 0.4f));
				c.fillRoundRect(r.x, r.y, r.w, r.h, 10f, Argb.withAlpha(gp.light ? 0xFFF8FAFF : Argb.lerp(gp.baseTop, gp.baseBottom, 0.5f), 0.92f));
				c.strokeRoundRect(r.x, r.y, r.w, r.h, 10f, c.pixel() * 1.4f, Argb.withAlpha(0xFFFFFFFF, gp.light ? 0.9f : 0.2f));
			}
		}
	}

	@Override
	public void selection(Canvas c, Rect r, float strength, boolean focused) {
		if (strength <= 0.01f) {
			return;
		}
		float rad = Math.min(10f, r.h * 0.5f);
		c.pushAlpha(MathUtil.clamp01(strength));
		glass(c, r, rad, 0f, focused ? Argb.withAlpha(gp.accent, 0.2f) : 0, 0f);
		c.popAlpha();
	}

	@Override
	public void separator(Canvas c, float x, float y, float w) {
		float[] pts = {x, y, x + w * 0.5f, y, x + w, y};
		int edge = Argb.withAlpha(0xFFFFFFFF, 0f);
		int mid = Argb.withAlpha(gp.light ? 0xFF1B2030 : 0xFFFFFFFF, 0.25f);
		c.strokePolyline(pts, new int[] {edge, mid, edge}, 3, false, c.pixel() * 1.2f, Canvas.Cap.BUTT);
	}

	@Override
	public void tooltip(Canvas c, Rect r) {
		panel(c, r, PanelStyle.POPOVER);
	}

	@Override
	public void focusRing(Canvas c, Rect r, float radius, float amount) {
		if (amount <= 0.01f) {
			return;
		}
		tmp.set(r.x - 2.5f, r.y - 2.5f, r.w + 5f, r.h + 5f);
		c.strokeRoundRect(tmp.x, tmp.y, tmp.w, tmp.h, radius + 2.5f, 1.5f, Argb.withAlpha(gp.accent, amount));
	}
}
