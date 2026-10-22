package io.github.alexwhitehk.gentleui.core.theme.material;

import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.config.ConfigStore;
import io.github.alexwhitehk.gentleui.core.config.MaterialConfig;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.scene.UiScreen;
import io.github.alexwhitehk.gentleui.core.theme.Backdrop;
import io.github.alexwhitehk.gentleui.core.theme.Skin;
import io.github.alexwhitehk.gentleui.core.theme.Theme;

/** Bright, colorful vector theme with tonal controls. */
public final class MaterialTheme implements Theme {
	public static final String ID = "material";

	private final MaterialSkin skin = new MaterialSkin();
	private final MaterialBackground background = new MaterialBackground();
	private MaterialColors colors;
	private final float[] previewPts = new float[2 * 64];

	public MaterialTheme() {
		configChanged();
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String nameKey() {
		return "gentleui.theme.material";
	}

	@Override
	public String iconId() {
		return "sparkle";
	}

	@Override
	public Skin skin() {
		return skin;
	}

	public MaterialColors colors() {
		return colors;
	}

	@Override
	public void background(Canvas c, Backdrop b) {
		background.render(c, b, colors);
	}

	@Override
	public void pointerPressed(float x, float y) {
		background.ink(x, y);
	}

	@Override
	public UiScreen createTitleScreen() {
		return new MaterialTitleScreen(this);
	}

	@Override
	public void preview(Canvas c, Rect r, float time) {
		MaterialColors light = colors.dark ? new MaterialColors(Argb.parse(ConfigStore.config().material.accent, 0xFF1A73E8), true) : colors;
		c.pushClip(r.x, r.y, r.w, r.h);
		c.fillRect(r.x, r.y, r.w, r.h, light.palette.background);
		float m = Math.min(r.w, r.h);
		Shapes.outline(Shapes.Kind.COOKIE, r.right() - m * 0.18f, r.y + m * 0.25f, m * 0.38f, time * 0.2f, 64, previewPts);
		c.fillPolygon(previewPts, 64, light.pastel(MaterialColors.BLUE));
		c.fillCircle(r.x + r.w * 0.62f, r.bottom() - m * 0.12f, m * 0.16f, light.pastel(MaterialColors.RED));
		Shapes.outline(Shapes.Kind.FLOWER, r.x + r.w * 0.5f, r.y + m * 0.16f, m * 0.1f, -time * 0.3f, 64, previewPts);
		c.fillPolygon(previewPts, 64, MaterialColors.YELLOW);
		c.fillRoundRect(r.x + 6f, r.y + r.h * 0.42f, r.w * 0.42f, 6f, 3f, light.palette.accent);
		c.fillRoundRect(r.x + 6f, r.y + r.h * 0.42f + 9f, r.w * 0.42f, 6f, 3f, light.palette.surfaceVariant);
		c.fillRoundRect(r.x + 6f, r.y + r.h * 0.42f + 18f, r.w * 0.42f, 6f, 3f, light.palette.surfaceVariant);
		c.fillCircle(r.x + 10f, r.y + 9f, 3f, MaterialColors.GREEN);
		c.popClip();
	}

	@Override
	public void configChanged() {
		MaterialConfig cfg = ConfigStore.config().material;
		colors = new MaterialColors(Argb.parse(cfg.accent, 0xFF1A73E8), cfg.dark);
		skin.apply(colors);
		background.configure(cfg.shapes);
	}
}
