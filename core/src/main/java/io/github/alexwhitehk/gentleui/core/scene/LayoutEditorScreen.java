package io.github.alexwhitehk.gentleui.core.scene;

import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.config.ConfigStore;
import io.github.alexwhitehk.gentleui.core.config.CustomElement;
import io.github.alexwhitehk.gentleui.core.config.LayoutConfig;
import io.github.alexwhitehk.gentleui.core.host.Actions;
import io.github.alexwhitehk.gentleui.core.host.Keys;
import io.github.alexwhitehk.gentleui.core.host.UiSound;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Align;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.render.Icon;
import io.github.alexwhitehk.gentleui.core.scene.elements.Anchor;
import io.github.alexwhitehk.gentleui.core.scene.elements.CustomElementView;
import io.github.alexwhitehk.gentleui.core.scene.elements.TitleElement;
import io.github.alexwhitehk.gentleui.core.theme.ButtonStyle;
import io.github.alexwhitehk.gentleui.core.theme.Palette;
import io.github.alexwhitehk.gentleui.core.theme.PanelStyle;
import io.github.alexwhitehk.gentleui.core.theme.Theme;
import io.github.alexwhitehk.gentleui.core.theme.Themes;
import io.github.alexwhitehk.gentleui.core.ui.Button;
import io.github.alexwhitehk.gentleui.core.ui.IconButton;
import io.github.alexwhitehk.gentleui.core.ui.Label;
import io.github.alexwhitehk.gentleui.core.ui.ScrollPanel;
import io.github.alexwhitehk.gentleui.core.ui.Slider;
import io.github.alexwhitehk.gentleui.core.ui.TextField;
import io.github.alexwhitehk.gentleui.core.ui.Toggle;
import io.github.alexwhitehk.gentleui.core.ui.Widget;
import io.github.alexwhitehk.gentleui.core.ui.WidgetRoot;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Drag-and-drop editor for the active theme's title screen. The real title screen runs underneath (frozen for
 * input) so what you arrange is exactly what you get.
 */
public final class LayoutEditorScreen extends UiScreen {
	private static final float SNAP = 4f;

	private TitleScreenBase preview;
	private final WidgetRoot ui = new WidgetRoot();
	private final Rect toolbar = new Rect();
	private final Rect panel = new Rect();
	private TitleElement selected;
	private String selectedId;
	private TitleElement hovered;
	private boolean dragging;
	private boolean resizing;
	private float grabDx;
	private float grabDy;
	private float startScale;
	private float startW;
	private float startH;
	private float startDist;
	private Float guideX;
	private Float guideY;
	private float time;
	private final io.github.alexwhitehk.gentleui.core.anim.Spring toolbarY = new io.github.alexwhitehk.gentleui.core.anim.Spring(6f, 160f, 0.9f);
	private boolean toolbarBottom;
	private boolean panelLeft;

	private LayoutConfig layoutConfig() {
		return ConfigStore.config().layout(Themes.active().id());
	}

	private void edit(Consumer<LayoutConfig> change) {
		ConfigStore.get().edit(c -> change.accept(c.layout(c.theme)));
	}

	@Override
	public String title() {
		return host.tr("gentleui.menu.editor");
	}

	@Override
	protected void onOpen() {
		UiScreen ts = Themes.active().createTitleScreen();
		if (ts instanceof TitleScreenBase base) {
			preview = base;
			preview.setEditing(true);
		}
	}

	@Override
	protected void layout() {
		if (preview != null) {
			preview.attach(host, width, height);
		}
		rebuild();
	}

	private void rebuild() {
		Widget root = ui.root;
		root.clear();
		float tw = Math.min(width - 16f, 6f + 6 * 21f + 6f + 62f + 6f);
		float ty = toolbarBottom ? height - 32f : 6f;
		toolbarY.target(ty);
		toolbar.set((width - tw) * 0.5f, ty, tw, 26f);
		String[][] adds = {
			{"text", CustomElement.TEXT},
			{"image", CustomElement.IMAGE},
			{"clock", CustomElement.CLOCK},
			{"forward", CustomElement.BUTTON},
			{"grid", CustomElement.SHAPE},
		};
		float x = toolbar.x + 6f;
		for (String[] a : adds) {
			String type = a[1];
			IconButton b = root.add(new IconButton(Icon.byId(a[0]), host.tr("gentleui.editor.add", host.tr("gentleui.element.type." + type)), () -> addElement(type)));
			b.style(ButtonStyle.GHOST);
			b.bounds(x, toolbar.y + 4f, 18f, 18f);
			x += 21f;
		}
		IconButton reset = root.add(new IconButton(Icon.RESET, host.tr("gentleui.settings.reset_layout"), this::resetAll));
		reset.style(ButtonStyle.GHOST);
		reset.bounds(x + 6f, toolbar.y + 4f, 18f, 18f);
		Button done = root.add(new Button(() -> host.tr("gui.done"), this::onClose));
		done.style(ButtonStyle.PRIMARY);
		done.bounds(toolbar.right() - 62f, toolbar.y + 4f, 56f, 18f);

		if (selected != null) {
			float pw = Math.min(150f, width * 0.36f);
			panelLeft = selected.bounds.cx() > width * 0.6f;
			float py = toolbarBottom ? 8f : toolbar.bottom() + 8f;
			float ph = Math.min(height - 16f - (toolbar.h + 8f), 230f);
			panel.set(panelLeft ? 8f : width - pw - 8f, py, pw, ph);
			ScrollPanel props = root.add(new ScrollPanel());
			props.bounds(panel.x, panel.y + 18f, panel.w, panel.h - 22f);
			fillProperties(props);
			ui.layout(width, height);
			props.stack(3f);
		} else {
			ui.layout(width, height);
		}
	}

	private void fillProperties(ScrollPanel p) {
		TitleElement e = selected;
		if (e instanceof CustomElementView view) {
			CustomElement m = view.model;
			if (!CustomElement.CLOCK.equals(m.type) && !CustomElement.SHAPE.equals(m.type) && !CustomElement.IMAGE.equals(m.type)) {
				field(p, "gentleui.editor.text", m.text, v -> m.text = v);
			}
			if (CustomElement.BUTTON.equals(m.type)) {
				field(p, "gentleui.editor.action", m.action, v -> m.action = v);
				field(p, "gentleui.editor.icon", m.icon, v -> m.icon = v);
			}
			if (CustomElement.IMAGE.equals(m.type)) {
				field(p, "gentleui.editor.image", m.image.startsWith("file:") ? m.image.substring(5) : m.image,
					v -> m.image = v.contains(":") ? v : "file:" + v);
				Button folder = p.add(new Button(() -> host.tr("gentleui.settings.open_images"), () -> host.run(Actions.FOLDER + ":images")));
				folder.icon(Icon.FOLDER);
				folder.prefHeight = 18f;
			}
			if (!CustomElement.IMAGE.equals(m.type)) {
				field(p, "gentleui.editor.color", m.color, v -> m.color = v);
			}
			p.add(new io.github.alexwhitehk.gentleui.core.ui.AnchorPicker(() -> host.tr("gentleui.editor.anchor"),
				() -> Anchor.byId(m.anchor).ordinal(), i -> reanchor(view, Anchor.values()[i])));
			p.add(new Slider(() -> host.tr("gentleui.editor.scale"), 0.25f, 4f, 0.05f, () -> m.scale, v -> touch(() -> m.scale = v), v -> Math.round(v * 100f) + "%"));
			p.add(new Slider(() -> host.tr("gentleui.editor.opacity"), 0.05f, 1f, 0.05f, () -> m.opacity, v -> touch(() -> m.opacity = v), v -> Math.round(v * 100f) + "%"));
			p.add(new Slider(() -> host.tr("gentleui.editor.rotation"), -180f, 180f, 1f, () -> m.rotation, v -> touch(() -> m.rotation = v), v -> Math.round(v) + "°"));
			if (CustomElement.SHAPE.equals(m.type) || CustomElement.IMAGE.equals(m.type) || CustomElement.BUTTON.equals(m.type)) {
				p.add(new Slider(() -> host.tr("gentleui.editor.width"), 8f, 400f, 1f, () -> m.w, v -> touch(() -> m.w = v), v -> Math.round(v) + ""));
				p.add(new Slider(() -> host.tr("gentleui.editor.height"), 8f, 300f, 1f, () -> m.h, v -> touch(() -> m.h = v), v -> Math.round(v) + ""));
			}
			if (CustomElement.SHAPE.equals(m.type)) {
				p.add(new Slider(() -> host.tr("gentleui.editor.radius"), 0f, 100f, 1f, () -> m.radius, v -> touch(() -> m.radius = v), v -> Math.round(v) + ""));
			}
			Button del = p.add(new Button(() -> host.tr("gentleui.editor.delete"), this::deleteSelected));
			del.icon(Icon.TRASH).style(ButtonStyle.DANGER);
			del.prefHeight = 20f;
		} else {
			LayoutConfig.ElementOverride o = layoutConfig().override(e.id);
			p.add(new Slider(() -> host.tr("gentleui.editor.scale"), 0.25f, 4f, 0.05f, () -> o.scale, v -> touch(() -> o.scale = v), v -> Math.round(v * 100f) + "%"));
			p.add(new Slider(() -> host.tr("gentleui.editor.opacity"), 0.05f, 1f, 0.05f, () -> o.opacity, v -> touch(() -> o.opacity = v), v -> Math.round(v * 100f) + "%"));
			p.add(new Toggle(() -> host.tr("gentleui.editor.hidden"), () -> o.hidden, v -> touch(() -> o.hidden = v)));
			Button reset = p.add(new Button(() -> host.tr("gentleui.editor.reset_element"), () -> {
				touch(() -> layoutConfig().overrides.remove(e.id));
				rebuild();
			}));
			reset.icon(Icon.RESET);
			reset.prefHeight = 20f;
		}
	}

	private void field(ScrollPanel p, String key, String value, Consumer<String> set) {
		Label l = p.add(new Label(host.tr(key)).tone(Label.Tone.MUTED));
		l.prefHeight = 11f;
		TextField f = p.add(new TextField(value == null ? "" : value));
		f.prefHeight = 18f;
		f.onChange = v -> touch(() -> set.accept(v));
	}

	/** Marks the configuration dirty after an in-place model change. */
	private void touch(Runnable change) {
		ConfigStore.get().edit(c -> change.run());
	}

	// ----------------------------------------------------------------------------------------------------------
	// Element operations
	// ----------------------------------------------------------------------------------------------------------

	private void addElement(String type) {
		CustomElement e = new CustomElement();
		e.id = type + "_" + Long.toString(System.currentTimeMillis() % 100000L, 36);
		e.type = type;
		e.anchor = Anchor.CENTER.id;
		e.x = 0f;
		e.y = 0f;
		switch (type) {
			case CustomElement.TEXT -> e.text = host.tr("gentleui.editor.sample_text");
			case CustomElement.BUTTON -> {
				e.text = host.tr("menu.multiplayer");
				e.action = Actions.MULTIPLAYER;
				e.icon = "people";
				e.w = 110f;
				e.h = 22f;
			}
			case CustomElement.IMAGE -> {
				e.w = 64f;
				e.h = 64f;
			}
			case CustomElement.SHAPE -> {
				e.w = 60f;
				e.h = 60f;
				e.radius = 30f;
				e.opacity = 0.6f;
			}
			default -> {
			}
		}
		edit(l -> l.custom.add(e));
		preview.reloadCustomElements();
		selectById(e.id);
		host.playSound(UiSound.OPEN);
	}

	private void deleteSelected() {
		if (selected instanceof CustomElementView view) {
			edit(l -> l.custom.removeIf(c -> c.id.equals(view.model.id)));
			preview.reloadCustomElements();
			selected = null;
			selectedId = null;
			host.playSound(UiSound.CLOSE);
			rebuild();
		} else if (selected != null) {
			LayoutConfig.ElementOverride o = layoutConfig().override(selected.id);
			touch(() -> o.hidden = !o.hidden);
			rebuild();
		}
	}

	private void resetAll() {
		ConfigStore.get().edit(c -> c.layouts.remove(c.theme));
		preview.reloadCustomElements();
		selected = null;
		selectedId = null;
		rebuild();
	}

	private void selectById(String id) {
		selectedId = id;
		selected = null;
		for (TitleElement e : preview.elements()) {
			if (e.id.equals(id)) {
				selected = e;
			}
		}
		rebuild();
	}

	private void reanchor(CustomElementView view, Anchor a) {
		CustomElement m = view.model;
		float cx = view.bounds.cx();
		float cy = view.bounds.cy();
		touch(() -> {
			m.anchor = a.id;
			m.x = cx - width * a.fx;
			m.y = cy - height * a.fy;
		});
	}

	// ----------------------------------------------------------------------------------------------------------
	// Frame
	// ----------------------------------------------------------------------------------------------------------

	@Override
	protected void render(Canvas c, float dt) {
		time += dt;
		Theme theme = Themes.active();
		Palette p = theme.palette();
		if (preview == null) {
			return;
		}
		preview.frame(c, mouseX, mouseY);
		if (selectedId != null && (selected == null || !preview.elements().contains(selected))) {
			selected = null;
			for (TitleElement e : preview.elements()) {
				if (e.id.equals(selectedId)) {
					selected = e;
				}
			}
		}
		// Edit-mode frame around the screen.
		int accent = p.accent;
		c.strokeRoundRect(2f, 2f, width - 4f, height - 4f, 10f, 1.5f, Argb.withAlpha(accent, 0.5f + 0.15f * MathUtil.sin(time * 2f)));
		hovered = dragging || resizing ? hovered : preview.elementAt(mouseX, mouseY);
		if (ui.ctx.hovered != null || selected != null && panel.contains(mouseX, mouseY)) {
			hovered = dragging || resizing ? hovered : null;
		}
		for (TitleElement e : preview.elements()) {
			Rect b = e.bounds;
			boolean hidden = !e.custom() && layoutConfig().overrideOrNull(e.id) != null && layoutConfig().overrideOrNull(e.id).hidden;
			if (e == selected) {
				c.strokeRoundRect(b.x - 3f, b.y - 3f, b.w + 6f, b.h + 6f, 4f, 1.4f, accent);
				float hs = 5f;
				c.fillRoundRect(b.right() + 3f - hs * 0.5f, b.bottom() + 3f - hs * 0.5f, hs, hs, 1.5f, accent);
				String name = e.displayName(host) + (hidden ? "  (" + host.tr("gentleui.editor.hidden") + ")" : "");
				float tw = c.textWidth(name) + 8f;
				float ty = b.y - 17f < toolbar.bottom() + 2f ? b.bottom() + 6f : b.y - 17f;
				Rect tag = new Rect(b.x - 3f, ty, tw, 12f);
				c.fillRoundRect(tag.x, tag.y, tag.w, tag.h, 3f, accent);
				c.text(name, tag.x + 4f, tag.y + 2f, p.onAccent, 1f, Align.LEFT, false);
			} else if (e == hovered) {
				c.strokeRoundRect(b.x - 3f, b.y - 3f, b.w + 6f, b.h + 6f, 4f, 1f, Argb.withAlpha(accent, 0.6f));
			} else {
				c.strokeRoundRect(b.x - 2f, b.y - 2f, b.w + 4f, b.h + 4f, 3f, c.pixel(), Argb.withAlpha(p.textMuted, hidden ? 0.15f : 0.35f));
			}
		}
		if (guideX != null) {
			c.line(guideX, 0f, guideX, height, c.pixel() * 1.2f, Argb.withAlpha(accent, 0.8f), Canvas.Cap.BUTT);
		}
		if (guideY != null) {
			c.line(0f, guideY, width, guideY, c.pixel() * 1.2f, Argb.withAlpha(accent, 0.8f), Canvas.Cap.BUTT);
		}
		boolean wantBottom = selected != null && selected.bounds.y < 40f;
		if (wantBottom != toolbarBottom && !dragging && !resizing) {
			toolbarBottom = wantBottom;
			rebuild();
		}
		toolbarY.update(dt);
		theme.skin().panel(c, toolbar, PanelStyle.POPOVER);
		if (selected != null) {
			theme.skin().panel(c, panel, PanelStyle.CARD);
			c.text(c.ellipsize(selected.displayName(host), panel.w - 12f, 1f), panel.x + 8f, panel.y + 6f, p.text, 1f, Align.LEFT, false);
		} else {
			String hint = host.tr("gentleui.editor.hint");
			c.text(hint, width * 0.5f, toolbarBottom ? 40f : height - 14f, Argb.withAlpha(p.textMuted, 0.9f), 1f, Align.CENTER, false);
		}
		ui.render(c, theme, host, mouseX, mouseY, dt, time);
	}

	// ----------------------------------------------------------------------------------------------------------
	// Input
	// ----------------------------------------------------------------------------------------------------------

	private boolean overHandle(float x, float y) {
		if (selected == null) {
			return false;
		}
		Rect b = selected.bounds;
		return MathUtil.dist(x, y, b.right() + 3f, b.bottom() + 3f) < 6f;
	}

	@Override
	public boolean mouseDown(float x, float y, int button, int mods) {
		if (ui.mouseDown(x, y, button)) {
			return true;
		}
		if (selected != null && panel.contains(x, y)) {
			return true;
		}
		if (overHandle(x, y)) {
			resizing = true;
			Rect b = selected.bounds;
			startDist = Math.max(4f, MathUtil.dist(b.cx(), b.cy(), x, y));
			if (selected instanceof CustomElementView v) {
				startScale = v.model.scale;
				startW = v.model.w;
				startH = v.model.h;
			} else {
				startScale = layoutConfig().override(selected.id).scale;
			}
			return true;
		}
		TitleElement e = preview.elementAt(x, y);
		if (e != selected) {
			selected = e;
			selectedId = e == null ? null : e.id;
			rebuild();
		}
		if (e != null) {
			dragging = true;
			grabDx = x - e.bounds.cx();
			grabDy = y - e.bounds.cy();
		}
		return true;
	}

	@Override
	public boolean mouseDragged(float x, float y, int button, float dx, float dy) {
		if (ui.mouseDragged(x, y, dx, dy)) {
			return true;
		}
		if (selected == null) {
			return false;
		}
		if (resizing) {
			Rect b = selected.bounds;
			float k = MathUtil.dist(b.cx(), b.cy(), x, y) / startDist;
			if (selected instanceof CustomElementView v) {
				CustomElement m = v.model;
				boolean sized = CustomElement.SHAPE.equals(m.type) || CustomElement.IMAGE.equals(m.type) || CustomElement.BUTTON.equals(m.type);
				touch(() -> {
					if (sized) {
						m.w = MathUtil.clamp(startW * k, 8f, 600f);
						m.h = MathUtil.clamp(startH * k, 8f, 400f);
					} else {
						m.scale = MathUtil.clamp(startScale * k, 0.25f, 6f);
					}
				});
			} else {
				LayoutConfig.ElementOverride o = layoutConfig().override(selected.id);
				touch(() -> o.scale = MathUtil.clamp(startScale * k, 0.25f, 6f));
			}
			return true;
		}
		if (dragging) {
			moveSelectedTo(x - grabDx, y - grabDy);
			return true;
		}
		return false;
	}

	/** Moves the selection so its center lands at (cx, cy), snapping to the screen center and other elements. */
	private void moveSelectedTo(float cx, float cy) {
		guideX = null;
		guideY = null;
		float[] xs = {width * 0.5f};
		float[] ys = {height * 0.5f};
		List<Float> cxs = new ArrayList<>();
		List<Float> cys = new ArrayList<>();
		cxs.add(width * 0.5f);
		cys.add(height * 0.5f);
		for (TitleElement o : preview.elements()) {
			if (o != selected) {
				cxs.add(o.bounds.cx());
				cys.add(o.bounds.cy());
			}
		}
		for (float gx : cxs) {
			if (Math.abs(cx - gx) < SNAP) {
				cx = gx;
				guideX = gx;
			}
		}
		for (float gy : cys) {
			if (Math.abs(cy - gy) < SNAP) {
				cy = gy;
				guideY = gy;
			}
		}
		float ddx = cx - selected.bounds.cx();
		float ddy = cy - selected.bounds.cy();
		translateSelected(ddx, ddy);
	}

	private void translateSelected(float ddx, float ddy) {
		if (selected instanceof CustomElementView v) {
			CustomElement m = v.model;
			touch(() -> {
				m.x += ddx;
				m.y += ddy;
			});
		} else if (selected != null) {
			LayoutConfig.ElementOverride o = layoutConfig().override(selected.id);
			touch(() -> {
				o.dx += ddx;
				o.dy += ddy;
			});
		}
		selected.bounds.x += ddx;
		selected.bounds.y += ddy;
	}

	@Override
	public boolean mouseUp(float x, float y, int button, int mods) {
		ui.mouseUp(x, y, button);
		if (dragging && selected instanceof CustomElementView v) {
			// Re-anchor to the nearest reference point so the element stays put on other window sizes.
			reanchor(v, Anchor.nearest(v.bounds.cx(), v.bounds.cy(), width, height));
		}
		dragging = false;
		resizing = false;
		guideX = null;
		guideY = null;
		return true;
	}

	@Override
	public boolean mouseScrolled(float x, float y, float dx, float dy) {
		if (ui.mouseScrolled(x, y, dy)) {
			return true;
		}
		if (selected != null && selected.bounds.contains(x, y)) {
			float k = dy > 0f ? 1.05f : 1f / 1.05f;
			if (selected instanceof CustomElementView v) {
				touch(() -> v.model.scale = MathUtil.clamp(v.model.scale * k, 0.25f, 6f));
			} else {
				LayoutConfig.ElementOverride o = layoutConfig().override(selected.id);
				touch(() -> o.scale = MathUtil.clamp(o.scale * k, 0.25f, 6f));
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean keyDown(int key, int scancode, int mods) {
		if (ui.ctx.focused instanceof TextField) {
			if (key == Keys.ESCAPE) {
				ui.setFocus(null);
				return true;
			}
			return ui.keyDown(key, mods);
		}
		if (key == Keys.ESCAPE) {
			if (selected != null) {
				selected = null;
				selectedId = null;
				rebuild();
			} else {
				onClose();
			}
			return true;
		}
		if (selected != null) {
			float step = Keys.shift(mods) ? 8f : 1f;
			switch (key) {
				case Keys.LEFT -> translateSelected(-step, 0f);
				case Keys.RIGHT -> translateSelected(step, 0f);
				case Keys.UP -> translateSelected(0f, -step);
				case Keys.DOWN -> translateSelected(0f, step);
				case Keys.DELETE, Keys.BACKSPACE -> deleteSelected();
				default -> {
					return ui.keyDown(key, mods);
				}
			}
			return true;
		}
		return ui.keyDown(key, mods);
	}

	@Override
	public boolean charTyped(int codepoint, int mods) {
		return ui.charTyped(codepoint);
	}

	@Override
	public boolean closeOnEscape() {
		return false;
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
		return false;
	}
}
