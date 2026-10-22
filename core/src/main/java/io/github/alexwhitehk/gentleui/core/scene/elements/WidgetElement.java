package io.github.alexwhitehk.gentleui.core.scene.elements;

import io.github.alexwhitehk.gentleui.core.config.LayoutConfig;
import io.github.alexwhitehk.gentleui.core.host.Host;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.theme.Theme;
import io.github.alexwhitehk.gentleui.core.ui.Widget;

/**
 * Makes a group of widgets (a title, a button card, a dock...) movable in the layout editor. The widget draws
 * itself; this element only maps the user's placement onto it.
 */
public final class WidgetElement extends TitleElement {
	private final Widget widget;
	private final String nameKey;

	public WidgetElement(String id, String nameKey, Widget widget) {
		super(id);
		this.widget = widget;
		this.nameKey = nameKey;
	}

	@Override
	public String displayName(Host host) {
		return host.tr(nameKey);
	}

	@Override
	public float width(Canvas c) {
		return widget.bounds.w;
	}

	@Override
	public float height(Canvas c) {
		return widget.bounds.h;
	}

	@Override
	public void defaultCenter(Canvas c, float screenW, float screenH, float[] out) {
		out[0] = widget.bounds.cx();
		out[1] = widget.bounds.cy();
	}

	@Override
	public float place(Canvas c, float screenW, float screenH, LayoutConfig.ElementOverride o, float[] tmp) {
		float scale = super.place(c, screenW, screenH, o, tmp);
		widget.userDx = o == null ? 0f : o.dx;
		widget.userDy = o == null ? 0f : o.dy;
		widget.userScale = o == null ? 1f : o.scale;
		widget.userOpacity = o == null ? 1f : o.opacity;
		widget.visible = scale > 0f || editingHint;
		return scale;
	}

	/** Set by the screen while editing so hidden groups stay visible (ghosted) and selectable. */
	public boolean editingHint;

	@Override
	public void draw(Canvas c, Theme theme, Host host, float cx, float cy, float scale, float time) {
		// The widget renders itself as part of the widget tree.
	}

	public boolean drawsItself() {
		return true;
	}
}
