package io.github.alexwhitehk.gentleui.core.ui;

import io.github.alexwhitehk.gentleui.core.host.UiSound;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.render.Icon;
import io.github.alexwhitehk.gentleui.core.theme.ButtonStyle;

/** Round icon-only button; always give it a tooltip so its purpose is discoverable. */
public class IconButton extends Widget {
	public Icon icon;
	public ButtonStyle style = ButtonStyle.GHOST;
	public Runnable onClick;

	public IconButton(Icon icon, String tooltip, Runnable onClick) {
		this.icon = icon;
		this.tooltip = tooltip;
		this.onClick = onClick;
	}

	public IconButton style(ButtonStyle s) {
		this.style = s;
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
		float s = 1f - state.press * 0.06f + state.hover * 0.02f;
		c.push();
		c.scaleAround(bounds.cx(), bounds.cy(), s);
		ctx.skin().iconButton(c, bounds, style, state);
		float size = Math.min(bounds.w, bounds.h) * 0.52f;
		icon.draw(c, bounds.cx(), bounds.cy(), size, ctx.skin().iconColor(style, state), 2f);
		c.pop();
		if (state.focus > 0f) {
			ctx.skin().focusRing(c, bounds, Math.min(bounds.w, bounds.h) * 0.5f, state.focus);
		}
	}
}
