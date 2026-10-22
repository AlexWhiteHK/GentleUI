package io.github.alexwhitehk.gentleui.core.theme.material;

import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.host.Actions;
import io.github.alexwhitehk.gentleui.core.host.UiSound;
import io.github.alexwhitehk.gentleui.core.host.WorldInfo;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Align;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.render.Icon;
import io.github.alexwhitehk.gentleui.core.scene.WidgetTitleScreen;
import io.github.alexwhitehk.gentleui.core.theme.ButtonStyle;
import io.github.alexwhitehk.gentleui.core.theme.PanelStyle;
import io.github.alexwhitehk.gentleui.core.theme.Palette;
import io.github.alexwhitehk.gentleui.core.theme.WidgetState;
import io.github.alexwhitehk.gentleui.core.ui.Button;
import io.github.alexwhitehk.gentleui.core.ui.IconButton;
import io.github.alexwhitehk.gentleui.core.ui.UiContext;
import io.github.alexwhitehk.gentleui.core.ui.Widget;
import java.util.List;

/** Title screen of the Material theme: a friendly greeting, clear primary actions and a "continue" card. */
public final class MaterialTitleScreen extends WidgetTitleScreen {
	public MaterialTitleScreen(MaterialTheme theme) {
		super(theme);
	}

	private String strip(String s) {
		return s.endsWith("...") ? s.substring(0, s.length() - 3) : s;
	}

	@Override
	protected void build(Widget root, boolean entrance) {
		float x0 = Math.max(18f, width * 0.08f);
		float colW = Math.min(170f, width * 0.42f);
		float top = Math.max(14f, height * 0.13f);
		float d = 0.08f;
		float t = entrance ? 0.05f : -1f;

		Greeting greeting = element("greeting", "gentleui.element.greeting", root.add(new Greeting()));
		greeting.bounds(x0, top, colW, 18f);
		greeting.enterDelay = t;

		Headline headline = element("headline", "gentleui.element.title", root.add(new Headline()));
		headline.bounds(x0, top + 26f, colW + 60f, 40f);
		headline.enterDelay = entrance ? t + d : -1f;

		float by = top + 74f;
		float bh = 22f;
		float gap = 6f;
		Widget actions = element("actions", "gentleui.element.actions", root.add(new Widget()));
		actions.bounds(x0, by, colW, 3 * (bh + gap) - gap);
		String[][] primary = {
			{"menu.singleplayer", "cube", Actions.SINGLEPLAYER},
			{"menu.multiplayer", "people", Actions.MULTIPLAYER},
			{"menu.online", "cloud", Actions.REALMS},
		};
		for (int i = 0; i < primary.length; i++) {
			String[] p = primary[i];
			String action = p[2];
			Button b = actions.add(new Button(() -> strip(host.tr(p[0])), () -> host.run(action)));
			b.icon(Icon.byId(p[1])).style(i == 0 ? ButtonStyle.PRIMARY : ButtonStyle.TONAL);
			b.bounds(x0, by + i * (bh + gap), colW, bh);
			b.enabled = host.available(action);
			if (!b.enabled) {
				b.tooltip = host.unavailableReason(action);
			}
			b.enterDelay = entrance ? t + d * (2 + i) : -1f;
		}
		float iy = by + primary.length * (bh + gap) + 8f;
		Widget iconRow = element("icons", "gentleui.element.icons", root.add(new Widget()));
		String[][] icons = {
			{"gear", "menu.options", Actions.OPTIONS},
			{"globe", "options.language", Actions.OPTIONS_LANGUAGE},
			{"accessibility", "options.accessibility", Actions.OPTIONS_ACCESSIBILITY},
			{"palette", "gentleui.menu.settings", Actions.GENTLE_SETTINGS},
			{"move", "gentleui.menu.editor", Actions.GENTLE_EDITOR},
			{"puzzle", "gentleui.menu.mods", Actions.MODS},
			{"power", "menu.quit", Actions.QUIT},
		};
		float ix = x0;
		float isz = 22f;
		int n = 0;
		for (String[] ic : icons) {
			String action = ic[2];
			if (Actions.MODS.equals(action) && !host.available(action)) {
				continue;
			}
			IconButton ib = iconRow.add(new IconButton(Icon.byId(ic[0]), strip(host.tr(ic[1])), () -> host.run(action)));
			ib.style(Actions.QUIT.equals(action) ? ButtonStyle.DANGER : ButtonStyle.GHOST);
			ib.bounds(ix, iy, isz, isz);
			ib.enterDelay = entrance ? t + d * 5 + n * 0.03f : -1f;
			ix += isz + 5f;
			n++;
		}
		iconRow.bounds(x0, iy, Math.max(isz, ix - 5f - x0), isz);
		float cardW = Math.min(180f, width * 0.36f);
		if (width - (x0 + colW) > cardW + 30f) {
			ContinueCard card = element("continue", "gentleui.element.continue", root.add(new ContinueCard()));
			float cx = Math.max(x0 + colW + 24f, width * 0.62f - cardW * 0.5f);
			card.bounds(Math.min(cx, width - cardW - 16f), height * 0.5f - 6f, cardW, 70f);
			card.enterDelay = entrance ? t + d * 4 : -1f;
		}
	}

	/** "Hi, Steve" with a round initial avatar, the way account chips look in web apps. */
	private final class Greeting extends Widget {
		@Override
		protected void paint(UiContext ctx) {
			Canvas c = ctx.canvas;
			Palette p = ctx.palette();
			String name = host.playerName();
			int avatar = MaterialColors.BRAND[Math.floorMod(name.hashCode(), MaterialColors.BRAND.length)];
			float r = 8f;
			c.fillCircle(bounds.x + r, bounds.cy(), r, avatar);
			String initial = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
			c.text(initial, bounds.x + r, bounds.cy() - c.fontHeight() * 0.5f + 0.5f, 0xFFFFFFFF, 1f, Align.CENTER, false);
			c.text(host.tr("gentleui.material.greeting", name), bounds.x + r * 2f + 6f, bounds.cy() - c.fontHeight() * 0.5f, p.textMuted, 1f, Align.LEFT, false);
		}
	}

	private final class Headline extends Widget {
		@Override
		protected void paint(UiContext ctx) {
			Canvas c = ctx.canvas;
			Palette p = ctx.palette();
			c.text("Minecraft", bounds.x - 1f, bounds.y, p.text, 3f, Align.LEFT, false);
			float w = c.textWidth("Minecraft", 3f);
			// Four colored dots, a nod to the brand palette.
			for (int i = 0; i < 4; i++) {
				c.fillCircle(bounds.x + w + 6f + i * 6f, bounds.y + 21f, 2f, MaterialColors.BRAND[i]);
			}
			c.text("Java Edition  ·  " + host.minecraftVersion(), bounds.x, bounds.y + 30f, p.textMuted, 1f, Align.LEFT, false);
		}
	}

	/** Card offering to resume the most recently played world. */
	private final class ContinueCard extends Widget {
		private final Rect fab = new Rect();
		private final WidgetState fabState = new WidgetState();

		@Override
		public boolean interactive() {
			return true;
		}

		@Override
		public boolean focusable() {
			return true;
		}

		@Override
		public boolean mouseDown(UiContext ctx, float x, float y, int button) {
			startRipple(x, y);
			return true;
		}

		@Override
		public void mouseUp(UiContext ctx, float x, float y, int button, boolean inside) {
			if (inside) {
				activate(ctx);
			}
		}

		@Override
		public void activate(UiContext ctx) {
			host.playSound(UiSound.CLICK);
			List<WorldInfo> worlds = host.recentWorlds();
			host.run(worlds.isEmpty() ? Actions.SINGLEPLAYER : Actions.CONTINUE);
		}

		@Override
		protected void paint(UiContext ctx) {
			Canvas c = ctx.canvas;
			Palette p = ctx.palette();
			float lift = state.hover * 2f;
			c.push();
			c.translate(0f, -lift);
			ctx.skin().panel(c, bounds, PanelStyle.CARD);
			c.fillRoundRect(bounds.x, bounds.y, bounds.w, bounds.h, 16f, Argb.withAlpha(p.accent, 0.04f * state.hover + 0.06f * state.press));
			if (state.ripple > 0f) {
				float e = 1f - (1f - state.ripple) * (1f - state.ripple);
				c.pushClip(bounds.x, bounds.y, bounds.w, bounds.h);
				c.fillCircle(bounds.x + state.rippleX, bounds.y + state.rippleY, 10f + e * bounds.w, Argb.withAlpha(p.accent, 0.12f * (1f - state.ripple)));
				c.popClip();
			}
			List<WorldInfo> worlds = host.recentWorlds();
			boolean has = !worlds.isEmpty();
			String kicker = host.tr(has ? "gentleui.material.continue" : "gentleui.material.first_world");
			String title = has ? worlds.get(0).name() : host.tr("gentleui.material.first_world_hint");
			String detail = has ? worlds.get(0).detail() : "";
			float tx = bounds.x + 14f;
			float maxW = bounds.w - 60f;
			c.text(kicker, tx, bounds.y + 12f, p.accent, 1f, Align.LEFT, false);
			List<String> lines = io.github.alexwhitehk.gentleui.core.ui.WidgetRoot.wrap(c, title, maxW);
			float ly = bounds.y + 27f;
			for (int i = 0; i < Math.min(2, lines.size()); i++) {
				String line = i == 1 && lines.size() > 2 ? c.ellipsize(lines.get(1) + " " + lines.get(2), maxW, 1f) : lines.get(i);
				c.text(c.ellipsize(line, maxW, 1f), tx, ly, p.text, 1f, Align.LEFT, false);
				ly += 11f;
			}
			if (!detail.isEmpty()) {
				c.text(c.ellipsize(detail, maxW, 1f), tx, ly + 3f, p.textMuted, 1f, Align.LEFT, false);
			}
			float fr = 15f + state.hover;
			fab.set(bounds.right() - 14f - fr * 2f, bounds.cy() - fr, fr * 2f, fr * 2f);
			fabState.set(state.hover, state.press, 0f, 0f, true);
			c.shadow(fab.x, fab.y + 2f, fab.w, fab.h, fr, 6f, 0f, Argb.withAlpha(p.shadow, 0.18f));
			c.fillCircle(fab.cx(), fab.cy(), fr, p.accent);
			Icon icon = has ? Icon.PLAY : Icon.PLUS;
			float spin = has ? 0f : state.hover * MathUtil.PI * 0.5f;
			c.push();
			c.translate(fab.cx(), fab.cy());
			c.rotate(spin);
			icon.draw(c, has ? 1f : 0f, 0f, fr * 0.95f, p.onAccent, 2.4f);
			c.pop();
			c.pop();
			if (state.focus > 0f) {
				ctx.skin().focusRing(c, bounds, 16f, state.focus);
			}
		}
	}
}
