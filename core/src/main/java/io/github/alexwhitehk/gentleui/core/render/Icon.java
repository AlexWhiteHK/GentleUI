package io.github.alexwhitehk.gentleui.core.render;

import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Line icon drawn from vector paths on a 24x24 grid. Icons are original geometric drawings made for GentleUI;
 * they scale to any size and inherit the caller's color and opacity.
 */
public final class Icon {
	private static final Map<String, Icon> REGISTRY = new LinkedHashMap<>();

	public final String id;
	private final Path stroke = new Path();
	private final Path fill = new Path();

	private Icon(String id, Consumer<Icon> builder) {
		this.id = id;
		builder.accept(this);
		REGISTRY.put(id, this);
	}

	private static Icon make(String id, Consumer<Icon> builder) {
		return new Icon(id, builder);
	}

	public static Icon byId(String id) {
		Icon icon = REGISTRY.get(id);
		return icon != null ? icon : DOT;
	}

	public static Map<String, Icon> all() {
		return Collections.unmodifiableMap(REGISTRY);
	}

	/**
	 * Draws the icon centered at (cx, cy).
	 *
	 * @param size   edge length of the icon box in GUI units
	 * @param weight stroke weight in grid units (2 is regular)
	 */
	public void draw(Canvas c, float cx, float cy, float size, int color, float weight) {
		float s = size / 24f;
		c.push();
		c.translate(cx - size * 0.5f, cy - size * 0.5f);
		c.scale(s);
		if (!fill.isEmpty()) {
			c.fillPath(fill, color);
		}
		if (!stroke.isEmpty()) {
			c.strokePath(stroke, weight, color, Canvas.Cap.ROUND);
		}
		c.pop();
	}

	public void draw(Canvas c, float cx, float cy, float size, int color) {
		draw(c, cx, cy, size, color, 2f);
	}

	// ----------------------------------------------------------------------------------------------------------
	// Builder helpers (grid units)
	// ----------------------------------------------------------------------------------------------------------

	private Path s() {
		return stroke;
	}

	private Path f() {
		return fill;
	}

	private void line(float x0, float y0, float x1, float y1) {
		stroke.moveTo(x0, y0).lineTo(x1, y1);
	}

	private void poly(float... pts) {
		stroke.moveTo(pts[0], pts[1]);
		for (int i = 2; i < pts.length; i += 2) {
			stroke.lineTo(pts[i], pts[i + 1]);
		}
	}

	private void closedPoly(float... pts) {
		poly(pts);
		stroke.close();
	}

	private void dot(float x, float y, float r) {
		fill.circle(x, y, r);
	}

	private void ring(float x, float y, float r) {
		stroke.circle(x, y, r);
	}

	private void arc(float cx, float cy, float r, float degFrom, float degTo) {
		int n = Math.max(6, MathUtil.ceilToInt(Math.abs(degTo - degFrom) / 8f));
		for (int i = 0; i <= n; i++) {
			float a = (float) Math.toRadians(degFrom + (degTo - degFrom) * i / n);
			float x = cx + MathUtil.cos(a) * r;
			float y = cy + MathUtil.sin(a) * r;
			if (i == 0) {
				stroke.moveTo(x, y);
			} else {
				stroke.lineTo(x, y);
			}
		}
	}

	private void ellipse(float cx, float cy, float rx, float ry) {
		int n = 40;
		for (int i = 0; i < n; i++) {
			float a = MathUtil.TAU * i / n;
			float x = cx + MathUtil.cos(a) * rx;
			float y = cy + MathUtil.sin(a) * ry;
			if (i == 0) {
				stroke.moveTo(x, y);
			} else {
				stroke.lineTo(x, y);
			}
		}
		stroke.close();
	}

	private void star(float cx, float cy, float outer, float inner, int points, boolean filled) {
		Path p = filled ? fill : stroke;
		for (int i = 0; i < points * 2; i++) {
			float r = i % 2 == 0 ? outer : inner;
			float a = -MathUtil.PI / 2f + MathUtil.PI * i / points;
			float x = cx + MathUtil.cos(a) * r;
			float y = cy + MathUtil.sin(a) * r;
			if (i == 0) {
				p.moveTo(x, y);
			} else {
				p.lineTo(x, y);
			}
		}
		p.close();
	}

	// ----------------------------------------------------------------------------------------------------------
	// Icon set
	// ----------------------------------------------------------------------------------------------------------

	public static final Icon DOT = make("dot", i -> i.dot(12, 12, 3));

	public static final Icon PLAY = make("play", i -> i.f().moveTo(8, 5).lineTo(19, 12).lineTo(8, 19).close());

	public static final Icon PERSON = make("person", i -> {
		i.ring(12, 8, 4);
		i.s().moveTo(5, 20).cubicTo(5, 16.1f, 8.1f, 13, 12, 13).cubicTo(15.9f, 13, 19, 16.1f, 19, 20);
	});

	public static final Icon PEOPLE = make("people", i -> {
		i.ring(9, 8, 3.5f);
		i.s().moveTo(2.5f, 20).cubicTo(2.5f, 16.4f, 5.4f, 13.5f, 9, 13.5f).cubicTo(12.6f, 13.5f, 15.5f, 16.4f, 15.5f, 20);
		i.arc(16.5f, 8.5f, 3, -80, 80);
		i.s().moveTo(17.5f, 14).cubicTo(19.8f, 14.6f, 21.5f, 16.8f, 21.5f, 20);
	});

	public static final Icon CUBE = make("cube", i -> {
		i.closedPoly(12, 2.5f, 20.5f, 7.25f, 20.5f, 16.75f, 12, 21.5f, 3.5f, 16.75f, 3.5f, 7.25f);
		i.poly(3.5f, 7.25f, 12, 12, 20.5f, 7.25f);
		i.line(12, 12, 12, 21.5f);
	});

	public static final Icon CLOUD = make("cloud", i -> {
		i.s().arc(7, 14.5f, 3.5f, MathUtil.PI / 2f, MathUtil.PI * 1.5f);
		i.s().lineTo(7, 11);
		appendArc(i.s(), 12, 11, 5, MathUtil.PI, MathUtil.TAU);
		appendArc(i.s(), 17, 14.5f, 3.5f, MathUtil.PI * 1.5f, MathUtil.PI * 2.5f);
		i.s().close();
	});

	public static final Icon GEAR = make("gear", i -> {
		i.ring(12, 12, 3);
		Path p = i.s();
		int teeth = 8;
		for (int k = 0; k < teeth; k++) {
			float base = MathUtil.TAU * k / teeth;
			float[] angles = {-0.40f, -0.20f, 0.20f, 0.40f};
			float[] radii = {7.2f, 9.6f, 9.6f, 7.2f};
			for (int j = 0; j < 4; j++) {
				float a = base + angles[j];
				float x = 12 + MathUtil.cos(a) * radii[j];
				float y = 12 + MathUtil.sin(a) * radii[j];
				if (k == 0 && j == 0) {
					p.moveTo(x, y);
				} else {
					p.lineTo(x, y);
				}
			}
		}
		p.close();
	});

	public static final Icon MONITOR = make("monitor", i -> {
		i.s().roundRect(3, 4, 18, 12, 2);
		i.line(12, 16, 12, 20);
		i.line(8, 20, 16, 20);
	});

	public static final Icon SPEAKER = make("speaker", i -> {
		i.closedPoly(4, 9, 8, 9, 13, 5, 13, 19, 8, 15, 4, 15);
		i.arc(14, 12, 3.5f, -50, 50);
		i.arc(14, 12, 7, -55, 55);
	});

	public static final Icon KEYBOARD = make("keyboard", i -> {
		i.s().roundRect(2.5f, 6, 19, 12, 2.5f);
		i.dot(6.5f, 10, 1.05f);
		i.dot(10.2f, 10, 1.05f);
		i.dot(13.8f, 10, 1.05f);
		i.dot(17.5f, 10, 1.05f);
		i.line(8, 14, 16, 14);
	});

	public static final Icon GLOBE = make("globe", i -> {
		i.ring(12, 12, 9);
		i.ellipse(12, 12, 4, 9);
		i.line(3, 12, 21, 12);
	});

	public static final Icon LAYERS = make("layers", i -> {
		i.closedPoly(12, 3, 21, 8, 12, 13, 3, 8);
		i.poly(3, 12.5f, 12, 17.5f, 21, 12.5f);
		i.poly(3, 16.5f, 12, 21.5f, 21, 16.5f);
	});

	public static final Icon ACCESSIBILITY = make("accessibility", i -> {
		i.dot(12, 4.5f, 2);
		i.poly(5, 8, 12, 9.5f, 19, 8);
		i.line(12, 9.5f, 12, 14);
		i.poly(8.5f, 21, 12, 14, 15.5f, 21);
	});

	public static final Icon SHIRT = make("shirt", i -> {
		Path p = i.s();
		p.moveTo(8, 3.5f).lineTo(3.5f, 6.5f).lineTo(5.5f, 10.5f).lineTo(7.5f, 9.8f).lineTo(7.5f, 20.5f).lineTo(16.5f, 20.5f)
			.lineTo(16.5f, 9.8f).lineTo(18.5f, 10.5f).lineTo(20.5f, 6.5f).lineTo(16, 3.5f);
		p.cubicTo(15, 5.5f, 9, 5.5f, 8, 3.5f).close();
	});

	public static final Icon CHAT = make("chat", i -> {
		Path p = i.s();
		p.moveTo(6, 4).lineTo(18, 4).quadTo(21, 4, 21, 7).lineTo(21, 13).quadTo(21, 16, 18, 16).lineTo(11, 16)
			.lineTo(6.5f, 20).lineTo(7, 16).lineTo(6, 16).quadTo(3, 16, 3, 13).lineTo(3, 7).quadTo(3, 4, 6, 4).close();
	});

	public static final Icon WIFI = make("wifi", i -> {
		i.arc(12, 19, 4.5f, -135, -45);
		i.arc(12, 19, 9, -135, -45);
		i.arc(12, 19, 13.5f, -132, -48);
		i.dot(12, 19, 1.4f);
	});

	public static final Icon INFO = make("info", i -> {
		i.ring(12, 12, 9);
		i.line(12, 11, 12, 16.5f);
		i.dot(12, 7.6f, 1.3f);
	});

	public static final Icon POWER = make("power", i -> {
		i.arc(12, 13, 8, -55, 235);
		i.line(12, 3, 12, 11.5f);
	});

	public static final Icon PALETTE = make("palette", i -> {
		Path p = i.s();
		p.moveTo(12, 3).cubicTo(7, 3, 3, 7, 3, 12).cubicTo(3, 17, 7, 21, 12, 21).cubicTo(13.4f, 21, 14, 20.2f, 14, 19.2f)
			.cubicTo(14, 18.2f, 13, 17.7f, 13, 16.6f).cubicTo(13, 15.6f, 14, 15, 15, 15).lineTo(17, 15)
			.cubicTo(19.5f, 15, 21, 13.5f, 21, 11.5f).cubicTo(21, 7, 17, 3, 12, 3).close();
		i.dot(7.5f, 11.5f, 1.35f);
		i.dot(9.5f, 7.5f, 1.35f);
		i.dot(14.5f, 7.3f, 1.35f);
		i.dot(17.2f, 10.8f, 1.35f);
	});

	public static final Icon BRUSH = make("brush", i -> {
		i.line(19.5f, 4.5f, 11.5f, 12.5f);
		i.s().moveTo(11.5f, 12.5f).cubicTo(8.5f, 12, 7, 14, 7, 16).cubicTo(7, 18, 5.5f, 19.2f, 4, 20)
			.cubicTo(8, 20.8f, 13.2f, 19.6f, 12.9f, 15).close();
	});

	public static final Icon MOVE = make("move", i -> {
		i.line(12, 3, 12, 21);
		i.line(3, 12, 21, 12);
		i.poly(9, 6, 12, 3, 15, 6);
		i.poly(9, 18, 12, 21, 15, 18);
		i.poly(6, 9, 3, 12, 6, 15);
		i.poly(18, 9, 21, 12, 18, 15);
	});

	public static final Icon SLIDERS = make("sliders", i -> {
		i.line(4, 7, 7, 7);
		i.line(11, 7, 20, 7);
		i.ring(9, 7, 2);
		i.line(4, 12, 13, 12);
		i.line(17, 12, 20, 12);
		i.ring(15, 12, 2);
		i.line(4, 17, 5, 17);
		i.line(9, 17, 20, 17);
		i.ring(7, 17, 2);
	});

	public static final Icon PUZZLE = make("puzzle", i -> {
		Path p = i.s();
		p.moveTo(5, 8).lineTo(9, 8).cubicTo(9, 6.3f, 10.1f, 5, 11.5f, 5).cubicTo(12.9f, 5, 14, 6.3f, 14, 8).lineTo(18, 8)
			.lineTo(18, 11.5f).cubicTo(19.7f, 11.5f, 21, 12.6f, 21, 14).cubicTo(21, 15.4f, 19.7f, 16.5f, 18, 16.5f)
			.lineTo(18, 20).lineTo(5, 20).close();
	});

	public static final Icon LIST = make("list", i -> {
		i.line(9, 6, 20, 6);
		i.line(9, 12, 20, 12);
		i.line(9, 18, 20, 18);
		i.dot(4.5f, 6, 1.4f);
		i.dot(4.5f, 12, 1.4f);
		i.dot(4.5f, 18, 1.4f);
	});

	public static final Icon BACK = make("back", i -> {
		i.line(19, 12, 5, 12);
		i.poly(11, 6, 5, 12, 11, 18);
	});

	public static final Icon FORWARD = make("forward", i -> {
		i.line(5, 12, 19, 12);
		i.poly(13, 6, 19, 12, 13, 18);
	});

	public static final Icon CHEVRON_RIGHT = make("chevron_right", i -> i.poly(9, 5, 16, 12, 9, 19));

	public static final Icon CHEVRON_DOWN = make("chevron_down", i -> i.poly(5, 9, 12, 16, 19, 9));

	public static final Icon CLOSE = make("close", i -> {
		i.line(6, 6, 18, 18);
		i.line(18, 6, 6, 18);
	});

	public static final Icon CHECK = make("check", i -> i.poly(5, 12.5f, 10, 17.5f, 19, 7));

	public static final Icon PLUS = make("plus", i -> {
		i.line(12, 5, 12, 19);
		i.line(5, 12, 19, 12);
	});

	public static final Icon TRASH = make("trash", i -> {
		i.line(4, 7, 20, 7);
		i.poly(6, 7, 7, 20, 17, 20, 18, 7);
		i.poly(9, 7, 9, 4.5f, 15, 4.5f, 15, 7);
		i.line(10, 11, 10, 16);
		i.line(14, 11, 14, 16);
	});

	public static final Icon REFRESH = make("refresh", i -> {
		i.arc(12, 12, 8, 20, 300);
		i.poly(20.5f, 3.5f, 20.5f, 8.5f, 15.5f, 8.5f);
	});

	public static final Icon SEARCH = make("search", i -> {
		i.ring(10.5f, 10.5f, 6.5f);
		i.line(15.5f, 15.5f, 20, 20);
	});

	public static final Icon FOLDER = make("folder", i -> {
		Path p = i.s();
		p.moveTo(3, 7).quadTo(3, 5, 5, 5).lineTo(9.5f, 5).lineTo(11.5f, 7).lineTo(19, 7).quadTo(21, 7, 21, 9).lineTo(21, 17)
			.quadTo(21, 19, 19, 19).lineTo(5, 19).quadTo(3, 19, 3, 17).close();
	});

	public static final Icon EDIT = make("edit", i -> {
		i.closedPoly(4, 20, 5, 15.5f, 15.5f, 5, 19, 8.5f, 8.5f, 19);
		i.line(13.5f, 7, 17, 10.5f);
	});

	public static final Icon STAR = make("star", i -> i.star(12, 12.5f, 9.5f, 4.2f, 5, false));

	public static final Icon SERVER = make("server", i -> {
		i.s().roundRect(3, 4, 18, 7, 2);
		i.s().roundRect(3, 13, 18, 7, 2);
		i.dot(7, 7.5f, 1.2f);
		i.dot(7, 16.5f, 1.2f);
	});

	public static final Icon IMAGE = make("image", i -> {
		i.s().roundRect(3, 4, 18, 16, 2.5f);
		i.ring(8.5f, 9.5f, 1.8f);
		i.poly(3.5f, 17, 9, 12, 13, 15.5f, 16, 13, 20.5f, 17);
	});

	public static final Icon TEXT = make("text", i -> {
		i.line(5, 5, 19, 5);
		i.line(12, 5, 12, 19);
		i.line(9, 19, 15, 19);
	});

	public static final Icon CLOCK = make("clock", i -> {
		i.ring(12, 12, 9);
		i.poly(12, 7, 12, 12, 15.5f, 14);
	});

	public static final Icon EXTERNAL = make("external", i -> {
		i.poly(14, 4, 20, 4, 20, 10);
		i.line(20, 4, 11, 13);
		i.s().moveTo(18, 14).lineTo(18, 18).quadTo(18, 20, 16, 20).lineTo(6, 20).quadTo(4, 20, 4, 18).lineTo(4, 8)
			.quadTo(4, 6, 6, 6).lineTo(10, 6);
	});

	public static final Icon DROP = make("drop", i -> i.s().moveTo(12, 3).cubicTo(12, 3, 5.5f, 10, 5.5f, 14.5f)
		.cubicTo(5.5f, 18.1f, 8.4f, 21, 12, 21).cubicTo(15.6f, 21, 18.5f, 18.1f, 18.5f, 14.5f)
		.cubicTo(18.5f, 10, 12, 3, 12, 3).close());

	public static final Icon SPARKLE = make("sparkle", i -> i.s().moveTo(12, 3).cubicTo(12.5f, 8, 16, 11.5f, 21, 12)
		.cubicTo(16, 12.5f, 12.5f, 16, 12, 21).cubicTo(11.5f, 16, 8, 12.5f, 3, 12).cubicTo(8, 11.5f, 11.5f, 8, 12, 3).close());

	public static final Icon GLASS = make("glass", i -> {
		i.s().roundRect(4, 4, 16, 16, 4.5f);
		i.line(8, 11, 11, 8);
		i.line(8, 15, 15, 8);
	});

	public static final Icon HOME = make("home", i -> {
		i.s().moveTo(4, 11).lineTo(12, 4).lineTo(20, 11).lineTo(20, 19).quadTo(20, 20, 19, 20).lineTo(5, 20).quadTo(4, 20, 4, 19).close();
		i.poly(10, 20, 10, 15, 14, 15, 14, 20);
	});

	public static final Icon LOGOUT = make("logout", i -> {
		i.s().moveTo(9, 4).lineTo(6, 4).quadTo(4, 4, 4, 6).lineTo(4, 18).quadTo(4, 20, 6, 20).lineTo(9, 20);
		i.line(10, 12, 20, 12);
		i.poly(16, 8, 20, 12, 16, 16);
	});

	public static final Icon FLAG = make("flag", i -> {
		i.line(5, 21, 5, 4);
		i.s().moveTo(5, 4).cubicTo(9, 2.5f, 11, 5.5f, 15, 4).lineTo(19, 4).lineTo(19, 13).cubicTo(15, 14.5f, 13, 11.5f, 9, 13)
			.lineTo(5, 13);
	});

	public static final Icon CHART = make("chart", i -> {
		i.line(4, 20, 20, 20);
		i.line(7.5f, 17, 7.5f, 12);
		i.line(12, 17, 12, 7);
		i.line(16.5f, 17, 16.5f, 10);
	});

	public static final Icon SHARE = make("share", i -> {
		i.ring(6, 12, 2.6f);
		i.ring(18, 6, 2.6f);
		i.ring(18, 18, 2.6f);
		i.line(8.3f, 10.8f, 15.7f, 7.2f);
		i.line(8.3f, 13.2f, 15.7f, 16.8f);
	});

	public static final Icon BUG = make("bug", i -> {
		i.s().roundRect(7.5f, 8, 9, 12.5f, 4.5f);
		i.line(12, 11, 12, 20);
		i.line(3.5f, 11, 7.5f, 12);
		i.line(20.5f, 11, 16.5f, 12);
		i.line(3.5f, 15.5f, 7.5f, 15.5f);
		i.line(20.5f, 15.5f, 16.5f, 15.5f);
		i.line(4.5f, 20, 8, 18.5f);
		i.line(19.5f, 20, 16, 18.5f);
		i.line(10, 8, 9, 4.5f);
		i.line(14, 8, 15, 4.5f);
	});

	public static final Icon EYE = make("eye", i -> {
		i.s().moveTo(2, 12).cubicTo(5, 6.5f, 8.5f, 5, 12, 5).cubicTo(15.5f, 5, 19, 6.5f, 22, 12).cubicTo(19, 17.5f, 15.5f, 19, 12, 19)
			.cubicTo(8.5f, 19, 5, 17.5f, 2, 12).close();
		i.ring(12, 12, 3);
	});

	public static final Icon EYE_OFF = make("eye_off", i -> {
		i.s().moveTo(2, 12).cubicTo(5, 6.5f, 8.5f, 5, 12, 5).cubicTo(15.5f, 5, 19, 6.5f, 22, 12).cubicTo(19, 17.5f, 15.5f, 19, 12, 19)
			.cubicTo(8.5f, 19, 5, 17.5f, 2, 12).close();
		i.line(4, 4, 20, 20);
	});

	public static final Icon BOOK = make("book", i -> {
		i.s().moveTo(4, 19).lineTo(4, 5).quadTo(4, 3, 6, 3).lineTo(20, 3).lineTo(20, 17).lineTo(6, 17).quadTo(4, 17, 4, 19)
			.quadTo(4, 21, 6, 21).lineTo(20, 21).lineTo(20, 17);
	});

	public static final Icon GRID = make("grid", i -> {
		i.s().roundRect(4, 4, 7, 7, 1.8f);
		i.s().roundRect(13, 4, 7, 7, 1.8f);
		i.s().roundRect(4, 13, 7, 7, 1.8f);
		i.s().roundRect(13, 13, 7, 7, 1.8f);
	});

	public static final Icon MORE = make("more", i -> {
		i.dot(5.5f, 12, 1.7f);
		i.dot(12, 12, 1.7f);
		i.dot(18.5f, 12, 1.7f);
	});

	public static final Icon RESET = make("reset", i -> {
		i.arc(12, 12, 8, 160, 440);
		i.poly(3.5f, 5.5f, 4.2f, 10.2f, 8.8f, 9.2f);
	});

	public static final Icon SUN = make("sun", i -> {
		i.ring(12, 12, 4);
		for (int k = 0; k < 8; k++) {
			float a = MathUtil.TAU * k / 8;
			i.line(12 + MathUtil.cos(a) * 7, 12 + MathUtil.sin(a) * 7, 12 + MathUtil.cos(a) * 9.5f, 12 + MathUtil.sin(a) * 9.5f);
		}
	});

	public static final Icon PAUSE = make("pause", i -> {
		i.line(9, 6, 9, 18);
		i.line(15, 6, 15, 18);
	});

	private static void appendArc(Path p, float cx, float cy, float r, float a0, float a1) {
		int n = Math.max(6, MathUtil.ceilToInt(Math.abs(a1 - a0) / MathUtil.TAU * 40f));
		for (int k = 1; k <= n; k++) {
			float a = a0 + (a1 - a0) * k / n;
			p.lineTo(cx + MathUtil.cos(a) * r, cy + MathUtil.sin(a) * r);
		}
	}
}
