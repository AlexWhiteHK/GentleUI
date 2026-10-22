package io.github.alexwhitehk.gentleui.core.theme.material;

import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.render.Icon;
import io.github.alexwhitehk.gentleui.core.theme.ButtonStyle;
import io.github.alexwhitehk.gentleui.core.theme.Palette;
import io.github.alexwhitehk.gentleui.core.theme.PanelStyle;
import io.github.alexwhitehk.gentleui.core.theme.Skin;
import io.github.alexwhitehk.gentleui.core.theme.WidgetState;

/** Flat, tonal controls with state layers and ink ripples. */
public final class MaterialSkin implements Skin {
	private MaterialColors colors = new MaterialColors(0xFF1A73E8, false);
	private final Rect tmp = new Rect();

	public void apply(MaterialColors colors) {
		this.colors = colors;
	}

	public MaterialColors colors() {
		return colors;
	}

	@Override
	public Palette palette() {
		return colors.palette;
	}

	@Override
	public float radius(float height) {
		return height * 0.5f;
	}

	private int container(ButtonStyle style, WidgetState s) {
		Palette p = colors.palette;
		if (!s.enabled) {
			return Argb.withAlpha(p.text, 0.10f);
		}
		return switch (style) {
			case PRIMARY -> p.accent;
			case DANGER -> colors.dark ? 0xFF8C1D18 : 0xFFF9DEDC;
			case GHOST -> Argb.withAlpha(p.accent, 0f);
			case NAV -> Argb.withAlpha(p.accentContainer, MathUtil.clamp01(s.active));
			default -> s.active > 0.5f ? p.accentContainer : colors.palette.surfaceVariant;
		};
	}

	@Override
	public int buttonText(ButtonStyle style, WidgetState s) {
		Palette p = colors.palette;
		if (!s.enabled) {
			return p.textDisabled;
		}
		return switch (style) {
			case PRIMARY -> p.onAccent;
			case DANGER -> colors.dark ? 0xFFF9DEDC : 0xFF8C1D18;
			case GHOST -> p.accent;
			case NAV -> s.active > 0.5f ? p.onAccentContainer : Argb.lerp(p.textMuted, p.text, s.hover);
			default -> s.active > 0.5f ? p.onAccentContainer : p.text;
		};
	}

	@Override
	public int iconColor(ButtonStyle style, WidgetState s) {
		if (style == ButtonStyle.GHOST && s.enabled) {
			return Argb.lerp(colors.palette.textMuted, colors.palette.text, s.hover);
		}
		return buttonText(style, s);
	}

	/** Hover/press "state layer": the content color laid over the container at low opacity. */
	private int stateLayer(int content, WidgetState s) {
		return Argb.withAlpha(content, 0.08f * s.hover + 0.06f * s.press + 0.04f * s.focus);
	}

	private void ink(Canvas c, Rect r, float radius, int content, WidgetState s) {
		if (s.ripple <= 0f) {
			return;
		}
		float t = s.ripple;
		float e = 1f - (1f - t) * (1f - t);
		float ox = r.x + MathUtil.clamp(s.rippleX, 0f, r.w);
		float oy = r.y + MathUtil.clamp(s.rippleY, 0f, r.h);
		float x0 = MathUtil.lerp(ox - 4f, r.x, e);
		float y0 = MathUtil.lerp(oy - 4f, r.y, e);
		float x1 = MathUtil.lerp(ox + 4f, r.right(), e);
		float y1 = MathUtil.lerp(oy + 4f, r.bottom(), e);
		float rad = MathUtil.lerp(4f, radius, e);
		c.fillRoundRect(x0, y0, x1 - x0, y1 - y0, rad, Argb.withAlpha(content, 0.16f * (1f - t * t)));
	}

	@Override
	public void button(Canvas c, Rect r, ButtonStyle style, WidgetState s) {
		float rad = radius(r.h);
		int content = buttonText(style, s);
		if (style == ButtonStyle.PRIMARY && s.enabled) {
			float lift = s.hover;
			c.shadow(r.x, r.y + 1f + lift, r.w, r.h, rad, 3f + 5f * lift, 0f, Argb.withAlpha(colors.palette.shadow, (colors.dark ? 0.35f : 0.14f) + 0.08f * lift));
		}
		if (style != ButtonStyle.GHOST && (style != ButtonStyle.NAV || s.active > 0.01f)) {
			c.fillRoundRect(r.x, r.y, r.w, r.h, rad, container(style, s));
		}
		if (s.enabled) {
			c.fillRoundRect(r.x, r.y, r.w, r.h, rad, stateLayer(content, s));
			ink(c, r, rad, content, s);
		}
	}

	@Override
	public void iconButton(Canvas c, Rect r, ButtonStyle style, WidgetState s) {
		float rad = Math.min(r.w, r.h) * 0.5f;
		int content = buttonText(style, s);
		if (style != ButtonStyle.GHOST) {
			c.fillCircle(r.cx(), r.cy(), rad, container(style, s));
		} else {
			c.strokeCircle(r.cx(), r.cy(), rad - 0.5f, 1f, colors.outlineVariant);
		}
		if (s.enabled) {
			c.fillCircle(r.cx(), r.cy(), rad, stateLayer(style == ButtonStyle.GHOST ? colors.palette.text : content, s));
			ink(c, r, rad, content, s);
		}
	}

	@Override
	public void toggle(Canvas c, Rect r, WidgetState s) {
		Palette p = colors.palette;
		float on = MathUtil.clamp01(s.active);
		float rad = r.h * 0.5f;
		int track = Argb.lerp(p.surfaceVariant, p.accent, on);
		c.fillRoundRect(r.x, r.y, r.w, r.h, rad, track);
		if (on < 0.99f) {
			c.strokeRoundRect(r.x, r.y, r.w, r.h, rad, 1.3f, Argb.withAlpha(p.outline, 1f - on));
		}
		float knobR = MathUtil.lerp(rad * 0.5f, rad - 2f, on) + s.press * 1.2f;
		float kx = MathUtil.lerp(r.x + rad, r.right() - rad, s.active);
		if (s.hover > 0.01f) {
			c.fillCircle(kx, r.cy(), rad + 3f, Argb.withAlpha(on > 0.5f ? p.accent : p.text, 0.08f * s.hover));
		}
		c.fillCircle(kx, r.cy(), knobR, Argb.lerp(p.outline, p.onAccent, on));
		if (on > 0.6f) {
			Icon.CHECK.draw(c, kx, r.cy(), knobR * 1.3f, Argb.withAlpha(p.accent, (on - 0.6f) / 0.4f), 3f);
		}
	}

	@Override
	public void checkbox(Canvas c, Rect r, WidgetState s) {
		Palette p = colors.palette;
		float on = MathUtil.clamp01(s.active);
		float size = Math.min(r.w, r.h) - 4f;
		float x = r.cx() - size * 0.5f;
		float y = r.cy() - size * 0.5f;
		if (s.hover > 0.01f || s.focus > 0.01f) {
			c.fillCircle(r.cx(), r.cy(), size * 0.5f + 5f, Argb.withAlpha(on > 0.5f ? p.accent : p.text, 0.08f * Math.max(s.hover, s.focus)));
		}
		if (on > 0.01f) {
			c.fillRoundRect(x, y, size, size, 2.5f, Argb.withAlpha(p.accent, on));
			Icon.CHECK.draw(c, r.cx(), r.cy(), size * 0.95f, Argb.withAlpha(p.onAccent, on), 2.8f);
		}
		if (on < 0.99f) {
			c.strokeRoundRect(x, y, size, size, 2.5f, 1.6f, Argb.withAlpha(p.textMuted, 1f - on));
		}
	}

	@Override
	public void slider(Canvas c, Rect r, float value, WidgetState s) {
		Palette p = colors.palette;
		float cy = r.cy();
		float x0 = r.x + 2f;
		float x1 = r.right() - 2f;
		float vx = MathUtil.lerp(x0, x1, value);
		float gap = 4f;
		float th = 4f;
		if (vx - gap > x0) {
			c.fillRoundRect(x0, cy - th * 0.5f, vx - gap - x0, th, th * 0.5f, p.accent);
		}
		if (x1 > vx + gap) {
			c.fillRoundRect(vx + gap, cy - th * 0.5f, x1 - vx - gap, th, th * 0.5f, p.accentContainer);
		}
		float hw = MathUtil.lerp(3f, 2f, s.active);
		float hh = r.h + 2f;
		c.fillRoundRect(vx - hw, cy - hh * 0.5f, hw * 2f, hh, hw, p.accent);
		if (s.hover > 0.01f) {
			c.fillCircle(vx, cy, hh * 0.6f, Argb.withAlpha(p.accent, 0.1f * s.hover));
		}
	}

	@Override
	public void sliderContainer(Canvas c, Rect r, float value, WidgetState s) {
		Palette p = colors.palette;
		float rad = radius(r.h);
		c.fillRoundRect(r.x, r.y, r.w, r.h, rad, p.surfaceVariant);
		if (value >= 0f) {
			float fw = MathUtil.clamp(r.w * value, rad * 2f, r.w);
			c.fillRoundRect(r.x, r.y, fw, r.h, rad, p.accentContainer);
			float hx = MathUtil.clamp(r.x + r.w * value, r.x + rad, r.right() - rad);
			c.fillRoundRect(hx - 2f, r.y + 3f, 4f, r.h - 6f, 2f, p.accent);
		}
		c.fillRoundRect(r.x, r.y, r.w, r.h, rad, stateLayer(p.text, s));
	}

	@Override
	public void textField(Canvas c, Rect r, WidgetState s) {
		Palette p = colors.palette;
		float focus = MathUtil.clamp01(Math.max(s.active, s.focus));
		c.fillRoundRect(r.x, r.y, r.w, r.h, 4f, p.surface);
		int border = Argb.lerp(Argb.lerp(p.outline, p.text, s.hover * 0.6f), p.accent, focus);
		c.strokeRoundRect(r.x, r.y, r.w, r.h, 4f, MathUtil.lerp(1f, 2f, focus), border);
	}

	@Override
	public void tab(Canvas c, Rect r, WidgetState s) {
		Palette p = colors.palette;
		float sel = MathUtil.clamp01(s.active);
		if (s.hover > 0.01f) {
			c.fillRoundRect(r.x + 2f, r.y + 2f, r.w - 4f, r.h - 4f, 8f, Argb.withAlpha(p.text, 0.06f * s.hover));
		}
		if (sel > 0.01f) {
			float w = Math.min(r.w - 12f, 48f) * sel;
			c.fillRoundRect(r.cx() - w * 0.5f, r.bottom() - 3f, w, 3f, 1.5f, p.accent);
		}
	}

	@Override
	public void scrollbar(Canvas c, Rect track, Rect thumb, WidgetState s) {
		Palette p = colors.palette;
		float w = Math.min(thumb.w + s.hover, 4f);
		c.fillRoundRect(thumb.cx() - w * 0.5f, thumb.y, w, thumb.h, w * 0.5f, Argb.lerp(colors.outlineVariant, p.outline, s.hover));
	}

	@Override
	public void panel(Canvas c, Rect r, PanelStyle style) {
		Palette p = colors.palette;
		float shadowA = colors.dark ? 0.4f : 0.10f;
		switch (style) {
			case CARD -> {
				c.shadow(r.x, r.y + 1.5f, r.w, r.h, 16f, 6f, 0f, Argb.withAlpha(p.shadow, shadowA));
				c.fillRoundRect(r.x, r.y, r.w, r.h, 16f, p.surface);
			}
			case SHEET -> {
				c.shadow(r.x, r.y + 3f, r.w, r.h, 24f, 16f, 0f, Argb.withAlpha(p.shadow, shadowA * 1.2f));
				c.fillRoundRect(r.x, r.y, r.w, r.h, 24f, p.surface);
			}
			case LIST -> {
				c.fillRoundRect(r.x, r.y, r.w, r.h, 16f, colors.dark ? p.surface : 0xFFFFFFFF);
				c.strokeRoundRect(r.x, r.y, r.w, r.h, 16f, 1f, colors.outlineVariant);
			}
			case POPOVER -> {
				c.shadow(r.x, r.y + 2f, r.w, r.h, 8f, 8f, 0f, Argb.withAlpha(p.shadow, shadowA * 1.4f));
				c.fillRoundRect(r.x, r.y, r.w, r.h, 8f, colors.containerHigh);
			}
		}
	}

	@Override
	public void selection(Canvas c, Rect r, float strength, boolean focused) {
		if (strength <= 0.01f) {
			return;
		}
		Palette p = colors.palette;
		float rad = Math.min(12f, r.h * 0.5f);
		c.fillRoundRect(r.x, r.y, r.w, r.h, rad, Argb.withAlpha(p.accentContainer, MathUtil.clamp01(strength)));
		if (focused) {
			c.strokeRoundRect(r.x, r.y, r.w, r.h, rad, 1.5f, Argb.withAlpha(p.accent, strength));
		}
	}

	@Override
	public void separator(Canvas c, float x, float y, float w) {
		c.fillRect(x + 8f, y - 0.5f, w - 16f, 1f, colors.outlineVariant);
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
		tmp.set(r.x - 3f, r.y - 3f, r.w + 6f, r.h + 6f);
		c.strokeRoundRect(tmp.x, tmp.y, tmp.w, tmp.h, radius + 3f, 2f, Argb.withAlpha(colors.palette.accent, amount));
	}
}
