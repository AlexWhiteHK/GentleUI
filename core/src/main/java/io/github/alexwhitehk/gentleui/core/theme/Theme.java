package io.github.alexwhitehk.gentleui.core.theme;

import io.github.alexwhitehk.gentleui.core.host.SoundSpec;
import io.github.alexwhitehk.gentleui.core.host.UiSound;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.scene.UiScreen;

/** A complete visual language: background, control skin and its own title screen. */
public interface Theme {
	String id();

	/** Translation key of the display name. */
	String nameKey();

	String iconId();

	Skin skin();

	default Palette palette() {
		return skin().palette();
	}

	/** Draws the full-screen background. Called at most once per frame by whichever screen is showing. */
	void background(Canvas c, Backdrop b);

	/** Pointer pressed anywhere on a themed screen; backgrounds may respond (ripples, sparks...). */
	default void pointerPressed(float x, float y) {
	}

	UiScreen createTitleScreen();

	/** Sound for an interface event, or null for silence. */
	default SoundSpec sound(UiSound sound) {
		return switch (sound) {
			case CLICK -> SoundSpec.CLICK;
			case TOGGLE -> new SoundSpec("minecraft:ui.button.click", 1.25f, 0.18f);
			case OPEN -> new SoundSpec("minecraft:ui.button.click", 1.1f, 0.2f);
			case CLOSE -> new SoundSpec("minecraft:ui.button.click", 0.85f, 0.18f);
			default -> null;
		};
	}

	/**
	 * Progress indicator centered at (cx, cy). {@code progress} is 0..1, or negative when unknown.
	 */
	default void progress(Canvas c, float cx, float cy, float radius, float progress, float time) {
		int accent = palette().accent;
		int track = io.github.alexwhitehk.gentleui.core.color.Argb.withAlpha(palette().text, 0.12f);
		c.strokeCircle(cx, cy, radius, 3f, track);
		float start = -io.github.alexwhitehk.gentleui.core.math.MathUtil.PI * 0.5f;
		if (progress < 0f) {
			float a = time * 4f;
			float sweep = 1.2f + 0.8f * io.github.alexwhitehk.gentleui.core.math.MathUtil.sin(time * 2.2f);
			c.strokeArc(cx, cy, radius, a, a + sweep, 3f, accent, Canvas.Cap.ROUND);
		} else if (progress > 0.001f) {
			c.strokeArc(cx, cy, radius, start, start + io.github.alexwhitehk.gentleui.core.math.MathUtil.TAU * Math.min(1f, progress), 3f, accent, Canvas.Cap.ROUND);
		}
	}

	/** Miniature illustration of the theme for pickers, drawn inside {@code r}. */
	void preview(Canvas c, Rect r, float time);

	/** Re-reads theme specific configuration. */
	default void configChanged() {
	}
}
