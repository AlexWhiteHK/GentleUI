package io.github.alexwhitehk.gentleui.core.scene;

import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.render.Align;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.theme.Theme;

/** Centered loading state used by themed loading screens. */
public final class LoadingView {
	private LoadingView() {
	}

	/**
	 * @param progress 0..1, or negative when the amount of work is unknown
	 */
	public static void draw(Canvas c, Theme theme, float width, float height, String title, float progress, float time) {
		float cx = width * 0.5f;
		float cy = height * 0.5f - 6f;
		float r = Math.min(22f, Math.min(width, height) * 0.09f);
		theme.progress(c, cx, cy, r, progress, time);
		int text = theme.palette().text;
		c.text(title, cx, cy + r + 14f, text, 1f, Align.CENTER, false);
		if (progress >= 0f) {
			c.text(Math.round(progress * 100f) + "%", cx, cy + r + 27f, Argb.withAlpha(theme.palette().textMuted, 0.9f), 1f, Align.CENTER, false);
		}
	}
}
