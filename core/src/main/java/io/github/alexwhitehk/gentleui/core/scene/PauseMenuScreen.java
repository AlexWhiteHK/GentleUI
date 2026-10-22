package io.github.alexwhitehk.gentleui.core.scene;

import io.github.alexwhitehk.gentleui.core.config.ConfigStore;
import io.github.alexwhitehk.gentleui.core.host.Actions;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Align;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.render.Icon;
import io.github.alexwhitehk.gentleui.core.theme.Backdrop;
import io.github.alexwhitehk.gentleui.core.theme.ButtonStyle;
import io.github.alexwhitehk.gentleui.core.theme.PanelStyle;
import io.github.alexwhitehk.gentleui.core.theme.Theme;
import io.github.alexwhitehk.gentleui.core.theme.Themes;
import io.github.alexwhitehk.gentleui.core.ui.Button;
import io.github.alexwhitehk.gentleui.core.ui.WidgetRoot;

/** Themed in-game menu. The world keeps running visibly (blurred) behind it. */
public final class PauseMenuScreen extends UiScreen {
	private final WidgetRoot ui = new WidgetRoot();
	private final Backdrop backdrop = new Backdrop();
	private final Rect card = new Rect();
	private final Rect cardNow = new Rect();
	private float time;
	private boolean built;

	@Override
	public String title() {
		return host.tr("menu.game");
	}

	private String label(String action, String key) {
		String l = host.actionLabel(action);
		String text = l != null ? l : host.tr(key);
		return text.endsWith("...") ? text.substring(0, text.length() - 3) : text;
	}

	@Override
	protected void layout() {
		float w = Math.min(250f, width - 32f);
		float bh = 22f;
		float gap = 5f;
		float h = 34f + 5 * bh + 4 * gap + 12f;
		card.set((width - w) * 0.5f, Math.max(10f, (height - h) * 0.42f), w, h);
		ui.root.clear();
		boolean entrance = !built;
		built = true;
		float x0 = card.x + 12f;
		float inner = card.w - 24f;
		float half = (inner - gap) * 0.5f;
		float y = card.y + 34f;
		int n = 0;
		add(label(Actions.RESUME, "menu.returnToGame"), Icon.PLAY, Actions.RESUME, ButtonStyle.PRIMARY, x0, y, inner, bh, entrance, n++);
		y += bh + gap;
		add(label(Actions.ADVANCEMENTS, "gui.advancements"), Icon.FLAG, Actions.ADVANCEMENTS, ButtonStyle.TONAL, x0, y, half, bh, entrance, n++);
		add(label(Actions.STATISTICS, "gui.stats"), Icon.CHART, Actions.STATISTICS, ButtonStyle.TONAL, x0 + half + gap, y, half, bh, entrance, n++);
		y += bh + gap;
		add(label(Actions.OPTIONS, "menu.options"), Icon.GEAR, Actions.OPTIONS, ButtonStyle.TONAL, x0, y, half, bh, entrance, n++);
		if (host.available(Actions.OPEN_TO_LAN)) {
			add(label(Actions.OPEN_TO_LAN, "menu.shareToLan"), Icon.SHARE, Actions.OPEN_TO_LAN, ButtonStyle.TONAL, x0 + half + gap, y, half, bh, entrance, n++);
		} else {
			add(label(Actions.PLAYER_REPORTING, "menu.playerReporting"), Icon.PEOPLE, Actions.PLAYER_REPORTING, ButtonStyle.TONAL, x0 + half + gap, y, half, bh, entrance, n++);
		}
		y += bh + gap;
		add("GentleUI", Icon.PALETTE, Actions.GENTLE_SETTINGS, ButtonStyle.TONAL, x0, y, half, bh, entrance, n++);
		add(host.tr("gentleui.pause.more"), Icon.MORE, Actions.VANILLA_PAUSE, ButtonStyle.TONAL, x0 + half + gap, y, half, bh, entrance, n++);
		y += bh + gap;
		add(label(Actions.DISCONNECT, "menu.returnToMenu"), Icon.LOGOUT, Actions.DISCONNECT, ButtonStyle.DANGER, x0, y, inner, bh, entrance, n++);
		ui.layout(width, height);
	}

	private void add(String text, Icon icon, String action, ButtonStyle style, float x, float y, float w, float h, boolean entrance, int index) {
		Button b = ui.root.add(new Button(text, () -> host.run(action)));
		b.icon(icon).style(style);
		b.bounds(x, y, w, h);
		b.enabled = host.available(action);
		if (entrance) {
			b.enterDelay = 0.06f + index * 0.035f;
		}
	}

	@Override
	protected void render(Canvas c, float dt) {
		time += dt;
		Theme theme = Themes.active();
		backdrop.width = width;
		backdrop.height = height;
		backdrop.mouseX = mouseX;
		backdrop.mouseY = mouseY;
		backdrop.dt = dt;
		backdrop.time = time;
		backdrop.inWorld = true;
		backdrop.calm = 0.8f;
		backdrop.pointerActive = ConfigStore.config().pointerEffects;
		theme.background(c, backdrop);
		// The card rises into place on open.
		float e = io.github.alexwhitehk.gentleui.core.math.Easing.EMPHASIZED.apply(Math.min(1f, time / 0.4f));
		cardNow.set(card.x, card.y + (1f - e) * 12f, card.w, card.h);
		c.pushAlpha(e);
		theme.skin().panel(c, cardNow, PanelStyle.SHEET);
		c.text(title(), cardNow.cx(), cardNow.y + 13f, theme.palette().text, 1f, Align.CENTER, false);
		theme.skin().separator(c, cardNow.x + 14f, cardNow.y + 28f, cardNow.w - 28f);
		c.popAlpha();
		ui.render(c, theme, host, mouseX, mouseY, dt, time);
	}

	@Override
	public boolean mouseDown(float x, float y, int button, int mods) {
		if (!ui.mouseDown(x, y, button)) {
			Themes.active().pointerPressed(x, y);
		}
		return true;
	}

	@Override
	public boolean mouseUp(float x, float y, int button, int mods) {
		return ui.mouseUp(x, y, button);
	}

	@Override
	public boolean keyDown(int key, int scancode, int mods) {
		if (super.keyDown(key, scancode, mods)) {
			return true;
		}
		return ui.keyDown(key, mods);
	}

	@Override
	public void onClose() {
		host.run(Actions.RESUME);
	}
}
