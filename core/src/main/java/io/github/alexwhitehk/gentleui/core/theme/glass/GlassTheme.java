package io.github.alexwhitehk.gentleui.core.theme.glass;

import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.config.ConfigStore;
import io.github.alexwhitehk.gentleui.core.config.GlassConfig;
import io.github.alexwhitehk.gentleui.core.host.SoundSpec;
import io.github.alexwhitehk.gentleui.core.host.UiSound;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.scene.UiScreen;
import io.github.alexwhitehk.gentleui.core.theme.Backdrop;
import io.github.alexwhitehk.gentleui.core.theme.Skin;
import io.github.alexwhitehk.gentleui.core.theme.Theme;

/** Frosted glass panels over luminous, slowly drifting color. */
public final class GlassTheme implements Theme {
	public static final String ID = "glass";

	private final GlassSkin skin = new GlassSkin();
	private final GlassBackground background = new GlassBackground();
	private GlassPalette palette = GlassPalette.AURORA;
	private final Rect tmp = new Rect();

	public GlassTheme() {
		configChanged();
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String nameKey() {
		return "gentleui.theme.glass";
	}

	@Override
	public String iconId() {
		return "glass";
	}

	@Override
	public Skin skin() {
		return skin;
	}

	public GlassSkin glassSkin() {
		return skin;
	}

	public boolean worldBlur() {
		return ConfigStore.config().glass.worldBlur;
	}

	@Override
	public void background(Canvas c, Backdrop b) {
		background.render(c, b, palette);
	}

	@Override
	public void pointerPressed(float x, float y) {
		background.spark(x, y);
	}

	@Override
	public UiScreen createTitleScreen() {
		return new GlassTitleScreen(this);
	}

	@Override
	public SoundSpec sound(UiSound sound) {
		return switch (sound) {
			case CLICK -> new SoundSpec("minecraft:ui.button.click", 1.35f, 0.2f);
			case OPEN -> new SoundSpec("minecraft:block.amethyst_block.chime", 1.8f, 0.35f);
			case CLOSE -> new SoundSpec("minecraft:block.amethyst_block.chime", 1.2f, 0.3f);
			case TOGGLE -> new SoundSpec("minecraft:ui.button.click", 1.6f, 0.16f);
			default -> null;
		};
	}

	@Override
	public void preview(Canvas c, Rect r, float time) {
		c.pushClip(r.x, r.y, r.w, r.h);
		c.fillRectGradientV(r.x, r.y, r.w, r.h, new int[] {palette.baseTop, palette.baseBottom}, new float[] {0f, 1f});
		float m = Math.max(r.w, r.h);
		c.glow(r.x + r.w * (0.25f + 0.05f * (float) Math.sin(time * 0.6)), r.y + r.h * 0.3f, m * 0.6f, Argb.withAlpha(palette.orbs[0], 0.8f));
		c.glow(r.x + r.w * 0.8f, r.y + r.h * (0.7f + 0.05f * (float) Math.cos(time * 0.5)), m * 0.55f, Argb.withAlpha(palette.orbs[2], 0.75f));
		c.glow(r.x + r.w * 0.7f, r.y + r.h * 0.15f, m * 0.4f, Argb.withAlpha(palette.orbs[1], 0.7f));
		tmp.set(r.x + r.w * 0.2f, r.y + r.h * 0.28f, r.w * 0.6f, r.h * 0.44f);
		skin.glass(c, tmp, 6f, 0f, 0, 0.5f);
		tmp.set(r.x + r.w * 0.3f, r.bottom() - 9f, r.w * 0.4f, 6f);
		skin.glass(c, tmp, 3f, 0f, 0, 0f);
		c.popClip();
	}

	@Override
	public void configChanged() {
		GlassConfig cfg = ConfigStore.config().glass;
		palette = GlassPalette.byId(cfg.palette);
		skin.apply(palette, cfg.frost);
	}
}
