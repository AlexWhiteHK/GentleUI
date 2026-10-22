package io.github.alexwhitehk.gentleui.core.theme.liquid;

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

/** Controls as droplets: soft pills, hairline rims and light that gathers where the pointer is. */
public final class LiquidSkin implements Skin {
	private final Palette palette = new Palette();
	private LiquidPalette lp = LiquidPalette.NIGHT;
	private final Paint paint = new Paint();
	private final Rect tmp = new Rect();

	public LiquidSkin() {
		apply(LiquidPalette.NIGHT);
	}

	public void apply(LiquidPalette p) {
		this.lp = p;
		palette.light = p.light;
		palette.background = p.base();
		palette.surface = Argb.withAlpha(p.light ? 0xFFFFFFFF : p.bgMid, p.light ? 0.6f : 0.55f);
		palette.surfaceVariant = Argb.withAlpha(p.fill, p.light ? 0.5f : 0.07f);
		palette.outline = Argb.withAlpha(p.rim, p.light ? 0.35f : 0.16f);
		palette.text = p.text;
		palette.textMuted = p.textMuted;
		palette.textDisabled = Argb.withAlpha(p.textMuted, 0.6f);
		palette.accent = p.accent;
		palette.onAccent = p.light ? 0xFFFFFFFF : 0xFF04141C;
		palette.accentContainer = Argb.withAlpha(p.accent, 0.22f);
		palette.onAccentContainer = p.text;
		palette.danger = 0xFFFF7A85;
		palette.shadow = p.shadow;
		palette.highlight = p.rim;
	}

	public LiquidPalette liquidPalette() {
		return lp;
	}

	@Override
	public Palette palette() {
		return palette;
	}

	@Override
	public float radius(float height) {
		return height * 0.5f;
	}

	private float fillAlpha(WidgetState s) {
		if (!s.enabled) {
			return lp.light ? 0.25f : 0.03f;
		}
		float base = lp.light ? 0.42f : 0.06f;
		float hover = lp.light ? 0.30f : 0.08f;
		return base + hover * s.hover + (lp.light ? 0.1f : 0.06f) * s.press;
	}

	@Override
	public void button(Canvas c, Rect r, ButtonStyle style, WidgetState s) {
		float rad = radius(r.h);
		float h = s.hover;
		if ((style == ButtonStyle.GHOST || style == ButtonStyle.NAV) && h < 0.01f && s.active < 0.01f) {
			return;
		}
		if (s.enabled && h > 0.01f) {
			c.shadow(r.x, r.y, r.w, r.h, rad, 12f, 1f, Argb.withAlpha(lp.accent, (lp.light ? 0.18f : 0.22f) * h));
		}
		int fillColor;
		int rimColor;
		switch (style) {
			case PRIMARY -> {
				fillColor = Argb.withAlpha(lp.accent, (lp.light ? 0.85f : 0.30f) + 0.12f * h + 0.3f * s.active);
				rimColor = Argb.withAlpha(lp.accent, 0.55f + 0.35f * h);
			}
			case DANGER -> {
				fillColor = Argb.withAlpha(0xFFFF5A6A, 0.16f + 0.14f * h);
				rimColor = Argb.withAlpha(0xFFFF8A94, 0.45f + 0.3f * h);
			}
			default -> {
				fillColor = Argb.withAlpha(lp.fill, fillAlpha(s) + 0.1f * s.active);
				rimColor = Argb.withAlpha(lp.rim, (lp.light ? 0.55f : 0.14f) + (lp.light ? 0.3f : 0.24f) * h + 0.25f * s.active);
			}
		}
		if (!s.enabled) {
			rimColor = Argb.withAlpha(lp.rim, lp.light ? 0.25f : 0.06f);
		}
		// Vertical sheen: slightly brighter at the top, like light on a water surface.
		int top = Argb.shade(fillColor, lp.light ? 0.15f : 0.06f);
		paint.setLinear(r.x, r.y, top, r.x, r.bottom(), fillColor);
		c.fillRoundRect(r.x, r.y, r.w, r.h, rad, paint);
		c.strokeRoundRect(r.x, r.y, r.w, r.h, rad, c.pixel() * 1.5f, rimColor);
		if (s.enabled && h > 0.01f) {
			// A thin highlight that slides in along the top edge.
			float inset = rad * 0.8f;
			float len = (r.w - inset * 2f) * (0.35f + 0.65f * h);
			float cx = r.cx();
			c.line(cx - len * 0.5f, r.y + 1.6f, cx + len * 0.5f, r.y + 1.6f, c.pixel() * 1.2f, Argb.withAlpha(lp.rim, 0.45f * h), Canvas.Cap.ROUND);
		}
	}

	@Override
	public void iconButton(Canvas c, Rect r, ButtonStyle style, WidgetState s) {
		float rad = Math.min(r.w, r.h) * 0.5f;
		float cx = r.cx();
		float cy = r.cy();
		float h = s.hover;
		if (h > 0.01f) {
			c.glow(cx, cy, rad * 2.2f, Argb.withAlpha(lp.accent, 0.22f * h));
		}
		if (style != ButtonStyle.GHOST || h > 0.01f) {
			int fill = style == ButtonStyle.PRIMARY ? Argb.withAlpha(lp.accent, 0.35f + 0.2f * h) : Argb.withAlpha(lp.fill, fillAlpha(s));
			c.fillCircle(cx, cy, rad, fill);
		}
		c.strokeCircle(cx, cy, rad - c.pixel(), c.pixel() * 1.4f, Argb.withAlpha(lp.rim, (style == ButtonStyle.GHOST ? 0.0f : 0.18f) + 0.32f * h));
	}

	@Override
	public int buttonText(ButtonStyle style, WidgetState s) {
		if (!s.enabled) {
			return palette.textDisabled;
		}
		if (style == ButtonStyle.PRIMARY && lp.light) {
			return 0xFFFFFFFF;
		}
		if (style == ButtonStyle.NAV) {
			return Argb.lerp(Argb.lerp(lp.textMuted, lp.text, s.hover), lp.text, s.active);
		}
		return Argb.lerp(Argb.withAlpha(lp.text, 0.88f), lp.text, s.hover);
	}

	@Override
	public void toggle(Canvas c, Rect r, WidgetState s) {
		float on = s.active;
		float rad = r.h * 0.5f;
		int off = Argb.withAlpha(lp.fill, lp.light ? 0.6f : 0.10f);
		int onC = Argb.withAlpha(lp.accent, lp.light ? 0.9f : 0.55f);
		c.fillRoundRect(r.x, r.y, r.w, r.h, rad, Argb.lerp(off, onC, MathUtil.clamp01(on)));
		c.strokeRoundRect(r.x, r.y, r.w, r.h, rad, c.pixel() * 1.3f, Argb.withAlpha(lp.rim, 0.2f + 0.25f * s.hover));
		float kr = rad - 2.2f;
		// The knob stretches while travelling, like a drop sliding.
		float kx = MathUtil.lerp(r.x + rad, r.right() - rad, on);
		float stretch = 1f + 0.35f * MathUtil.sin(MathUtil.clamp01(on) * MathUtil.PI);
		c.fillEllipse(kx, r.cy(), kr * stretch, kr, lp.light ? 0xFFFFFFFF : Argb.lerp(0xFFD9EEF5, 0xFFFFFFFF, on));
	}

	@Override
	public void checkbox(Canvas c, Rect r, WidgetState s) {
		float rad = Math.min(r.w, r.h) * 0.5f - 1f;
		float cx = r.cx();
		float cy = r.cy();
		float on = MathUtil.clamp01(s.active);
		if (s.hover > 0.01f) {
			c.glow(cx, cy, rad * 2.4f, Argb.withAlpha(lp.accent, 0.2f * s.hover));
		}
		c.fillCircle(cx, cy, rad, Argb.withAlpha(lp.fill, fillAlpha(s)));
		c.strokeCircle(cx, cy, rad - c.pixel(), c.pixel() * 1.5f, Argb.withAlpha(lp.rim, 0.3f + 0.3f * s.hover));
		if (on > 0.01f) {
			c.fillCircle(cx, cy, rad * (0.4f + 0.6f * on), Argb.withAlpha(lp.accent, 0.9f * on));
			Icon.CHECK.draw(c, cx, cy, rad * 1.5f, Argb.withAlpha(palette.onAccent, on), 2.6f);
		}
	}

	@Override
	public void slider(Canvas c, Rect r, float value, WidgetState s) {
		float cy = r.cy();
		float x0 = r.x + 5f;
		float x1 = r.right() - 5f;
		float vx = MathUtil.lerp(x0, x1, value);
		c.line(x0, cy, x1, cy, 2f, Argb.withAlpha(lp.rim, lp.light ? 0.35f : 0.18f), Canvas.Cap.ROUND);
		c.line(x0, cy, vx, cy, 2f, Argb.withAlpha(lp.accent, 0.95f), Canvas.Cap.ROUND);
		float kr = 4.5f + 1.2f * s.hover + 1f * s.active;
		c.glow(vx, cy, kr * 3.2f, Argb.withAlpha(lp.accent, 0.25f + 0.25f * s.hover));
		c.fillCircle(vx, cy, kr, lp.light ? 0xFFFFFFFF : 0xFFF4FDFF);
		c.strokeCircle(vx, cy, kr - c.pixel(), c.pixel() * 1.4f, Argb.withAlpha(lp.accent, 0.9f));
	}

	@Override
	public void sliderContainer(Canvas c, Rect r, float value, WidgetState s) {
		float rad = radius(r.h);
		c.fillRoundRect(r.x, r.y, r.w, r.h, rad, Argb.withAlpha(lp.fill, fillAlpha(s) * 0.8f));
		if (value >= 0f) {
			float fw = MathUtil.clamp(r.w * value, rad * 2f, r.w);
			paint.setLinear(r.x, r.y, Argb.withAlpha(lp.accent, 0.10f), r.x + fw, r.y, Argb.withAlpha(lp.accent, 0.32f + 0.12f * s.hover));
			c.fillRoundRect(r.x, r.y, fw, r.h, rad, paint);
			float kx = MathUtil.clamp(r.x + r.w * value, r.x + rad, r.right() - rad);
			c.glow(kx, r.cy(), r.h * 0.9f, Argb.withAlpha(lp.accent, 0.18f + 0.2f * s.hover));
			c.fillRoundRect(kx - 1.5f, r.y + 4f, 3f, r.h - 8f, 1.5f, Argb.withAlpha(lp.rim, 0.75f + 0.25f * s.hover));
		}
		c.strokeRoundRect(r.x, r.y, r.w, r.h, rad, c.pixel() * 1.5f, Argb.withAlpha(lp.rim, (lp.light ? 0.5f : 0.14f) + 0.24f * s.hover));
	}

	@Override
	public void textField(Canvas c, Rect r, WidgetState s) {
		float rad = Math.min(radius(r.h), 8f);
		float focus = MathUtil.clamp01(Math.max(s.focus, s.active));
		c.fillRoundRect(r.x, r.y, r.w, r.h, rad, Argb.withAlpha(lp.light ? 0xFFFFFFFF : 0xFF000000, lp.light ? 0.55f : 0.22f));
		int rim = Argb.lerp(Argb.withAlpha(lp.rim, (lp.light ? 0.45f : 0.16f) + 0.15f * s.hover), Argb.withAlpha(lp.accent, 0.85f), focus);
		if (focus > 0.01f) {
			c.shadow(r.x, r.y, r.w, r.h, rad, 8f, 0f, Argb.withAlpha(lp.accent, 0.18f * focus));
		}
		c.strokeRoundRect(r.x, r.y, r.w, r.h, rad, c.pixel() * 1.5f, rim);
	}

	@Override
	public void tab(Canvas c, Rect r, WidgetState s) {
		float sel = MathUtil.clamp01(s.active);
		if (s.hover > 0.01f || sel > 0.01f) {
			c.fillRoundRect(r.x + 2f, r.y + 2f, r.w - 4f, r.h - 4f, (r.h - 4f) * 0.5f, Argb.withAlpha(lp.fill, (lp.light ? 0.4f : 0.05f) * Math.max(s.hover, sel)));
		}
		if (sel > 0.01f) {
			float w = (r.w - 16f) * sel;
			c.line(r.cx() - w * 0.5f, r.bottom() - 2.5f, r.cx() + w * 0.5f, r.bottom() - 2.5f, 2f, Argb.withAlpha(lp.accent, sel), Canvas.Cap.ROUND);
			c.glow(r.cx(), r.bottom() - 2.5f, r.w * 0.4f, Argb.withAlpha(lp.accent, 0.15f * sel));
		}
	}

	@Override
	public void scrollbar(Canvas c, Rect track, Rect thumb, WidgetState s) {
		float w = Math.min(thumb.w, 3f + s.hover);
		float x = thumb.cx() - w * 0.5f;
		c.fillRoundRect(x, thumb.y, w, thumb.h, w * 0.5f, Argb.withAlpha(lp.rim, (lp.light ? 0.45f : 0.28f) + 0.25f * s.hover));
	}

	@Override
	public void panel(Canvas c, Rect r, PanelStyle style) {
		switch (style) {
			case CARD -> {
				float rad = 12f;
				c.shadow(r.x, r.y + 3f, r.w, r.h, rad, 18f, 0f, Argb.withAlpha(lp.shadow, lp.light ? 0.12f : 0.35f));
				c.fillRoundRect(r.x, r.y, r.w, r.h, rad, Argb.withAlpha(lp.light ? 0xFFFFFFFF : lp.bgTop, lp.light ? 0.62f : 0.55f));
				c.strokeRoundRect(r.x, r.y, r.w, r.h, rad, c.pixel() * 1.3f, Argb.withAlpha(lp.rim, lp.light ? 0.6f : 0.10f));
			}
			case SHEET -> {
				float rad = 16f;
				c.shadow(r.x, r.y + 4f, r.w, r.h, rad, 28f, 0f, Argb.withAlpha(lp.shadow, lp.light ? 0.10f : 0.4f));
				c.fillRoundRect(r.x, r.y, r.w, r.h, rad, Argb.withAlpha(lp.light ? 0xFFFFFFFF : lp.bgTop, lp.light ? 0.5f : 0.42f));
				c.strokeRoundRect(r.x, r.y, r.w, r.h, rad, c.pixel() * 1.3f, Argb.withAlpha(lp.rim, lp.light ? 0.55f : 0.08f));
			}
			case LIST -> {
				float rad = 10f;
				c.fillRoundRect(r.x, r.y, r.w, r.h, rad, Argb.withAlpha(lp.light ? 0xFFFFFFFF : 0xFF000000, lp.light ? 0.35f : 0.22f));
				c.strokeRoundRect(r.x, r.y, r.w, r.h, rad, c.pixel() * 1.2f, Argb.withAlpha(lp.rim, lp.light ? 0.4f : 0.07f));
			}
			case POPOVER -> {
				float rad = 7f;
				c.shadow(r.x, r.y + 2f, r.w, r.h, rad, 10f, 0f, Argb.withAlpha(lp.shadow, lp.light ? 0.15f : 0.45f));
				c.fillRoundRect(r.x, r.y, r.w, r.h, rad, Argb.withAlpha(lp.light ? 0xFFFFFFFF : lp.bgTop, 0.94f));
				c.strokeRoundRect(r.x, r.y, r.w, r.h, rad, c.pixel() * 1.3f, Argb.withAlpha(lp.rim, lp.light ? 0.6f : 0.16f));
			}
		}
	}

	@Override
	public void selection(Canvas c, Rect r, float strength, boolean focused) {
		if (strength <= 0.01f) {
			return;
		}
		float rad = Math.min(8f, r.h * 0.5f);
		c.fillRoundRect(r.x, r.y, r.w, r.h, rad, Argb.withAlpha(lp.fill, (lp.light ? 0.45f : 0.07f) * strength));
		if (focused) {
			c.strokeRoundRect(r.x, r.y, r.w, r.h, rad, c.pixel() * 1.5f, Argb.withAlpha(lp.accent, 0.7f * strength));
		} else {
			c.strokeRoundRect(r.x, r.y, r.w, r.h, rad, c.pixel() * 1.2f, Argb.withAlpha(lp.rim, (lp.light ? 0.4f : 0.12f) * strength));
		}
	}

	@Override
	public void separator(Canvas c, float x, float y, float w) {
		float[] pts = {x, y, x + w * 0.5f, y, x + w, y};
		int edge = Argb.withAlpha(lp.rim, 0f);
		int mid = Argb.withAlpha(lp.rim, lp.light ? 0.45f : 0.16f);
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
		tmp.set(r.x - 2f, r.y - 2f, r.w + 4f, r.h + 4f);
		c.strokeRoundRect(tmp.x, tmp.y, tmp.w, tmp.h, radius + 2f, 1.2f, Argb.withAlpha(lp.accent, 0.85f * amount));
	}
}
