package io.github.alexwhitehk.gentleui.core.scene.elements;

import io.github.alexwhitehk.gentleui.core.config.LayoutConfig;
import io.github.alexwhitehk.gentleui.core.host.Host;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.theme.Theme;

/**
 * Something placed on a title screen that the layout editor can move, scale or hide. Built-in elements
 * come from the theme; custom ones from {@link io.github.alexwhitehk.gentleui.core.config.CustomElement}.
 */
public abstract class TitleElement {
	public final String id;
	/** Last placed bounds (screen space), refreshed every frame. */
	public final Rect bounds = new Rect();
	public float hover;

	protected TitleElement(String id) {
		this.id = id;
	}

	public abstract String displayName(Host host);

	/** Unscaled size. */
	public abstract float width(Canvas c);

	public abstract float height(Canvas c);

	/** Center position without user adjustments. */
	public abstract void defaultCenter(Canvas c, float screenW, float screenH, float[] out);

	/** Draws the element centered at (cx, cy). */
	public abstract void draw(Canvas c, Theme theme, Host host, float cx, float cy, float scale, float time);

	public boolean clickable() {
		return false;
	}

	public void click(Host host) {
	}

	/** Custom elements can be deleted from the editor; built-in ones can only be hidden. */
	public boolean custom() {
		return false;
	}

	/** Applies placement for this frame and returns the effective scale (0 when hidden). */
	public float place(Canvas c, float screenW, float screenH, LayoutConfig.ElementOverride o, float[] tmp) {
		defaultCenter(c, screenW, screenH, tmp);
		float scale = o == null ? 1f : o.scale;
		float cx = tmp[0] + (o == null ? 0f : o.dx);
		float cy = tmp[1] + (o == null ? 0f : o.dy);
		float w = width(c) * scale;
		float h = height(c) * scale;
		bounds.set(cx - w * 0.5f, cy - h * 0.5f, w, h);
		return o != null && o.hidden ? 0f : scale;
	}
}
