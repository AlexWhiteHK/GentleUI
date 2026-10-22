package io.github.alexwhitehk.gentleui.core.theme;

import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Canvas;

/**
 * Paints individual controls. Both GentleUI's own widgets and the restyled vanilla widgets go through the same
 * skin, which is what keeps every menu visually consistent.
 */
public interface Skin {
	Palette palette();

	/** Corner radius for regular controls of the given height. */
	float radius(float height);

	void button(Canvas c, Rect r, ButtonStyle style, WidgetState s);

	/** Round icon-only button centered in {@code r}. */
	void iconButton(Canvas c, Rect r, ButtonStyle style, WidgetState s);

	/** Text color to use on top of {@link #button} for the given style and state. */
	int buttonText(ButtonStyle style, WidgetState s);

	/** Glyph color for {@link #iconButton}. */
	default int iconColor(ButtonStyle style, WidgetState s) {
		return buttonText(style, s);
	}

	void toggle(Canvas c, Rect r, WidgetState s);

	void checkbox(Canvas c, Rect r, WidgetState s);

	/** Slider made of a track with value fill and a knob. {@code value} is 0..1. */
	void slider(Canvas c, Rect r, float value, WidgetState s);

	/**
	 * Container used by vanilla sliders, whose label is drawn centered on top. {@code value} is 0..1, or negative
	 * when only the track is known.
	 */
	void sliderContainer(Canvas c, Rect r, float value, WidgetState s);

	void textField(Canvas c, Rect r, WidgetState s);

	void tab(Canvas c, Rect r, WidgetState s);

	void scrollbar(Canvas c, Rect track, Rect thumb, WidgetState s);

	void panel(Canvas c, Rect r, PanelStyle style);

	/** Highlight for a selected or hovered list row; {@code strength} 0..1. */
	void selection(Canvas c, Rect r, float strength, boolean focused);

	void separator(Canvas c, float x, float y, float w);

	void tooltip(Canvas c, Rect r);

	void focusRing(Canvas c, Rect r, float radius, float amount);
}
