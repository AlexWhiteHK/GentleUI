package io.github.alexwhitehk.gentleui.core.scene;

import io.github.alexwhitehk.gentleui.core.config.ConfigStore;
import io.github.alexwhitehk.gentleui.core.config.CustomElement;
import io.github.alexwhitehk.gentleui.core.config.GentleConfig;
import io.github.alexwhitehk.gentleui.core.config.LayoutConfig;
import io.github.alexwhitehk.gentleui.core.host.Keys;
import io.github.alexwhitehk.gentleui.core.host.UiSound;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.scene.elements.Anchor;
import io.github.alexwhitehk.gentleui.core.scene.elements.CustomElementView;
import io.github.alexwhitehk.gentleui.core.scene.elements.TitleElement;
import io.github.alexwhitehk.gentleui.core.scene.elements.VersionElement;
import io.github.alexwhitehk.gentleui.core.theme.Backdrop;
import io.github.alexwhitehk.gentleui.core.theme.Theme;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared behaviour of every theme's title screen: background, the user-arrangeable element layer and the hooks
 * the layout editor uses to move things around.
 */
public abstract class TitleScreenBase extends UiScreen {
	protected final Theme theme;
	protected final Backdrop backdrop = new Backdrop();
	private float time;
	/** Built-in elements contributed by the theme (version label etc.). */
	protected final List<TitleElement> builtIns = new ArrayList<>();
	private final List<TitleElement> customs = new ArrayList<>();
	private final float[] tmp = new float[2];
	private TitleElement pressedElement;
	/** While true the screen is being edited: elements do not react, the editor overlay handles input. */
	protected boolean editing;

	protected TitleScreenBase(Theme theme) {
		this.theme = theme;
	}

	public Theme theme() {
		return theme;
	}

	protected float time() {
		return time;
	}

	@Override
	protected void onOpen() {
		builtIns.clear();
		addBuiltIns(builtIns);
		reloadCustomElements();
	}

	/** Themes add their movable built-in elements here. */
	protected void addBuiltIns(List<TitleElement> out) {
		if (ConfigStore.config().showVersion) {
			out.add(new VersionElement(host, Anchor.BOTTOM_LEFT));
		}
	}

	public void reloadCustomElements() {
		layoutStamp = ConfigStore.toJson(layoutConfig()).hashCode();
		customs.clear();
		for (CustomElement e : layoutConfig().custom) {
			customs.add(new CustomElementView(e));
		}
	}

	public LayoutConfig layoutConfig() {
		return ConfigStore.config().layout(theme.id());
	}

	public List<TitleElement> elements() {
		List<TitleElement> all = new ArrayList<>(builtIns);
		all.addAll(customs);
		return all;
	}

	private int layoutStamp = -1;

	/**
	 * True when the layout was changed elsewhere (e.g. in the editor) since this screen built its elements;
	 * the platform then recreates the screen when returning to it.
	 */
	public boolean editedLayoutStale() {
		return layoutStamp != ConfigStore.toJson(layoutConfig()).hashCode();
	}

	public void setEditing(boolean editing) {
		this.editing = editing;
	}

	@Override
	protected final void render(Canvas c, float dt) {
		time += dt;
		renderScene(c, dt);
		renderElements(c, dt);
	}

	protected abstract void renderScene(Canvas c, float dt);

	/** Fills the backdrop for this frame and draws the theme background. */
	protected void drawBackground(Canvas c, float dt, float calm) {
		GentleConfig cfg = ConfigStore.config();
		backdrop.width = width;
		backdrop.height = height;
		backdrop.mouseX = mouseX;
		backdrop.mouseY = mouseY;
		backdrop.time = time;
		backdrop.dt = dt;
		backdrop.inWorld = host.inWorld();
		backdrop.calm = calm;
		backdrop.pointerActive = cfg.pointerEffects && !editing;
		theme.background(c, backdrop);
	}

	/** Applies placement to every element; widget-backed elements must be placed before the widgets render. */
	protected void placeElements(Canvas c) {
		LayoutConfig layout = layoutConfig();
		for (TitleElement e : elements()) {
			if (e instanceof io.github.alexwhitehk.gentleui.core.scene.elements.WidgetElement we) {
				we.editingHint = editing;
				we.place(c, width, height, layout.overrideOrNull(e.id), tmp);
			}
		}
	}

	protected void renderElements(Canvas c, float dt) {
		LayoutConfig layout = layoutConfig();
		for (TitleElement e : elements()) {
			if (e instanceof io.github.alexwhitehk.gentleui.core.scene.elements.WidgetElement) {
				continue;
			}
			LayoutConfig.ElementOverride o = e.custom() ? null : layout.overrideOrNull(e.id);
			float scale = e.place(c, width, height, o, tmp);
			if (scale <= 0f) {
				if (!editing) {
					continue;
				}
				// Hidden elements stay faintly visible while editing so they can be found and restored.
				scale = o != null && o.scale > 0f ? o.scale : 1f;
				c.pushAlpha(0.25f);
				e.draw(c, theme, host, e.bounds.cx(), e.bounds.cy(), scale, time);
				c.popAlpha();
				continue;
			}
			boolean hot = !editing && e.clickable() && e.bounds.contains(mouseX, mouseY);
			e.hover = MathUtil.approach(e.hover, hot ? 1f : 0f, 0.05f, dt);
			float opacity = o == null ? 1f : o.opacity;
			c.pushAlpha(opacity);
			e.draw(c, theme, host, e.bounds.cx(), e.bounds.cy(), scale, time);
			c.popAlpha();
		}
	}

	/** Element under the pointer, topmost first. */
	public TitleElement elementAt(float x, float y) {
		List<TitleElement> all = elements();
		for (int i = all.size() - 1; i >= 0; i--) {
			TitleElement e = all.get(i);
			if (e.bounds.contains(x, y)) {
				return e;
			}
		}
		return null;
	}

	@Override
	public boolean mouseDown(float x, float y, int button, int mods) {
		if (editing) {
			return true;
		}
		TitleElement e = elementAt(x, y);
		if (e != null && e.clickable() && button == Keys.MOUSE_LEFT) {
			pressedElement = e;
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseUp(float x, float y, int button, int mods) {
		if (editing) {
			return true;
		}
		TitleElement e = pressedElement;
		pressedElement = null;
		if (e != null && e.bounds.contains(x, y)) {
			host.playSound(UiSound.CLICK);
			e.click(host);
			return true;
		}
		return false;
	}

	@Override
	public boolean keyDown(int key, int scancode, int mods) {
		return editing;
	}

	@Override
	public boolean closeOnEscape() {
		return false;
	}

	@Override
	public boolean pausesGame() {
		return false;
	}

	@Override
	public String title() {
		return "Minecraft";
	}
}
