package io.github.alexwhitehk.gentleui.core.ui;

import io.github.alexwhitehk.gentleui.core.host.Host;
import io.github.alexwhitehk.gentleui.core.host.Keys;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.theme.Theme;
import java.util.ArrayList;
import java.util.List;

/** Hosts a widget tree: dispatches input, tracks hover/focus/capture and draws tooltips. */
public final class WidgetRoot {
	public final Widget root = new Widget();
	public final UiContext ctx = new UiContext();
	private float tooltipTimer;
	private String lastTooltip;
	private final Rect tooltipRect = new Rect();

	public void render(Canvas canvas, Theme theme, Host host, float mouseX, float mouseY, float dt, float time) {
		ctx.canvas = canvas;
		ctx.theme = theme;
		ctx.host = host;
		ctx.dt = dt;
		ctx.time = time;
		ctx.mouseX = mouseX;
		ctx.mouseY = mouseY;
		ctx.hovered = ctx.captured != null && ctx.pointerDown ? hitCaptured(mouseX, mouseY) : root.hit(mouseX, mouseY);
		ctx.tooltip = null;
		root.render(ctx);
		renderTooltip(canvas, dt);
	}

	private Widget hitCaptured(float x, float y) {
		Widget h = root.hit(x, y);
		return h == ctx.captured ? h : null;
	}

	private void renderTooltip(Canvas c, float dt) {
		String tip = ctx.hovered != null && ctx.hovered.enabled ? ctx.hovered.tooltip : null;
		if (tip == null || tip.isEmpty() || ctx.pointerDown) {
			tooltipTimer = 0f;
			lastTooltip = null;
			return;
		}
		if (!tip.equals(lastTooltip)) {
			lastTooltip = tip;
			tooltipTimer = 0f;
		}
		tooltipTimer += dt;
		float a = Math.min(1f, Math.max(0f, (tooltipTimer - 0.45f) / 0.15f));
		if (a <= 0f) {
			return;
		}
		List<String> lines = wrap(c, tip, 200f);
		float lh = c.fontHeight() + 2f;
		float w = 0f;
		for (String l : lines) {
			w = Math.max(w, c.textWidth(l));
		}
		w += 12f;
		float h = lines.size() * lh + 8f;
		float x = ctx.mouseX + 10f;
		float y = ctx.mouseY + 14f;
		if (x + w > c.width() - 4f) {
			x = ctx.mouseX - w - 6f;
		}
		if (y + h > c.height() - 4f) {
			y = ctx.mouseY - h - 6f;
		}
		tooltipRect.set(x, y, w, h);
		c.pushAlpha(a);
		ctx.skin().tooltip(c, tooltipRect);
		for (int i = 0; i < lines.size(); i++) {
			c.text(lines.get(i), x + 6f, y + 5f + i * lh, ctx.palette().text);
		}
		c.popAlpha();
	}

	/** Greedy word wrap that also breaks CJK text between any two characters. */
	public static List<String> wrap(Canvas c, String text, float maxWidth) {
		List<String> out = new ArrayList<>();
		for (String para : text.split("\n", -1)) {
			StringBuilder line = new StringBuilder();
			int i = 0;
			while (i < para.length()) {
				int end = i;
				char ch = para.charAt(i);
				if (Character.isWhitespace(ch)) {
					end = i + 1;
				} else if (ch < 0x2E80) {
					while (end < para.length() && !Character.isWhitespace(para.charAt(end)) && para.charAt(end) < 0x2E80) {
						end++;
					}
				} else {
					end = i + Character.charCount(para.codePointAt(i));
				}
				String token = para.substring(i, end);
				if (line.length() > 0 && c.textWidth(line + token) > maxWidth) {
					out.add(line.toString().stripTrailing());
					line.setLength(0);
					if (token.isBlank()) {
						i = end;
						continue;
					}
				}
				line.append(token);
				i = end;
			}
			out.add(line.toString().stripTrailing());
		}
		return out;
	}

	public void layout(float width, float height) {
		root.bounds.set(0f, 0f, width, height);
		root.layout();
	}

	public boolean mouseDown(float x, float y, int button) {
		ctx.mouseX = x;
		ctx.mouseY = y;
		ctx.keyboardFocusVisible = false;
		Widget target = root.hit(x, y);
		if (target != null && target.enabled) {
			ctx.pointerDown = true;
			ctx.captured = target;
			setFocus(target.focusable() ? target : null);
			return target.mouseDown(ctx, x, y, button) || true;
		}
		setFocus(null);
		return false;
	}

	public boolean mouseUp(float x, float y, int button) {
		Widget c = ctx.captured;
		ctx.pointerDown = false;
		ctx.captured = null;
		if (c != null) {
			c.mouseUp(ctx, x, y, button, root.hit(x, y) == c);
			return true;
		}
		return false;
	}

	public boolean mouseDragged(float x, float y, float dx, float dy) {
		ctx.mouseX = x;
		ctx.mouseY = y;
		if (ctx.captured != null) {
			ctx.captured.mouseDragged(ctx, x, y, dx, dy);
			return true;
		}
		return false;
	}

	public boolean mouseScrolled(float x, float y, float amount) {
		Widget w = root.hit(x, y);
		if (w == null) {
			// Scroll containers are not interactive themselves, so walk the tree for one under the pointer.
			w = deepestAt(root, x, y);
		}
		for (Widget p = w; p != null; p = p.parent) {
			if (p.mouseScrolled(ctx, x, y, amount)) {
				return true;
			}
		}
		return false;
	}

	private Widget deepestAt(Widget w, float x, float y) {
		if (!w.visible || !w.bounds.contains(x, y)) {
			return null;
		}
		float cx = x + w.contentOffsetX();
		float cy = y + w.contentOffsetY();
		for (int i = w.children.size() - 1; i >= 0; i--) {
			Widget d = deepestAt(w.children.get(i), cx, cy);
			if (d != null) {
				return d;
			}
		}
		return w;
	}

	public boolean keyDown(int key, int mods) {
		if (ctx.focused != null && ctx.focused.keyDown(ctx, key, mods)) {
			return true;
		}
		if (key == Keys.TAB) {
			moveFocus(Keys.shift(mods) ? -1 : 1);
			return true;
		}
		if (key == Keys.DOWN || key == Keys.UP) {
			moveFocus(key == Keys.DOWN ? 1 : -1);
			return true;
		}
		if ((key == Keys.ENTER || key == Keys.KP_ENTER || key == Keys.SPACE) && ctx.focused != null) {
			ctx.focused.activate(ctx);
			return true;
		}
		return false;
	}

	public boolean charTyped(int codepoint) {
		return ctx.focused != null && ctx.focused.charTyped(ctx, codepoint);
	}

	public void moveFocus(int dir) {
		List<Widget> list = new ArrayList<>();
		root.collectFocusable(list);
		if (list.isEmpty()) {
			return;
		}
		int i = list.indexOf(ctx.focused);
		int next = i < 0 ? (dir > 0 ? 0 : list.size() - 1) : Math.floorMod(i + dir, list.size());
		setFocus(list.get(next));
		ctx.keyboardFocusVisible = true;
		ensureVisible(list.get(next));
	}

	private void ensureVisible(Widget w) {
		for (Widget p = w.parent; p != null; p = p.parent) {
			if (p instanceof ScrollPanel sp) {
				sp.reveal(w);
			}
		}
	}

	public void setFocus(Widget w) {
		if (ctx.focused == w) {
			return;
		}
		Widget old = ctx.focused;
		ctx.focused = w;
		if (old != null) {
			old.focusChanged(ctx, false);
		}
		if (w != null) {
			w.focusChanged(ctx, true);
		}
	}
}
