package io.github.alexwhitehk.gentleui.core.ui;

import io.github.alexwhitehk.gentleui.core.host.Host;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.theme.Palette;
import io.github.alexwhitehk.gentleui.core.theme.Skin;
import io.github.alexwhitehk.gentleui.core.theme.Theme;

/** Shared per-frame state for a widget tree. */
public final class UiContext {
	public Canvas canvas;
	public Theme theme;
	public Host host;
	public float dt;
	public float time;
	public float mouseX;
	public float mouseY;
	public boolean pointerDown;
	public Widget hovered;
	public Widget focused;
	/** Widget that received the current press and therefore gets drag and release events. */
	public Widget captured;
	/** Focus rings are only shown after keyboard navigation, like :focus-visible on the web. */
	public boolean keyboardFocusVisible;
	public String tooltip;

	public Skin skin() {
		return theme.skin();
	}

	public Palette palette() {
		return theme.skin().palette();
	}

	public String tr(String key, Object... args) {
		return host.tr(key, args);
	}
}
