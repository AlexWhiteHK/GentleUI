package io.github.alexwhitehk.gentleui.core.theme.glass;

import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.config.ConfigStore;
import io.github.alexwhitehk.gentleui.core.host.Actions;
import io.github.alexwhitehk.gentleui.core.host.UiSound;
import io.github.alexwhitehk.gentleui.core.host.WorldInfo;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Align;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.render.Icon;
import io.github.alexwhitehk.gentleui.core.scene.WidgetTitleScreen;
import io.github.alexwhitehk.gentleui.core.scene.elements.Anchor;
import io.github.alexwhitehk.gentleui.core.scene.elements.TitleElement;
import io.github.alexwhitehk.gentleui.core.scene.elements.VersionElement;
import io.github.alexwhitehk.gentleui.core.theme.ButtonStyle;
import io.github.alexwhitehk.gentleui.core.theme.Palette;
import io.github.alexwhitehk.gentleui.core.theme.Theme;
import io.github.alexwhitehk.gentleui.core.ui.Button;
import io.github.alexwhitehk.gentleui.core.ui.UiContext;
import io.github.alexwhitehk.gentleui.core.ui.Widget;
import io.github.alexwhitehk.gentleui.core.host.Host;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Title screen of the Glass theme: a centered frosted card and a magnifying dock. */
public final class GlassTitleScreen extends WidgetTitleScreen {
	private final GlassTheme glass;

	public GlassTitleScreen(GlassTheme theme) {
		super(theme);
		this.glass = theme;
	}

	private String strip(String s) {
		return s.endsWith("...") ? s.substring(0, s.length() - 3) : s;
	}

	@Override
	protected void addBuiltIns(List<TitleElement> out) {
		if (ConfigStore.config().showVersion) {
			out.add(new VersionElement(host, Anchor.TOP_LEFT));
		}
		out.add(new ClockElement());
	}

	private boolean builtWithWorlds;

	@Override
	protected void renderScene(Canvas c, float dt) {
		// The world list loads in the background; add the "continue" dock item once it arrives.
		if (builtWithWorlds == host.recentWorlds().isEmpty()) {
			layout();
		}
		super.renderScene(c, dt);
	}

	@Override
	protected void build(Widget root, boolean entrance) {
		builtWithWorlds = !host.recentWorlds().isEmpty();
		float cardW = Math.min(210f, width - 40f);
		float bh = 24f;
		float gap = 6f;
		float cardH = 3 * bh + 2 * gap + 24f;
		float titleH = 44f;
		float dockH = 34f;
		float total = titleH + cardH;
		float top = Math.max(26f, (height - dockH - 20f - total) * 0.5f);

		Title title = element("headline", "gentleui.element.title", root.add(new Title()));
		title.bounds(width * 0.5f - 120f, top, 240f, titleH);
		title.enterDelay = entrance ? 0.05f : -1f;

		Card card = element("actions", "gentleui.element.actions", root.add(new Card()));
		card.bounds(width * 0.5f - cardW * 0.5f, top + titleH, cardW, cardH);
		card.enterDelay = entrance ? 0.14f : -1f;
		String[][] primary = {
			{"menu.singleplayer", "cube", Actions.SINGLEPLAYER},
			{"menu.multiplayer", "people", Actions.MULTIPLAYER},
			{"menu.online", "cloud", Actions.REALMS},
		};
		for (int i = 0; i < primary.length; i++) {
			String[] p = primary[i];
			String action = p[2];
			Button b = card.add(new Button(() -> strip(host.tr(p[0])), () -> host.run(action)));
			b.icon(Icon.byId(p[1])).style(i == 0 ? ButtonStyle.PRIMARY : ButtonStyle.TONAL);
			b.bounds(card.bounds.x + 12f, card.bounds.y + 12f + i * (bh + gap), cardW - 24f, bh);
			b.enabled = host.available(action);
			if (!b.enabled) {
				b.tooltip = host.unavailableReason(action);
			}
		}

		List<DockItem> items = new ArrayList<>();
		if (!host.recentWorlds().isEmpty()) {
			items.add(new DockItem("forward", "gentleui.menu.continue", Actions.CONTINUE));
		}
		items.add(new DockItem("gear", "menu.options", Actions.OPTIONS));
		items.add(new DockItem("globe", "options.language", Actions.OPTIONS_LANGUAGE));
		items.add(new DockItem("accessibility", "options.accessibility", Actions.OPTIONS_ACCESSIBILITY));
		items.add(new DockItem("palette", "gentleui.menu.settings", Actions.GENTLE_SETTINGS));
		items.add(new DockItem("move", "gentleui.menu.editor", Actions.GENTLE_EDITOR));
		if (host.available(Actions.MODS)) {
			items.add(new DockItem("puzzle", "gentleui.menu.mods", Actions.MODS));
		}
		items.add(new DockItem("book", "gentleui.menu.credits", Actions.CREDITS));
		items.add(new DockItem("power", "menu.quit", Actions.QUIT));
		Dock dock = element("dock", "gentleui.element.dock", root.add(new Dock(items)));
		float dw = items.size() * 26f + 16f;
		dock.bounds(width * 0.5f - dw * 0.5f, height - dockH - 10f, dw, dockH);
		dock.enterDelay = entrance ? 0.26f : -1f;
	}

	private final class Title extends Widget {
		@Override
		protected void paint(UiContext ctx) {
			Canvas c = ctx.canvas;
			Palette p = ctx.palette();
			c.textTracked("MINECRAFT", bounds.cx(), bounds.y, p.text, 2f, 3f, Align.CENTER);
			c.text("Java Edition", bounds.cx(), bounds.y + 22f, Argb.withAlpha(p.textMuted, 0.9f), 1f, Align.CENTER, false);
		}
	}

	private final class Card extends Widget {
		@Override
		protected void paint(UiContext ctx) {
			glass.glassSkin().glass(ctx.canvas, bounds, 18f, 0f, 0, 1.4f);
		}
	}

	private record DockItem(String icon, String label, String action) {
	}

	/** Icon dock that magnifies the items around the pointer. */
	private final class Dock extends Widget {
		private final List<DockItem> items;
		private final float[] scale;
		private final float[] lift;
		private final float[] drawnX;
		private final float[] drawnHalf;
		private boolean drawn;
		private final Rect slab = new Rect();
		private final Rect icon = new Rect();
		private int pressed = -1;

		Dock(List<DockItem> items) {
			this.items = items;
			this.scale = new float[items.size()];
			this.lift = new float[items.size()];
			this.drawnX = new float[items.size()];
			this.drawnHalf = new float[items.size()];
		}

		@Override
		public boolean interactive() {
			return true;
		}

		private float itemX(int i) {
			return bounds.x + 8f + 13f + i * 26f;
		}

		/** Hit test against where items were actually drawn, which differs from layout while magnified. */
		private int indexAt(float x, float y) {
			if (y < bounds.y - 14f || y > bounds.bottom()) {
				return -1;
			}
			int best = -1;
			float bestD = Float.MAX_VALUE;
			for (int i = 0; i < items.size(); i++) {
				float cx = drawn ? drawnX[i] : itemX(i);
				float half = drawn ? drawnHalf[i] : 13f;
				float d = Math.abs(x - cx);
				if (d <= half && d < bestD) {
					best = i;
					bestD = d;
				}
			}
			return best;
		}

		@Override
		public boolean mouseDown(UiContext ctx, float x, float y, int button) {
			pressed = indexAt(x, y);
			return true;
		}

		@Override
		public void mouseUp(UiContext ctx, float x, float y, int button, boolean inside) {
			int i = indexAt(x, y);
			if (i >= 0 && i == pressed) {
				host.playSound(UiSound.CLICK);
				host.run(items.get(i).action());
			}
			pressed = -1;
		}

		@Override
		protected void paint(UiContext ctx) {
			Canvas c = ctx.canvas;
			Palette p = ctx.palette();
			boolean magnify = ConfigStore.config().glass.dockMagnify;
			boolean inside = isHovered(ctx) || (ctx.mouseY > bounds.y - 18f && ctx.mouseY < bounds.bottom() && ctx.mouseX > bounds.x && ctx.mouseX < bounds.right());
			int hoverIndex = inside ? indexAt(ctx.mouseX, ctx.mouseY) : -1;
			float extra = 0f;
			for (int i = 0; i < items.size(); i++) {
				float d = Math.abs(ctx.mouseX - itemX(i)) / 26f;
				float target = inside && magnify ? 1f + 0.45f * (float) Math.exp(-d * d * 0.9f) : 1f;
				scale[i] = MathUtil.approach(scale[i] == 0f ? 1f : scale[i], target, 0.05f, ctx.dt);
				lift[i] = MathUtil.approach(lift[i], i == hoverIndex ? 1f : 0f, 0.06f, ctx.dt);
				extra += (scale[i] - 1f) * 20f;
			}
			slab.set(bounds.x - extra * 0.5f, bounds.y, bounds.w + extra, bounds.h);
			glass.glassSkin().glass(c, slab, 14f, 0f, 0, 1.2f);
			float x = slab.x + 8f;
			for (int i = 0; i < items.size(); i++) {
				DockItem it = items.get(i);
				float s = scale[i];
				float size = 20f * s;
				float slot = 26f + (s - 1f) * 20f;
				float cx = x + slot * 0.5f;
				float cy = bounds.cy() - (s - 1f) * 9f;
				icon.set(cx - size * 0.5f, cy - size * 0.5f, size, size);
				drawnX[i] = cx;
				drawnHalf[i] = slot * 0.5f;
				boolean available = host.available(it.action());
				c.pushAlpha(available ? 1f : 0.45f);
				glass.glassSkin().glass(c, icon, size * 0.32f, lift[i], Actions.QUIT.equals(it.action()) ? Argb.withAlpha(0xFFFF4D6D, 0.25f * lift[i]) : 0, 0.2f + 0.6f * lift[i]);
				Icon.byId(it.icon()).draw(c, cx, cy, size * 0.58f, p.text, 2f);
				c.popAlpha();
				if (i == hoverIndex && lift[i] > 0.05f) {
					String label = strip(host.tr(it.label()));
					if (!available && host.unavailableReason(it.action()) != null) {
						label = host.unavailableReason(it.action());
					}
					float tw = c.textWidth(label) + 12f;
					float ty = icon.y - 20f - 3f * lift[i];
					Rect tip = new Rect(cx - tw * 0.5f, ty, tw, 15f);
					c.pushAlpha(lift[i]);
					ctx.skin().tooltip(c, tip);
					c.text(label, cx, ty + 4f, p.text, 1f, Align.CENTER, false);
					c.popAlpha();
				}
				x += slot;
			}
			drawn = true;
		}
	}

	/** Large clock in the top right, a status-bar touch. */
	private final class ClockElement extends TitleElement {
		private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

		ClockElement() {
			super("clock");
		}

		@Override
		public String displayName(Host host) {
			return host.tr("gentleui.element.type.clock");
		}

		@Override
		public float width(Canvas c) {
			return c.textWidth("00:00", 2f) + 4f;
		}

		@Override
		public float height(Canvas c) {
			return c.fontHeight() * 2f + 4f;
		}

		@Override
		public void defaultCenter(Canvas c, float sw, float sh, float[] out) {
			out[0] = sw - 10f - width(c) * 0.5f;
			out[1] = 10f + height(c) * 0.5f;
		}

		@Override
		public void draw(Canvas c, Theme theme, Host host, float cx, float cy, float scale, float time) {
			String now = LocalTime.now().format(fmt);
			c.text(now, cx, cy - c.fontHeight() * scale, Argb.withAlpha(theme.palette().text, 0.85f), 2f * scale, Align.CENTER, false);
		}
	}
}
