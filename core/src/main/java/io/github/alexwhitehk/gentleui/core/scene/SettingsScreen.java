package io.github.alexwhitehk.gentleui.core.scene;

import io.github.alexwhitehk.gentleui.core.anim.Spring;
import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.config.ConfigStore;
import io.github.alexwhitehk.gentleui.core.config.GentleConfig;
import io.github.alexwhitehk.gentleui.core.config.LiquidConfig;
import io.github.alexwhitehk.gentleui.core.config.MenuNode;
import io.github.alexwhitehk.gentleui.core.host.Actions;
import io.github.alexwhitehk.gentleui.core.host.UiSound;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Align;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.render.Icon;
import io.github.alexwhitehk.gentleui.core.theme.Backdrop;
import io.github.alexwhitehk.gentleui.core.theme.ButtonStyle;
import io.github.alexwhitehk.gentleui.core.theme.PanelStyle;
import io.github.alexwhitehk.gentleui.core.theme.Theme;
import io.github.alexwhitehk.gentleui.core.theme.Themes;
import io.github.alexwhitehk.gentleui.core.theme.glass.GlassPalette;
import io.github.alexwhitehk.gentleui.core.theme.liquid.LiquidPalette;
import io.github.alexwhitehk.gentleui.core.theme.material.MaterialColors;
import io.github.alexwhitehk.gentleui.core.ui.Button;
import io.github.alexwhitehk.gentleui.core.ui.Choice;
import io.github.alexwhitehk.gentleui.core.ui.IconButton;
import io.github.alexwhitehk.gentleui.core.ui.Label;
import io.github.alexwhitehk.gentleui.core.ui.ScrollPanel;
import io.github.alexwhitehk.gentleui.core.ui.Slider;
import io.github.alexwhitehk.gentleui.core.ui.Toggle;
import io.github.alexwhitehk.gentleui.core.ui.UiContext;
import io.github.alexwhitehk.gentleui.core.ui.Widget;
import io.github.alexwhitehk.gentleui.core.ui.WidgetRoot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** GentleUI's own settings, styled by the active theme and applied live. */
public final class SettingsScreen extends UiScreen {
	private enum Section {
		APPEARANCE("gentleui.settings.appearance", "palette"),
		MOTION("gentleui.settings.motion", "sparkle"),
		INTERFACE("gentleui.settings.interface", "monitor"),
		LAYOUT("gentleui.settings.layout", "move"),
		ABOUT("gentleui.settings.about", "info");

		final String key;
		final String icon;

		Section(String key, String icon) {
			this.key = key;
			this.icon = icon;
		}
	}

	private final WidgetRoot ui = new WidgetRoot();
	private final Backdrop backdrop = new Backdrop();
	private final Rect sheet = new Rect();
	private Section section = Section.APPEARANCE;
	private ScrollPanel content;
	private final Spring contentIn = new Spring(1f, 160f, 1f);
	private float time;
	private String builtForTheme = "";

	private GentleConfig cfg() {
		return ConfigStore.config();
	}

	private void edit(Consumer<GentleConfig> change) {
		ConfigStore.get().edit(change);
		host.configChanged();
	}

	@Override
	public String title() {
		return "GentleUI";
	}

	@Override
	protected void layout() {
		float w = Math.min(420f, width - 24f);
		float h = Math.min(300f, height - 20f);
		sheet.set((width - w) * 0.5f, (height - h) * 0.5f, w, h);
		rebuild();
	}

	private void rebuild() {
		Widget root = ui.root;
		root.clear();
		builtForTheme = cfg().theme;
		IconButton close = root.add(new IconButton(Icon.CLOSE, host.tr("gui.done"), this::onClose));
		close.bounds(sheet.right() - 26f, sheet.y + 8f, 18f, 18f);
		float sideW = Math.min(104f, sheet.w * 0.28f);
		float y = sheet.y + 36f;
		for (Section s : Section.values()) {
			Button b = root.add(new Button(() -> host.tr(s.key), () -> switchTo(s)));
			b.icon(Icon.byId(s.icon)).style(ButtonStyle.NAV);
			b.align = Align.LEFT;
			b.selected = () -> section == s;
			b.bounds(sheet.x + 8f, y, sideW - 8f, 20f);
			y += 23f;
		}
		content = root.add(new ScrollPanel());
		content.bounds(sheet.x + sideW + 6f, sheet.y + 34f, sheet.w - sideW - 12f, sheet.h - 42f);
		fillSection(content);
		ui.layout(width, height);
		content.stack(4f);
	}

	private void switchTo(Section s) {
		if (section == s) {
			return;
		}
		section = s;
		contentIn.snap(0f).target(1f);
		rebuild();
		content.scrollTo(0f);
	}

	// ----------------------------------------------------------------------------------------------------------
	// Sections
	// ----------------------------------------------------------------------------------------------------------

	private void fillSection(ScrollPanel p) {
		switch (section) {
			case APPEARANCE -> appearance(p);
			case MOTION -> motion(p);
			case INTERFACE -> interfaceSection(p);
			case LAYOUT -> layoutSection(p);
			case ABOUT -> about(p);
		}
	}

	private Widget header(ScrollPanel p, String key) {
		Label l = p.add(new Label(host.tr(key)));
		l.tone(Label.Tone.ACCENT);
		l.prefHeight = 18f;
		return l;
	}

	private Toggle toggle(ScrollPanel p, String key, java.util.function.Supplier<Boolean> get, Consumer<Boolean> set) {
		Toggle t = p.add(new Toggle(() -> host.tr(key), get, v -> edit(c -> set.accept(v))));
		String desc = key + ".desc";
		if (host.hasTranslation(desc)) {
			t.description(host.tr(desc));
		}
		return t;
	}

	private void appearance(ScrollPanel p) {
		header(p, "gentleui.settings.theme");
		p.add(new ThemePicker()).prefHeight = 74f;
		String theme = cfg().theme;
		if ("liquid".equals(theme)) {
			header(p, "gentleui.theme.liquid");
			List<String> ids = new ArrayList<>(LiquidPalette.all().keySet());
			p.add(new Choice(() -> host.tr("gentleui.settings.palette"), names(ids, "gentleui.palette."),
				() -> Math.max(0, ids.indexOf(cfg().liquid.palette)), i -> edit(c -> c.liquid.palette = ids.get(i))));
			p.add(new Slider(() -> host.tr("gentleui.settings.ripples"), 0f, 2f, 0.05f, () -> cfg().liquid.rippleStrength,
				v -> edit(c -> c.liquid.rippleStrength = v), v -> Math.round(v * 100f) + "%"));
			toggle(p, "gentleui.settings.labels", () -> cfg().liquid.labels, v -> cfg().liquid.labels = v);
			toggle(p, "gentleui.settings.idle_hint", () -> cfg().liquid.idleHint, v -> cfg().liquid.idleHint = v);
		} else if ("material".equals(theme)) {
			header(p, "gentleui.theme.material");
			p.add(new Swatches()).prefHeight = 38f;
			toggle(p, "gentleui.settings.dark", () -> cfg().material.dark, v -> cfg().material.dark = v);
			toggle(p, "gentleui.settings.shapes", () -> cfg().material.shapes, v -> cfg().material.shapes = v);
		} else if ("glass".equals(theme)) {
			header(p, "gentleui.theme.glass");
			List<String> ids = new ArrayList<>(GlassPalette.all().keySet());
			p.add(new Choice(() -> host.tr("gentleui.settings.palette"), names(ids, "gentleui.palette."),
				() -> Math.max(0, ids.indexOf(cfg().glass.palette)), i -> edit(c -> c.glass.palette = ids.get(i))));
			p.add(new Slider(() -> host.tr("gentleui.settings.frost"), 0f, 1f, 0.05f, () -> cfg().glass.frost,
				v -> edit(c -> c.glass.frost = v), v -> Math.round(v * 100f) + "%"));
			toggle(p, "gentleui.settings.world_blur", () -> cfg().glass.worldBlur, v -> cfg().glass.worldBlur = v);
			toggle(p, "gentleui.settings.dock_magnify", () -> cfg().glass.dockMagnify, v -> cfg().glass.dockMagnify = v);
		}
	}

	private List<String> names(List<String> ids, String prefix) {
		List<String> out = new ArrayList<>();
		for (String id : ids) {
			out.add(host.tr(prefix + id));
		}
		return out;
	}

	private void motion(ScrollPanel p) {
		header(p, "gentleui.settings.motion");
		p.add(new Slider(() -> host.tr("gentleui.settings.speed"), 0.5f, 2f, 0.05f, () -> cfg().animationSpeed,
			v -> edit(c -> c.animationSpeed = v), v -> String.format(Locale.ROOT, "%.2f×", v)));
		toggle(p, "gentleui.settings.reduce_motion", () -> cfg().reduceMotion, v -> cfg().reduceMotion = v);
		toggle(p, "gentleui.settings.transitions", () -> cfg().screenTransitions, v -> cfg().screenTransitions = v);
		toggle(p, "gentleui.settings.pointer", () -> cfg().pointerEffects, v -> cfg().pointerEffects = v);
		p.add(new Slider(() -> host.tr("gentleui.settings.detail"), 0.25f, 1.5f, 0.05f, () -> cfg().detail,
			v -> edit(c -> c.detail = v), v -> Math.round(v * 100f) + "%"));
	}

	private void interfaceSection(ScrollPanel p) {
		header(p, "gentleui.settings.menus");
		toggle(p, "gentleui.settings.skin", () -> cfg().skinMenus, v -> cfg().skinMenus = v);
		toggle(p, "gentleui.settings.adapt_text", () -> cfg().adaptText, v -> cfg().adaptText = v);
		toggle(p, "gentleui.settings.pause", () -> cfg().themedPauseMenu, v -> cfg().themedPauseMenu = v);
		toggle(p, "gentleui.settings.loading", () -> cfg().themedLoading, v -> cfg().themedLoading = v);
		header(p, "gentleui.settings.extras");
		toggle(p, "gentleui.settings.version", () -> cfg().showVersion, v -> cfg().showVersion = v);
		toggle(p, "gentleui.settings.sounds", () -> cfg().uiSounds, v -> cfg().uiSounds = v);
	}

	private static final String[] ROOT_MENUS = {"play", "options", "personalize", "system"};
	private static final String[] EDGES = {LiquidConfig.BOTTOM, LiquidConfig.RIGHT, LiquidConfig.LEFT, LiquidConfig.TOP};

	private void layoutSection(ScrollPanel p) {
		header(p, "gentleui.settings.layout_editor");
		Label hint = p.add(new Label(host.tr("gentleui.settings.layout_editor.desc")).tone(Label.Tone.MUTED).wrap());
		hint.prefHeight = 30f;
		Button open = p.add(new Button(() -> host.tr("gentleui.menu.editor"), () -> host.run(Actions.GENTLE_EDITOR)));
		open.icon(Icon.MOVE).style(ButtonStyle.PRIMARY);
		open.prefHeight = 22f;
		Button reset = p.add(new Button(() -> host.tr("gentleui.settings.reset_layout"), () -> edit(c -> c.layouts.remove(c.theme))));
		reset.icon(Icon.RESET);
		reset.prefHeight = 22f;
		header(p, "gentleui.settings.shores");
		List<String> options = new ArrayList<>();
		for (String id : ROOT_MENUS) {
			options.add(host.tr(rootLabel(id)));
		}
		options.add(host.tr("gentleui.settings.none"));
		for (String edge : EDGES) {
			p.add(new Choice(() -> host.tr("gentleui.edge." + edge), options, () -> edgeIndex(edge), i -> assignEdge(edge, i)));
		}
		Button restore = p.add(new Button(() -> host.tr("gentleui.settings.restore_menus"), () -> edit(c -> c.liquid.menus = LiquidConfig.defaultMenus())));
		restore.icon(Icon.RESET);
		restore.prefHeight = 22f;
	}

	private String rootLabel(String id) {
		for (MenuNode n : LiquidConfig.defaultMenus().values()) {
			if (n.id.equals(id)) {
				return n.label;
			}
		}
		return id;
	}

	private int edgeIndex(String edge) {
		MenuNode n = cfg().liquid.menus.get(edge);
		if (n == null) {
			return ROOT_MENUS.length;
		}
		for (int i = 0; i < ROOT_MENUS.length; i++) {
			if (ROOT_MENUS[i].equals(n.id)) {
				return i;
			}
		}
		return ROOT_MENUS.length;
	}

	/** Puts menu {@code index} on {@code edge}, swapping with the edge that held it before. */
	private void assignEdge(String edge, int index) {
		edit(c -> {
			Map<String, MenuNode> menus = c.liquid.menus;
			MenuNode current = menus.get(edge);
			if (index >= ROOT_MENUS.length) {
				menus.remove(edge);
				return;
			}
			String wanted = ROOT_MENUS[index];
			String otherEdge = null;
			for (Map.Entry<String, MenuNode> e : menus.entrySet()) {
				if (e.getValue().id.equals(wanted)) {
					otherEdge = e.getKey();
				}
			}
			MenuNode node = otherEdge != null ? menus.get(otherEdge) : null;
			if (node == null) {
				for (MenuNode d : LiquidConfig.defaultMenus().values()) {
					if (d.id.equals(wanted)) {
						node = d;
					}
				}
			}
			menus.put(edge, node);
			if (otherEdge != null && !otherEdge.equals(edge)) {
				if (current != null) {
					menus.put(otherEdge, current);
				} else {
					menus.remove(otherEdge);
				}
			}
		});
	}

	private void about(ScrollPanel p) {
		header(p, "gentleui.settings.about");
		Label l = p.add(new Label(() -> "GentleUI " + host.modVersion() + "  ·  Minecraft " + host.minecraftVersion()));
		l.prefHeight = 16f;
		Label d = p.add(new Label(host.tr("gentleui.settings.about.desc")).tone(Label.Tone.MUTED).wrap());
		d.prefHeight = 44f;
		Button folder = p.add(new Button(() -> host.tr("gentleui.settings.open_folder"), () -> host.run(Actions.FOLDER + ":config")));
		folder.icon(Icon.FOLDER);
		folder.prefHeight = 22f;
		Button images = p.add(new Button(() -> host.tr("gentleui.settings.open_images"), () -> host.run(Actions.FOLDER + ":images")));
		images.icon(Icon.IMAGE);
		images.prefHeight = 22f;
	}

	// ----------------------------------------------------------------------------------------------------------
	// Frame and input
	// ----------------------------------------------------------------------------------------------------------

	@Override
	protected void render(Canvas c, float dt) {
		time += dt;
		if (!builtForTheme.equals(cfg().theme)) {
			// The theme changed from inside this screen: rebuild so theme-specific options follow.
			rebuild();
		}
		Theme theme = Themes.active();
		backdrop.width = width;
		backdrop.height = height;
		backdrop.mouseX = mouseX;
		backdrop.mouseY = mouseY;
		backdrop.dt = dt;
		backdrop.time = time;
		backdrop.inWorld = host.inWorld();
		backdrop.calm = 0.7f;
		backdrop.pointerActive = cfg().pointerEffects;
		theme.background(c, backdrop);
		// The sheet rises and fades in when the screen opens.
		float enter = io.github.alexwhitehk.gentleui.core.math.Easing.EMPHASIZED.apply(Math.min(1f, time / 0.4f));
		c.pushAlpha(enter);
		c.push();
		c.translate(0f, (1f - enter) * 12f);
		theme.skin().panel(c, sheet, PanelStyle.SHEET);
		Icon.SLIDERS.draw(c, sheet.x + 18f, sheet.y + 17f, 12f, theme.palette().accent, 2.2f);
		c.text("GentleUI", sheet.x + 30f, sheet.y + 13f, theme.palette().text, 1f, Align.LEFT, false);
		c.text(host.tr(section.key), sheet.x + 30f + c.textWidth("GentleUI") + 8f, sheet.y + 13f, theme.palette().textMuted, 1f, Align.LEFT, false);
		theme.skin().separator(c, sheet.x + 8f, sheet.y + 30f, sheet.w - 16f);
		contentIn.update(dt);
		// Switching sections cross-fades only the content column.
		float content = MathUtil.clamp01(contentIn.value());
		if (this.content != null) {
			this.content.userOpacity = content;
			this.content.userDy = (1f - content) * 6f;
		}
		ui.render(c, theme, host, mouseX, mouseY, dt, time);
		c.pop();
		c.popAlpha();
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
	public boolean mouseDragged(float x, float y, int button, float dx, float dy) {
		return ui.mouseDragged(x, y, dx, dy);
	}

	@Override
	public boolean mouseScrolled(float x, float y, float dx, float dy) {
		return ui.mouseScrolled(x, y, dy);
	}

	@Override
	public boolean keyDown(int key, int scancode, int mods) {
		if (super.keyDown(key, scancode, mods)) {
			return true;
		}
		return ui.keyDown(key, mods);
	}

	@Override
	public boolean charTyped(int codepoint, int mods) {
		return ui.charTyped(codepoint);
	}

	@Override
	public void onClose() {
		ConfigStore.get().saveIfDirty();
		host.close();
	}

	@Override
	public void removed() {
		ConfigStore.get().saveIfDirty();
	}

	@Override
	public boolean pausesGame() {
		return true;
	}

	// ----------------------------------------------------------------------------------------------------------
	// Custom widgets
	// ----------------------------------------------------------------------------------------------------------

	/** Three live previews; clicking one switches the theme everywhere. */
	private final class ThemePicker extends Widget {
		private final Rect card = new Rect();
		private final Rect art = new Rect();
		private final float[] hover = new float[8];
		private int pressed = -1;

		@Override
		public boolean interactive() {
			return true;
		}

		private int indexAt(float x) {
			List<String> ids = Themes.ids();
			float w = (bounds.w - 8f * (ids.size() - 1)) / ids.size();
			float local = x - screenX();
			int i = (int) (local / (w + 8f));
			return i >= 0 && i < ids.size() && local - i * (w + 8f) <= w ? i : -1;
		}

		@Override
		public boolean mouseDown(UiContext ctx, float x, float y, int button) {
			pressed = indexAt(x);
			return true;
		}

		@Override
		public void mouseUp(UiContext ctx, float x, float y, int button, boolean inside) {
			int i = indexAt(x);
			if (inside && i >= 0 && i == pressed) {
				String id = Themes.ids().get(i);
				if (!id.equals(cfg().theme)) {
					host.playSound(UiSound.TOGGLE);
					edit(c -> c.theme = id);
				}
			}
			pressed = -1;
		}

		@Override
		protected void paint(UiContext ctx) {
			Canvas c = ctx.canvas;
			List<String> ids = Themes.ids();
			float w = (bounds.w - 8f * (ids.size() - 1)) / ids.size();
			int hi = isHovered(ctx) ? indexAt(ctx.mouseX) : -1;
			for (int i = 0; i < ids.size(); i++) {
				Theme t = Themes.get(ids.get(i));
				boolean selected = ids.get(i).equals(cfg().theme);
				hover[i] = MathUtil.approach(hover[i], i == hi ? 1f : 0f, 0.05f, ctx.dt);
				card.set(bounds.x + i * (w + 8f), bounds.y + 2f - hover[i] * 1.5f, w, bounds.h - 16f);
				art.set(card.x + 3f, card.y + 3f, card.w - 6f, card.h - 6f);
				ctx.skin().panel(c, card, PanelStyle.CARD);
				t.preview(c, art, ctx.time);
				if (selected) {
					c.strokeRoundRect(card.x - 2f, card.y - 2f, card.w + 4f, card.h + 4f, 10f, 1.6f, ctx.palette().accent);
				}
				int color = selected ? ctx.palette().accent : (hover[i] > 0.5f ? ctx.palette().text : ctx.palette().textMuted);
				Icon.byId(t.iconId()).draw(c, card.cx() - c.textWidth(host.tr(t.nameKey())) * 0.5f - 6f, bounds.bottom() - 6f, 9f, color, 2.2f);
				c.text(host.tr(t.nameKey()), card.cx() + 5f, bounds.bottom() - 10f, color, 1f, Align.CENTER, false);
			}
		}
	}

	/** Accent seed colors for the Material theme. */
	private final class Swatches extends Widget {
		private final float[] hover = new float[MaterialColors.SEEDS.length];

		@Override
		public boolean interactive() {
			return true;
		}

		private int indexAt(float x) {
			float local = x - screenX() - 8f;
			int i = (int) (local / 22f);
			return i >= 0 && i < MaterialColors.SEEDS.length && local - i * 22f <= 18f ? i : -1;
		}

		@Override
		public void mouseUp(UiContext ctx, float x, float y, int button, boolean inside) {
			int i = indexAt(x);
			if (inside && i >= 0) {
				host.playSound(UiSound.TOGGLE);
				edit(c -> c.material.accent = MaterialColors.SEEDS[i]);
			}
		}

		@Override
		protected void paint(UiContext ctx) {
			Canvas c = ctx.canvas;
			c.text(host.tr("gentleui.settings.accent"), bounds.x + 8f, bounds.y + 2f, ctx.palette().text, 1f, Align.LEFT, false);
			int hi = isHovered(ctx) ? indexAt(ctx.mouseX) : -1;
			for (int i = 0; i < MaterialColors.SEEDS.length; i++) {
				int color = Argb.parse(MaterialColors.SEEDS[i], 0xFF1A73E8);
				boolean selected = MaterialColors.SEEDS[i].equalsIgnoreCase(cfg().material.accent);
				hover[i] = MathUtil.approach(hover[i], i == hi ? 1f : 0f, 0.05f, ctx.dt);
				float cx = bounds.x + 17f + i * 22f;
				float cy = bounds.y + 24f;
				float r = 7f + hover[i] * 1.2f;
				c.fillCircle(cx, cy, r, color);
				if (selected) {
					c.strokeCircle(cx, cy, r + 3f, 1.5f, color);
					Icon.CHECK.draw(c, cx, cy, r * 1.3f, 0xFFFFFFFF, 2.6f);
				}
			}
		}
	}
}
