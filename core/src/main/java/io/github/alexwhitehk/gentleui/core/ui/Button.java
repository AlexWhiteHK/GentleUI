package io.github.alexwhitehk.gentleui.core.ui;

import io.github.alexwhitehk.gentleui.core.host.UiSound;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.render.Align;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.render.Icon;
import io.github.alexwhitehk.gentleui.core.theme.ButtonStyle;
import java.util.function.Supplier;

/** Text button with an optional leading icon. */
public class Button extends Widget {
	protected Supplier<String> label;
	public Icon icon;
	public ButtonStyle style = ButtonStyle.TONAL;
	public Runnable onClick;
	/** Optional selected state painted as "active" (for toggle groups). */
	public Supplier<Boolean> selected;
	/** Content alignment; navigation lists use {@link Align#LEFT}. */
	public Align align = Align.CENTER;

	public Button(String label, Runnable onClick) {
		this(() -> label, onClick);
	}

	public Button(Supplier<String> label, Runnable onClick) {
		this.label = label;
		this.onClick = onClick;
	}

	public Button icon(Icon icon) {
		this.icon = icon;
		return this;
	}

	public Button style(ButtonStyle style) {
		this.style = style;
		return this;
	}

	public Button tooltip(String tip) {
		this.tooltip = tip;
		return this;
	}

	@Override
	public boolean interactive() {
		return true;
	}

	@Override
	public boolean focusable() {
		return true;
	}

	@Override
	public boolean mouseDown(UiContext ctx, float x, float y, int button) {
		startRipple(x, y);
		return true;
	}

	@Override
	public void mouseUp(UiContext ctx, float x, float y, int button, boolean inside) {
		if (inside && enabled) {
			activate(ctx);
		}
	}

	@Override
	public void activate(UiContext ctx) {
		ctx.host.playSound(UiSound.CLICK);
		if (onClick != null) {
			onClick.run();
		}
	}

	@Override
	protected void paint(UiContext ctx) {
		Canvas c = ctx.canvas;
		state.active = selected != null && selected.get() ? 1f : 0f;
		float s = 1f - state.press * 0.03f;
		c.push();
		c.scaleAround(bounds.cx(), bounds.cy(), s);
		ctx.skin().button(c, bounds, style, state);
		int color = ctx.skin().buttonText(style, state);
		String text = label.get();
		float iconSize = Math.min(14f, bounds.h - 6f);
		float textW = text.isEmpty() ? 0f : c.textWidth(text);
		float gap = icon != null && !text.isEmpty() ? 5f : 0f;
		float total = (icon != null ? iconSize : 0f) + gap + textW;
		float x = align == Align.LEFT ? bounds.x + 8f : bounds.cx() - total * 0.5f;
		if (total > bounds.w - 12f) {
			x = bounds.x + 6f;
		}
		if (icon != null) {
			icon.draw(c, x + iconSize * 0.5f, bounds.cy(), iconSize, color, 2.1f);
			x += iconSize + gap;
		}
		if (!text.isEmpty()) {
			String shown = c.ellipsize(text, bounds.right() - 6f - x, 1f);
			c.text(shown, x, bounds.cy() - c.fontHeight() * 0.5f + 0.5f, color, 1f, Align.LEFT, false);
		}
		c.pop();
		if (state.focus > 0f) {
			ctx.skin().focusRing(c, bounds, ctx.skin().radius(bounds.h), MathUtil.clamp01(state.focus));
		}
	}
}
