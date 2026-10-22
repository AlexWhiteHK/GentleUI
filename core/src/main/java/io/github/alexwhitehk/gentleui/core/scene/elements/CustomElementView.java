package io.github.alexwhitehk.gentleui.core.scene.elements;

import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.config.CustomElement;
import io.github.alexwhitehk.gentleui.core.config.LayoutConfig;
import io.github.alexwhitehk.gentleui.core.host.Host;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Align;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.render.Icon;
import io.github.alexwhitehk.gentleui.core.render.ImageRef;
import io.github.alexwhitehk.gentleui.core.theme.ButtonStyle;
import io.github.alexwhitehk.gentleui.core.theme.Theme;
import io.github.alexwhitehk.gentleui.core.theme.WidgetState;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Renders a user-defined {@link CustomElement}. */
public final class CustomElementView extends TitleElement {
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, d MMM");

	public final CustomElement model;
	private final WidgetState state = new WidgetState();
	private final Rect rect = new Rect();
	private ImageRef image;
	private String imageSource;

	public CustomElementView(CustomElement model) {
		super(model.id);
		this.model = model;
	}

	@Override
	public boolean custom() {
		return true;
	}

	@Override
	public String displayName(Host host) {
		String kind = host.tr("gentleui.element.type." + model.type);
		if (model.text != null && !model.text.isEmpty() && !CustomElement.IMAGE.equals(model.type)) {
			String t = model.text.length() > 14 ? model.text.substring(0, 14) + "..." : model.text;
			return kind + " · " + t;
		}
		return kind;
	}

	private String clockText() {
		return LocalDateTime.now().format(TIME);
	}

	@Override
	public float width(Canvas c) {
		return switch (model.type) {
			case CustomElement.TEXT -> Math.max(8f, c.textWidth(model.text.isEmpty() ? "Text" : model.text));
			case CustomElement.CLOCK -> Math.max(c.textWidth(clockText()) * 2f, c.textWidth(LocalDateTime.now().format(DATE)));
			default -> model.w;
		};
	}

	@Override
	public float height(Canvas c) {
		return switch (model.type) {
			case CustomElement.TEXT -> c.fontHeight() + 2f;
			case CustomElement.CLOCK -> c.fontHeight() * 3f + 4f;
			default -> model.h;
		};
	}

	@Override
	public void defaultCenter(Canvas c, float sw, float sh, float[] out) {
		Anchor a = Anchor.byId(model.anchor);
		out[0] = sw * a.fx + model.x;
		out[1] = sh * a.fy + model.y;
	}

	@Override
	public float place(Canvas c, float sw, float sh, LayoutConfig.ElementOverride ignored, float[] tmp) {
		defaultCenter(c, sw, sh, tmp);
		float w = width(c) * model.scale;
		float h = height(c) * model.scale;
		bounds.set(tmp[0] - w * 0.5f, tmp[1] - h * 0.5f, w, h);
		return model.scale;
	}

	@Override
	public boolean clickable() {
		return CustomElement.BUTTON.equals(model.type) && model.action != null && !model.action.isEmpty();
	}

	@Override
	public void click(Host host) {
		host.run(model.action);
	}

	private int color(Theme theme, int fallback) {
		return Argb.parse(model.color, fallback);
	}

	@Override
	public void draw(Canvas c, Theme theme, Host host, float cx, float cy, float scale, float time) {
		c.pushAlpha(MathUtil.clamp01(model.opacity));
		c.push();
		c.translate(cx, cy);
		if (model.rotation != 0f) {
			c.rotate((float) Math.toRadians(model.rotation));
		}
		c.scale(scale);
		float w = width(c);
		float h = height(c);
		switch (model.type) {
			case CustomElement.TEXT -> c.text(model.text.isEmpty() ? "Text" : model.text, 0f, -c.fontHeight() * 0.5f, color(theme, theme.palette().text), 1f, Align.CENTER, false);
			case CustomElement.CLOCK -> {
				int col = color(theme, theme.palette().text);
				c.text(clockText(), 0f, -h * 0.5f, col, 2f, Align.CENTER, false);
				c.text(LocalDateTime.now().format(DATE), 0f, -h * 0.5f + c.fontHeight() * 2f + 3f, Argb.withAlpha(col, 0.65f), 1f, Align.CENTER, false);
			}
			case CustomElement.IMAGE -> {
				if (image == null || !model.image.equals(imageSource)) {
					imageSource = model.image;
					image = model.image.isEmpty() ? null : host.image(model.image);
				}
				if (image != null && image.ready()) {
					c.image(image, -w * 0.5f, -h * 0.5f, w, h);
				} else {
					c.strokeRoundRect(-w * 0.5f, -h * 0.5f, w, h, 6f, 1f, Argb.withAlpha(theme.palette().textMuted, 0.6f));
					Icon.IMAGE.draw(c, 0f, 0f, Math.min(w, h) * 0.4f, theme.palette().textMuted);
				}
			}
			case CustomElement.SHAPE -> {
				int col = color(theme, Argb.withAlpha(theme.palette().accent, 0.5f));
				c.fillRoundRect(-w * 0.5f, -h * 0.5f, w, h, Math.min(model.radius, Math.min(w, h) * 0.5f), col);
			}
			case CustomElement.BUTTON -> {
				rect.set(-w * 0.5f, -h * 0.5f, w, h);
				state.reset();
				state.hover = hover;
				theme.skin().button(c, rect, ButtonStyle.TONAL, state);
				int col = theme.skin().buttonText(ButtonStyle.TONAL, state);
				String label = model.text.isEmpty() ? model.action : model.text;
				float iconW = 0f;
				if (model.icon != null && !model.icon.isEmpty()) {
					iconW = 14f;
				}
				float tw = c.textWidth(label);
				float x = -(tw + iconW) * 0.5f;
				if (iconW > 0f) {
					Icon.byId(model.icon).draw(c, x + 5f, 0f, 11f, col);
				}
				c.text(label, x + iconW, -c.fontHeight() * 0.5f + 0.5f, col, 1f, Align.LEFT, false);
			}
			default -> {
			}
		}
		c.pop();
		c.popAlpha();
	}
}
