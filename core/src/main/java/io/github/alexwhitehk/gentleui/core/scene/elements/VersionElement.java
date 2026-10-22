package io.github.alexwhitehk.gentleui.core.scene.elements;

import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.host.Host;
import io.github.alexwhitehk.gentleui.core.render.Align;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.theme.Theme;

/** "Minecraft x.y.z · GentleUI a.b.c" in a corner. */
public final class VersionElement extends TitleElement {
	private final Host host;
	private final Anchor anchor;

	public VersionElement(Host host, Anchor anchor) {
		super("version");
		this.host = host;
		this.anchor = anchor;
	}

	private String text() {
		return "Minecraft " + host.minecraftVersion() + "  ·  GentleUI " + host.modVersion();
	}

	@Override
	public String displayName(Host host) {
		return host.tr("gentleui.element.version");
	}

	@Override
	public float width(Canvas c) {
		return c.textWidth(text()) + 2f;
	}

	@Override
	public float height(Canvas c) {
		return c.fontHeight() + 2f;
	}

	@Override
	public void defaultCenter(Canvas c, float sw, float sh, float[] out) {
		float w = width(c);
		float h = height(c);
		float margin = 6f;
		out[0] = anchor.fx == 0f ? margin + w * 0.5f : (anchor.fx == 1f ? sw - margin - w * 0.5f : sw * 0.5f);
		out[1] = anchor.fy == 1f ? sh - margin - h * 0.5f : (anchor.fy == 0f ? margin + h * 0.5f : sh * 0.5f);
	}

	@Override
	public void draw(Canvas c, Theme theme, Host host, float cx, float cy, float scale, float time) {
		int color = Argb.withAlpha(theme.palette().textMuted, 0.7f);
		c.text(text(), cx, cy - c.fontHeight() * scale * 0.5f, color, scale, Align.CENTER, false);
	}
}
