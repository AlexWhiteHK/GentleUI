package io.github.alexwhitehk.gentleui.core.ui;

import io.github.alexwhitehk.gentleui.core.render.Align;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import java.util.List;
import java.util.function.Supplier;

/** Static or dynamic text. Wraps to its width when {@link #wrap} is set. */
public class Label extends Widget {
	public enum Tone {
		NORMAL,
		MUTED,
		ACCENT,
		TITLE
	}

	private Supplier<String> text;
	public float scale = 1f;
	public Align align = Align.LEFT;
	public Tone tone = Tone.NORMAL;
	public boolean wrap;

	public Label(String text) {
		this.text = () -> text;
	}

	public Label(Supplier<String> text) {
		this.text = text;
	}

	public Label scale(float s) {
		this.scale = s;
		return this;
	}

	public Label tone(Tone t) {
		this.tone = t;
		return this;
	}

	public Label align(Align a) {
		this.align = a;
		return this;
	}

	public Label wrap() {
		this.wrap = true;
		return this;
	}

	public void setText(String t) {
		this.text = () -> t;
	}

	public String text() {
		return text.get();
	}

	/** Height needed for the text at the current width. */
	public float measureHeight(Canvas c) {
		float lh = (c.fontHeight() + 2f) * scale;
		if (!wrap) {
			return lh;
		}
		return WidgetRoot.wrap(c, text.get(), bounds.w / scale).size() * lh;
	}

	@Override
	protected void paint(UiContext ctx) {
		Canvas c = ctx.canvas;
		int color = switch (tone) {
			case MUTED -> ctx.palette().textMuted;
			case ACCENT -> ctx.palette().accent;
			default -> ctx.palette().text;
		};
		float ax = bounds.x + bounds.w * align.factor;
		if (!wrap) {
			float y = bounds.y + (bounds.h - c.fontHeight() * scale) * 0.5f;
			c.text(c.ellipsize(text.get(), bounds.w, scale), ax, y, color, scale, align, false);
			return;
		}
		List<String> lines = WidgetRoot.wrap(c, text.get(), bounds.w / scale);
		float lh = (c.fontHeight() + 2f) * scale;
		for (int i = 0; i < lines.size(); i++) {
			c.text(lines.get(i), ax, bounds.y + i * lh, color, scale, align, false);
		}
	}
}
