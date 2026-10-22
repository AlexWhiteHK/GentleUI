package io.github.alexwhitehk.gentleui.core.ui;

import io.github.alexwhitehk.gentleui.core.anim.Spring;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.theme.WidgetState;
import java.util.ArrayList;
import java.util.List;

/**
 * Base of GentleUI's retained widget tree. Bounds are expressed in the parent's content space; containers that
 * scroll shift their children via {@link #contentOffsetX()} / {@link #contentOffsetY()}.
 */
public class Widget {
	private static int seeds;

	public final Rect bounds = new Rect();
	protected final List<Widget> children = new ArrayList<>();
	protected Widget parent;
	public boolean visible = true;
	public boolean enabled = true;
	public String tooltip;
	/** Screen time (seconds) at which this widget starts its entrance animation; negative disables it. */
	public float enterDelay = -1f;
	/** Preferred height used by layout containers; 0 means "use the current bounds". */
	public float prefHeight;
	public float prefWidth;

	protected final Spring hoverAnim = new Spring(0f, 300f, 1f);
	protected final Spring pressAnim = new Spring(0f, 520f, 1f);
	protected final Spring focusAnim = new Spring(0f, 300f, 1f);
	protected final Spring appear = new Spring(0f, 120f, 0.85f);
	protected final WidgetState state = new WidgetState();
	private float rippleAge = -1f;
	protected final float seed = MathUtil.hash01(++seeds);

	public Widget() {
		appear.target(1f);
	}

	public Widget bounds(float x, float y, float w, float h) {
		bounds.set(x, y, w, h);
		return this;
	}

	public <T extends Widget> T add(T child) {
		child.parent = this;
		children.add(child);
		return child;
	}

	public void remove(Widget child) {
		if (children.remove(child)) {
			child.parent = null;
		}
	}

	public void clear() {
		for (Widget c : children) {
			c.parent = null;
		}
		children.clear();
	}

	public List<Widget> children() {
		return children;
	}

	public Widget parent() {
		return parent;
	}

	/** Position children inside {@link #bounds}. */
	public void layout() {
		for (Widget c : children) {
			c.layout();
		}
	}

	public float contentOffsetX() {
		return 0f;
	}

	public float contentOffsetY() {
		return 0f;
	}

	/** Whether this widget itself reacts to the pointer (containers usually do not). */
	public boolean interactive() {
		return false;
	}

	public boolean focusable() {
		return false;
	}

	public boolean isHovered(UiContext ctx) {
		return ctx.hovered == this;
	}

	public boolean isFocused(UiContext ctx) {
		return ctx.focused == this;
	}

	public boolean isPressed(UiContext ctx) {
		return ctx.captured == this && ctx.pointerDown;
	}

	/** Absolute (screen) position of this widget's top-left corner. */
	public float screenX() {
		float x = bounds.x;
		for (Widget p = parent; p != null; p = p.parent) {
			x += -p.contentOffsetX();
		}
		return x;
	}

	public float screenY() {
		float y = bounds.y;
		for (Widget p = parent; p != null; p = p.parent) {
			y += -p.contentOffsetY();
		}
		return y;
	}

	/** Finds the deepest interactive widget under the point given in this widget's parent content space. */
	public Widget hit(float x, float y) {
		if (!visible) {
			return null;
		}
		boolean inside = bounds.contains(x, y);
		if (!inside && clipsChildren()) {
			return null;
		}
		float cx = x + contentOffsetX();
		float cy = y + contentOffsetY();
		for (int i = children.size() - 1; i >= 0; i--) {
			Widget h = children.get(i).hit(cx, cy);
			if (h != null) {
				return h;
			}
		}
		return inside && interactive() ? this : null;
	}

	protected boolean clipsChildren() {
		return false;
	}

	/** Updates animation state; called every frame before painting. */
	protected void update(UiContext ctx) {
		boolean on = enabled;
		hoverAnim.target(on && isHovered(ctx) ? 1f : 0f).update(ctx.dt);
		pressAnim.target(on && isPressed(ctx) && isHovered(ctx) ? 1f : 0f).update(ctx.dt);
		focusAnim.target(isFocused(ctx) && ctx.keyboardFocusVisible ? 1f : 0f).update(ctx.dt);
		appear.update(ctx.dt);
		state.hover = MathUtil.clamp01(hoverAnim.value());
		state.press = MathUtil.clamp01(pressAnim.value());
		state.focus = MathUtil.clamp01(focusAnim.value());
		state.enabled = on;
		state.time = ctx.time;
		state.seed = seed;
		if (rippleAge >= 0f) {
			rippleAge += ctx.dt;
			state.ripple = Math.min(1f, rippleAge / 0.55f);
			if (rippleAge > 0.75f) {
				rippleAge = -1f;
				state.ripple = 0f;
			}
		}
	}

	/** Starts a press ripple at a screen-space point. */
	protected void startRipple(float sx, float sy) {
		rippleAge = 0f;
		state.rippleX = sx - screenX();
		state.rippleY = sy - screenY();
		state.ripple = 0.001f;
	}

	public void render(UiContext ctx) {
		if (!visible) {
			return;
		}
		float enter = 1f;
		if (enterDelay >= 0f) {
			enter = io.github.alexwhitehk.gentleui.core.math.Easing.EMPHASIZED.apply(MathUtil.clamp01((ctx.time - enterDelay) / 0.45f));
			if (enter <= 0.001f) {
				update(ctx);
				return;
			}
			ctx.canvas.pushAlpha(enter);
			ctx.canvas.push();
			ctx.canvas.translate(0f, (1f - enter) * 10f);
		}
		update(ctx);
		paint(ctx);
		renderChildren(ctx);
		paintOverlay(ctx);
		if (enterDelay >= 0f) {
			ctx.canvas.pop();
			ctx.canvas.popAlpha();
		}
	}

	protected void renderChildren(UiContext ctx) {
		if (children.isEmpty()) {
			return;
		}
		Canvas c = ctx.canvas;
		boolean clip = clipsChildren();
		if (clip) {
			c.pushClip(bounds.x, bounds.y, bounds.w, bounds.h);
		}
		c.push();
		c.translate(-contentOffsetX(), -contentOffsetY());
		for (Widget child : children) {
			child.render(ctx);
		}
		c.pop();
		if (clip) {
			c.popClip();
		}
	}

	protected void paint(UiContext ctx) {
	}

	protected void paintOverlay(UiContext ctx) {
	}

	// Input. Coordinates are in screen space; widgets convert with screenX()/screenY() when needed.

	public boolean mouseDown(UiContext ctx, float x, float y, int button) {
		return false;
	}

	public void mouseUp(UiContext ctx, float x, float y, int button, boolean inside) {
	}

	public void mouseDragged(UiContext ctx, float x, float y, float dx, float dy) {
	}

	public boolean mouseScrolled(UiContext ctx, float x, float y, float amount) {
		return false;
	}

	public boolean keyDown(UiContext ctx, int key, int mods) {
		return false;
	}

	public boolean charTyped(UiContext ctx, int codepoint) {
		return false;
	}

	public void focusChanged(UiContext ctx, boolean focused) {
	}

	/** Collects focusable widgets in tree order for keyboard navigation. */
	public void collectFocusable(List<Widget> out) {
		if (!visible) {
			return;
		}
		if (focusable() && enabled) {
			out.add(this);
		}
		for (Widget c : children) {
			c.collectFocusable(out);
		}
	}

	/** Activates the widget from the keyboard (Enter/Space). */
	public void activate(UiContext ctx) {
	}
}
