package io.github.alexwhitehk.gentleui.core.scene;

import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.scene.elements.TitleElement;
import io.github.alexwhitehk.gentleui.core.scene.elements.WidgetElement;
import java.util.ArrayList;
import java.util.List;
import io.github.alexwhitehk.gentleui.core.theme.Theme;
import io.github.alexwhitehk.gentleui.core.ui.Widget;
import io.github.alexwhitehk.gentleui.core.ui.WidgetRoot;

/** Title screen built from regular widgets (Material, Glass). Subclasses describe their layout in {@link #build}. */
public abstract class WidgetTitleScreen extends TitleScreenBase {
	protected final WidgetRoot ui = new WidgetRoot();
	private boolean built;
	private final List<WidgetElement> widgetElements = new ArrayList<>();

	protected WidgetTitleScreen(Theme theme) {
		super(theme);
	}

	/** Creates the widget tree for the current size. {@code entrance} is true the first time. */
	protected abstract void build(Widget root, boolean entrance);

	@Override
	protected void layout() {
		super.layout();
		ui.root.clear();
		widgetElements.clear();
		build(ui.root, !built);
		built = true;
		ui.layout(width, height);
	}

	/** Registers a top-level widget group as a movable title element. */
	protected <T extends Widget> T element(String id, String nameKey, T widget) {
		widgetElements.add(new WidgetElement(id, nameKey, widget));
		return widget;
	}

	@Override
	public List<TitleElement> elements() {
		List<TitleElement> all = new ArrayList<>(widgetElements);
		all.addAll(super.elements());
		return all;
	}

	/** Background plus anything drawn behind the widgets. */
	protected void drawScenery(Canvas c, float dt) {
		drawBackground(c, dt, 0f);
	}

	@Override
	protected void renderScene(Canvas c, float dt) {
		drawScenery(c, dt);
		placeElements(c);
		if (!editing) {
			ui.render(c, theme, host, mouseX, mouseY, dt, time());
		} else {
			ui.render(c, theme, host, -1000f, -1000f, dt, time());
		}
	}

	@Override
	public boolean mouseDown(float x, float y, int button, int mods) {
		if (super.mouseDown(x, y, button, mods)) {
			return true;
		}
		if (ui.mouseDown(x, y, button)) {
			return true;
		}
		theme.pointerPressed(x, y);
		return true;
	}

	@Override
	public boolean mouseUp(float x, float y, int button, int mods) {
		if (super.mouseUp(x, y, button, mods)) {
			return true;
		}
		return ui.mouseUp(x, y, button);
	}

	@Override
	public boolean mouseDragged(float x, float y, int button, float dx, float dy) {
		return !editing && ui.mouseDragged(x, y, dx, dy);
	}

	@Override
	public boolean mouseScrolled(float x, float y, float dx, float dy) {
		return !editing && ui.mouseScrolled(x, y, dy);
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
		return !editing && ui.charTyped(codepoint);
	}
}
