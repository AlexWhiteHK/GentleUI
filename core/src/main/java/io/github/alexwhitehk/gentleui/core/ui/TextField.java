package io.github.alexwhitehk.gentleui.core.ui;

import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.host.Keys;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.render.Align;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import java.util.function.Consumer;

/** Single-line text input with selection, clipboard and horizontal scrolling. */
public class TextField extends Widget {
	private final StringBuilder text = new StringBuilder();
	private int cursor;
	private int anchor;
	private float scroll;
	private float blink;
	public String placeholder = "";
	public int maxLength = 256;
	public Consumer<String> onChange;
	public Runnable onSubmit;

	public TextField(String initial) {
		text.append(initial == null ? "" : initial);
		cursor = anchor = text.length();
		prefHeight = 20f;
	}

	public TextField placeholder(String p) {
		this.placeholder = p;
		return this;
	}

	public String value() {
		return text.toString();
	}

	public void setValue(String v) {
		text.setLength(0);
		text.append(v);
		cursor = anchor = text.length();
	}

	@Override
	public boolean interactive() {
		return true;
	}

	@Override
	public boolean focusable() {
		return true;
	}

	private int selStart() {
		return Math.min(cursor, anchor);
	}

	private int selEnd() {
		return Math.max(cursor, anchor);
	}

	private void changed() {
		blink = 0f;
		if (onChange != null) {
			onChange.accept(text.toString());
		}
	}

	private void insert(String s, UiContext ctx) {
		StringBuilder clean = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if (ch >= 32 && ch != 127) {
				clean.append(ch);
			}
		}
		int a = selStart();
		int b = selEnd();
		text.delete(a, b);
		int room = maxLength - text.length();
		String ins = clean.length() > room ? clean.substring(0, Math.max(0, room)) : clean.toString();
		text.insert(a, ins);
		cursor = anchor = a + ins.length();
		changed();
	}

	private int indexAt(UiContext ctx, float x) {
		Canvas c = ctx.canvas;
		float local = x - (screenX() + 6f) + scroll;
		int best = 0;
		float bestD = Float.MAX_VALUE;
		for (int i = 0; i <= text.length(); i++) {
			float w = c.textWidth(text.substring(0, i));
			float d = Math.abs(w - local);
			if (d < bestD) {
				bestD = d;
				best = i;
			}
		}
		return best;
	}

	@Override
	public boolean mouseDown(UiContext ctx, float x, float y, int button) {
		cursor = indexAt(ctx, x);
		anchor = cursor;
		blink = 0f;
		return true;
	}

	@Override
	public void mouseDragged(UiContext ctx, float x, float y, float dx, float dy) {
		cursor = indexAt(ctx, x);
	}

	@Override
	public boolean charTyped(UiContext ctx, int codepoint) {
		insert(new String(Character.toChars(codepoint)), ctx);
		return true;
	}

	private int wordLeft(int i) {
		while (i > 0 && text.charAt(i - 1) == ' ') {
			i--;
		}
		while (i > 0 && text.charAt(i - 1) != ' ') {
			i--;
		}
		return i;
	}

	private int wordRight(int i) {
		int n = text.length();
		while (i < n && text.charAt(i) != ' ') {
			i++;
		}
		while (i < n && text.charAt(i) == ' ') {
			i++;
		}
		return i;
	}

	@Override
	public boolean keyDown(UiContext ctx, int key, int mods) {
		boolean shift = Keys.shift(mods);
		boolean ctrl = Keys.ctrl(mods);
		switch (key) {
			case Keys.LEFT -> {
				cursor = ctrl ? wordLeft(cursor) : (!shift && cursor != anchor ? selStart() : Math.max(0, cursor - 1));
				if (!shift) {
					anchor = cursor;
				}
			}
			case Keys.RIGHT -> {
				cursor = ctrl ? wordRight(cursor) : (!shift && cursor != anchor ? selEnd() : Math.min(text.length(), cursor + 1));
				if (!shift) {
					anchor = cursor;
				}
			}
			case Keys.HOME -> {
				cursor = 0;
				if (!shift) {
					anchor = cursor;
				}
			}
			case Keys.END -> {
				cursor = text.length();
				if (!shift) {
					anchor = cursor;
				}
			}
			case Keys.BACKSPACE -> {
				if (cursor != anchor) {
					insert("", ctx);
				} else if (cursor > 0) {
					int from = ctrl ? wordLeft(cursor) : cursor - 1;
					text.delete(from, cursor);
					cursor = anchor = from;
					changed();
				}
			}
			case Keys.DELETE -> {
				if (cursor != anchor) {
					insert("", ctx);
				} else if (cursor < text.length()) {
					int to = ctrl ? wordRight(cursor) : cursor + 1;
					text.delete(cursor, to);
					changed();
				}
			}
			case Keys.ENTER, Keys.KP_ENTER -> {
				if (onSubmit != null) {
					onSubmit.run();
				}
			}
			default -> {
				if (ctrl && key == Keys.A) {
					anchor = 0;
					cursor = text.length();
				} else if (ctrl && key == Keys.C) {
					if (cursor != anchor) {
						ctx.host.setClipboard(text.substring(selStart(), selEnd()));
					}
				} else if (ctrl && key == Keys.X) {
					if (cursor != anchor) {
						ctx.host.setClipboard(text.substring(selStart(), selEnd()));
						insert("", ctx);
					}
				} else if (ctrl && key == Keys.V) {
					insert(ctx.host.clipboard(), ctx);
				} else {
					return false;
				}
			}
		}
		blink = 0f;
		return true;
	}

	@Override
	protected void paint(UiContext ctx) {
		Canvas c = ctx.canvas;
		boolean focused = isFocused(ctx);
		state.active = focused ? 1f : 0f;
		state.focus = focused ? 1f : state.focus;
		ctx.skin().textField(c, bounds, state);
		blink += ctx.dt;
		float innerW = bounds.w - 12f;
		float cursorX = c.textWidth(text.substring(0, cursor));
		if (cursorX - scroll > innerW) {
			scroll = cursorX - innerW;
		} else if (cursorX - scroll < 0f) {
			scroll = cursorX;
		}
		scroll = MathUtil.clamp(scroll, 0f, Math.max(0f, c.textWidth(text.toString()) - innerW));
		float tx = bounds.x + 6f - scroll;
		float ty = bounds.cy() - c.fontHeight() * 0.5f + 0.5f;
		c.pushClip(bounds.x + 4f, bounds.y, bounds.w - 8f, bounds.h);
		if (cursor != anchor && focused) {
			float a = c.textWidth(text.substring(0, selStart()));
			float b = c.textWidth(text.substring(0, selEnd()));
			c.fillRect(tx + a, ty - 1f, b - a, c.fontHeight() + 2f, Argb.withAlpha(ctx.palette().accent, 0.35f));
		}
		if (text.length() == 0 && !placeholder.isEmpty()) {
			c.text(placeholder, bounds.x + 6f, ty, ctx.palette().textDisabled, 1f, Align.LEFT, false);
		} else {
			c.text(text.toString(), tx, ty, ctx.palette().text, 1f, Align.LEFT, false);
		}
		if (focused && (blink % 1.0f) < 0.6f) {
			c.fillRect(tx + cursorX, ty - 1f, 1f, c.fontHeight() + 1f, ctx.palette().accent);
		}
		c.popClip();
	}
}
